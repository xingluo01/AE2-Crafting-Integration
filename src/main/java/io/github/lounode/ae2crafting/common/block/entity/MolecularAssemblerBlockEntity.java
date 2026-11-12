package io.github.lounode.ae2crafting.common.block.entity;

import java.util.List;


import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkedInvBlockEntity;
import appeng.client.render.crafting.AssemblerAnimationStatus;
import appeng.core.definitions.AEItems;
import appeng.util.inv.CombinedInternalInventory;

import io.github.lounode.ae2crafting.common.logic.AugmentedRecipeMatcher;
import io.github.lounode.ae2crafting.common.menu.AssemblerSlots;
import io.github.lounode.ae2crafting.common.pattern.AugmentedPattern;
import io.github.lounode.ae2crafting.integration.polymorph.PolymorphRecipeSelection;
import io.github.lounode.ae2crafting.registry.ModBlockEntities;
import io.github.lounode.ae2crafting.registry.ModBlocks;

/**
 * 通用分子装配室方块实体：91 格物品、4 格升级卡、样板执行与 ME 网络节点。
 *
 * <p>继承 {@link AENetworkedInvBlockEntity} 而不是裸 {@code BlockEntity}，一次拿到 AE2 方块实体的四条基线：
 * 网格节点生命周期（{@code onReady} 建节点、{@code setRemoved} 销毁）、
 * {@code getInternalInventory()} 的存读与掉落、{@code saveChanges()} 的合并标脏、
 * 以及 {@code IInWorldGridNodeHost} 的实现。</p>
 *
 * <p><b>执行模型与 AE2 分子装配室一致</b>：样板供应器每次「发配」一份样板过来，本机把原料按
 * 9×9 的格子铺好、推进度、完成一次合成、把产物推回供应器那一侧，然后清空等待下一份。
 * 三件事都接在 AE2 自己的接口上，而不是自造一套：</p>
 * <ul>
 *   <li>{@link ICraftingMachine} —— 供应器用 {@code AECapabilities.CRAFTING_MACHINE} 找到本机；</li>
 *   <li>{@link IGridTickable} —— 进度由网格的 tick 管理器推进，空闲时节点休眠；</li>
 *   <li>{@link IPowerChannelState} —— 界面与渲染读它决定「有没有电」。</li>
 * </ul>
 *
 * <p><b>标志物栏是准入门槛</b>：编码终端把玩家放的标志工作方块写进了样板
 * （{@link io.github.lounode.ae2crafting.common.pattern.AugmentedPatternData#workstation()}）。
 * 本机的标志物栏（网格下方那一排）里没有对应工作台时，{@link #pushPattern} 直接拒绝——
 * 样板连推都推不进来，更不会开动。这条就是「只在标志物槽具有样板可用的标志物时才自动化」。</p>
 *
 * <p><b>为什么产物推回供应器而不是塞进产物槽</b>：样板供应器把回收库存以
 * {@code ItemHandler.BLOCK} 暴露出来（AE2 的 {@code GENERIC_INTERNAL_INV} 适配器），
 * 用 {@link InternalInventory#wrapExternal} 往那个方向塞就是「还回 ME 网络」。产物槽只是推不出去时的
 * 缓冲，下一 tick 继续尝试，玩家也能手动取走。</p>
 *
 * <p><b>存读入口是 {@code loadTag} 不是 {@code loadAdditional}</b>：AE2 把 {@code loadAdditional}
 * 定为 final，负责先解客户端视觉数据再转调 {@code loadTag}，子类只能覆盖后者。</p>
 */
public class MolecularAssemblerBlockEntity extends AENetworkedInvBlockEntity
        implements IUpgradeableObject, IGridTickable, ICraftingMachine, IPowerChannelState {

    /**
     * 合成时间戳距今多少 tick 之内还算「刚做完」，值得起一次浮空动画。
     *
     * <p>更新包可能因为别的原因（供电翻转、进度刷新）后到，那时候动画已经过期，
     * 重放一次会显得机器在凭空抽搐。</p>
     */
    private static final long ANIMATION_WINDOW = 20;

    private final AssemblerInventory inventory;
    /**
     * 对外自动化的视图：标志物/催化剂栏 + 产物槽。
     *
     * <p>9x9 网格是装配输入，不接受管道/漏斗/输出总线往里面塞东西，因此整段排除。
     * 产物槽在 {@link AssemblerInventory#isItemValid} 里已禁止放入，抽取不受影响。</p>
     */
    private final InternalInventory exposedInventory;
    /** 升级卡库存不参与 {@code getInternalInventory()}，需要单独存读。 */
    private final IUpgradeInventory upgrades;

    /** 服务端解析出来的样板；客户端不解析，恒为 {@code null}。 */
    @Nullable
    private AugmentedPattern plan;
    /** 样板的物品形态：落盘与推回产物时要用，重进世界后靠它重新解码 {@link #plan}。 */
    private ItemStack planItem = ItemStack.EMPTY;
    /** 供应器是从哪一面把样板推进来的：产物也往那一面推回去。 */
    @Nullable
    private Direction pushDirection;

    private int progress;
    private int maxProgress = AssemblerAcceleration.FULL_PROGRESS;

    /** 配方规则与铺料：标志物、机器类别、通量耗能、9×9 铺料都在这。 */
    private final AssemblerCraftOps craftOps = new AssemblerCraftOps(this);

    private boolean powered;
    /** 网格 tick 的休眠标志，只在状态翻转时通知 tick 管理器，避免每 tick 都发命令。 */
    private boolean awake;

    /** 最近一次合成的输出与耗时：随方块实体更新包同步给客户端做浮动产物渲染。 */
    @Nullable
    private AEKey animationOutput;
    private byte animationSpeed;
    private long animationTick;

    /** 客户端侧的动画骨架；服务端没有这个字段（{@code @OnlyIn} 会被剥离）。 */
    @OnlyIn(Dist.CLIENT)
    @Nullable
    private AssemblerAnimationStatus animationStatus;
    /**
     * 上一次起过动画的合成时间戳，0 表示「还没起过」。
     *
     * <p><b>绝不能写成 {@code = Long.MIN_VALUE}</b>：带初始化器的字段，赋值语句会被编译进
     * <b>构造函数</b>，而构造函数不会被 {@code @OnlyIn} 剥离。于是专用服务器上 {@code new} 这个
     * 方块实体时，构造函数会去 {@code putfield} 一个已经被剥掉的字段，直接
     * {@code NoSuchFieldError: ... does not have member field 'long lastAnimationTick'}——
     * 症状是整个方块实体在服务端加载不出来。这也是 AE2 自己的 {@code animationStatus}
     * 不带初始化器的原因。</p>
     */
    @OnlyIn(Dist.CLIENT)
    private long lastAnimationTick;

    public MolecularAssemblerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MOLECULAR_ASSEMBLER.get(), pos, state);
    }

    public MolecularAssemblerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);

        this.inventory = new AssemblerInventory(this);
        this.exposedInventory = new CombinedInternalInventory(
                inventory.getSubInventory(AssemblerSlots.catalyst(0), AssemblerSlots.output()),
                inventory.getSlotInv(AssemblerSlots.output()));
        this.upgrades = UpgradeInventories.forMachine(
                ModBlocks.MOLECULAR_ASSEMBLER.get(),
                AssemblerAcceleration.MAX_CARDS,
                this::saveChanges);

        // 装配室自身不耗待机功率：接上网络后靠样板供应器推进，频道与能耗由供应器那侧承担。
        // 注意必须在 onReady() 建节点之前设置，节点建立后再改能耗不会追溯生效。
        // 同时挂 IGridTickable：进度由网格 tick 管理器按需唤醒，机器本体不需要方块 ticker。
        getMainNode().setIdlePowerUsage(0.0).addService(IGridTickable.class, this);
    }

    public AssemblerInventory getInventory() {
        return inventory;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return inventory;
    }

    /** 供能力登记使用；不要拿它代替 {@link #getInternalInventory()}，后者才是完整 91 格。 */
    public InternalInventory getExposedInventory() {
        return exposedInventory;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    /** 已插入的加速卡数量，0..{@link AssemblerAcceleration#MAX_CARDS}。 */
    public int getSpeedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    /** 当前每 tick 推进的进度点数，插满 4 张时为 250。 */
    public double getProgressPerTick() {
        return AssemblerAcceleration.progressPerTick(getSpeedCardCount());
    }

    /** 当前完成一次合成需要的 tick 数，插满 4 张时为 1。 */
    public int getTicksPerCraft() {
        return AssemblerAcceleration.ticksPerCraft(getSpeedCardCount());
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    /** 正在执行的样板；没有任务时是 {@code null}。界面与测试用它判断槽位该不该亮。 */
    @Nullable
    public AugmentedPattern getCurrentPattern() {
        ensurePlan();
        return plan;
    }

    // ------------------------------------------------------------------
    // ICraftingMachine：样板供应器把样板推进来
    // ------------------------------------------------------------------

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        Component name = hasCustomName() ? getCustomName() : ModBlocks.MOLECULAR_ASSEMBLER.get().getName();
        var speedCards = getSpeedCardCount();
        List<Component> tooltip = speedCards == 0
                ? List.of()
                : List.of(Component.translatable("gui.ae2_crafting_integration.assembler.speed_cards", speedCards));
        return new PatternContainerGroup(AEItemKey.of(ModBlocks.MOLECULAR_ASSEMBLER.get().asItem()), name, tooltip);
    }

    /**
     * 供应器发配一份样板。
     *
     * <p>四个前置条件缺一不可：机器空闲、网格是空的、样板是本模组的增广样板、
     * <b>标志物栏里有这张样板要的工作台</b>。任一不满足就返回 {@code false}，
     * 让供应器去试下一台机器——而不是把半份原料铺进网格。</p>
     */
    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] table, Direction where) {
        if (level == null || level.isClientSide()) {
            return false;
        }
        if (!(patternDetails instanceof AugmentedPattern pattern)) {
            return false;
        }
        ensurePlan();
        if (plan != null || !craftOps.isGridEmpty()) {
            return false;
        }
        if (!craftOps.hasRequiredWorkstation(pattern)) {
            return false;
        }
        // 组合合成器：基础 1 个输入槽，标志物栏里每个基座再解锁 1 个（最多 80 个）。
        // 槽位不够就当场拒收，别把原料铺进网格再让机器卡住。
        craftOps.ensureRules(pattern);
        if (!craftOps.canAcceptCombination()) {
            return false;
        }
        if (!craftOps.fillGrid(pattern, table)) {
            return false;
        }
        // 通量合成器：每个生效的通量发电机额外扣 8000 AE。电量不够就整份拒绝，
        // 原料还在供应器那边没被扣掉，换一台机器或等电回来再发。
        if (!craftOps.payFluxPower()) {
            craftOps.clearGrid();
            return false;
        }

        this.plan = pattern;
        this.planItem = pattern.getDefinition().toStack();
        this.pushDirection = where;
        this.progress = 0;
        updateSleepiness();
        saveChanges();
        return true;
    }

    /**
     * 还接不接新样板。
     *
     * <p>与 AE2 的分子装配室同口径：有活就说不接，让供应器直接找下一台，
     * 而不是先答应再在 {@link #pushPattern} 里拒绝。</p>
     */
    @Override
    public boolean acceptsPlans() {
        ensurePlan();
        return plan == null;
    }

    // ------------------------------------------------------------------
    // IGridTickable：推进度、完成合成、把产物推回去
    // ------------------------------------------------------------------

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        ensurePlan();
        return new TickingRequest(1, 1, !isAwake());
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        return tick(ticksSinceLastCall);
    }

    /**
     * 一次网格 tick。抽成公开方法是为了让 GameTest 能直接驱动它——
     * 构造一个真实的 {@link IGridNode} 需要整套网格服务，测试里不值得。
     */
    public TickRateModulation tick(int ticksSinceLastCall) {
        if (level == null || level.isClientSide()) {
            return TickRateModulation.SLEEP;
        }
        ensurePlan();

        // 上一轮的产物还没推出去：先推它，不接新活，免得把两批产物混在一起。
        if (!inventory.getStackInSlot(AssemblerSlots.output()).isEmpty()) {
            pushOut();
            updateSleepiness();
            return isAwake() ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
        }

        if (plan == null) {
            updateSleepiness();
            return TickRateModulation.SLEEP;
        }

        // 标志物被抽走：暂停而不是作废，网格里的原料一格都不会少。
        if (!craftOps.hasRequiredWorkstation(plan)) {
            return TickRateModulation.SLEEP;
        }

        // 末影 / 通量合成器：没有对应发电机时慢 50 倍，每放一个减 1，最多按 49 个计。
        craftOps.ensureRules(plan);
        double multiplier = craftOps.durationMultiplier();
        int delta = (int) Math.round(
                getProgressPerTick() / multiplier * Math.max(1, ticksSinceLastCall));
        progress += Math.max(1, delta);
        if (progress < maxProgress) {
            return TickRateModulation.FASTER;
        }

        finishCraft();
        return isAwake() ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
    }

    /**
     * 完成一次合成。
     *
     * <p>余料不自己算：重跑一遍判定器链拿 {@code Match.remaining()}——空桶、无尽的不消耗催化剂
     * 都在那里。重跑同时是一道保险：配方被数据包删掉时不吞原料，改为把网格里的东西退回去。</p>
     */
    private void finishCraft() {
        var pattern = this.plan;
        this.progress = 0;
        if (pattern == null) {
            return;
        }

        var match = AugmentedRecipeMatcher.matchPreferred(level, craftOps.snapshotGridItems(),
                AssemblerSlots.GRID_SIZE, AssemblerSlots.GRID_SIZE, craftOps.stationFor(pattern),
                pattern.getRecipeId(), PolymorphRecipeSelection.forBlockEntity(this));
        if (match == null || match.remaining().isEmpty()) {
            craftOps.clearGrid();
        } else {
            for (int i = 0; i < AssemblerSlots.GRID_SLOTS; i++) {
                var remaining = i < match.remaining().size() ? match.remaining().get(i) : ItemStack.EMPTY;
                inventory.setItemDirect(i, remaining);
            }
        }

        var outputs = pattern.getOutputs();
        for (var output : outputs) {
            if (output.what() instanceof AEItemKey key) {
                pushOutStack(key.toStack((int) Math.min(Integer.MAX_VALUE, output.amount())));
            }
        }
        startAnimation(outputs.getFirst());

        this.plan = null;
        this.planItem = ItemStack.EMPTY;
        this.pushDirection = null;
        saveChanges();
        updateSleepiness();
    }

    /** 推不出去时的最后手段：塞进产物槽；产物槽也被占就掉在机器旁边，绝不静默吞掉。 */
    private void pushOutStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        var leftover = pushTo(stack, pushDirection);
        if (leftover.isEmpty()) {
            return;
        }

        var buffered = inventory.getStackInSlot(AssemblerSlots.output());
        if (buffered.isEmpty()) {
            inventory.setItemDirect(AssemblerSlots.output(), leftover);
        } else if (ItemStack.isSameItemSameComponents(buffered, leftover)
                && buffered.getCount() + leftover.getCount() <= buffered.getMaxStackSize()) {
                    var merged = buffered.copy();
                    merged.grow(leftover.getCount());
                    inventory.setItemDirect(AssemblerSlots.output(), merged);
                } else if (level != null) {
                    net.minecraft.world.Containers.dropItemStack(level,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            leftover);
                }
    }

    /** 每个 tick 把产物槽里的存货往供应器那侧推，直到推空。 */
    private void pushOut() {
        var stack = inventory.getStackInSlot(AssemblerSlots.output());
        if (stack.isEmpty()) {
            return;
        }
        inventory.setItemDirect(AssemblerSlots.output(), pushTo(stack, pushDirection));
    }

    private ItemStack pushTo(ItemStack stack, @Nullable Direction direction) {
        if (level == null || stack.isEmpty()) {
            return stack;
        }
        if (direction != null) {
            return pushToSide(stack, direction);
        }
        for (var side : Direction.values()) {
            stack = pushToSide(stack, side);
            if (stack.isEmpty()) {
                break;
            }
        }
        return stack;
    }

    private ItemStack pushToSide(ItemStack stack, Direction side) {
        var target = InternalInventory.wrapExternal(level, worldPosition.relative(side), side.getOpposite());
        if (target == null) {
            return stack;
        }
        var leftover = target.addItems(stack);
        if (leftover.getCount() != stack.getCount()) {
            saveChanges();
        }
        return leftover;
    }

    private boolean isAwake() {
        return plan != null || !inventory.getStackInSlot(AssemblerSlots.output()).isEmpty();
    }

    /** 只在「有活/没活」翻转时通知 tick 管理器，与 AE2 分子装配室同口径。 */
    private void updateSleepiness() {
        boolean shouldWake = isAwake();
        if (shouldWake == awake) {
            return;
        }
        awake = shouldWake;
        getMainNode().ifPresent((grid, node) -> {
            if (shouldWake) {
                grid.getTickManager().wakeDevice(node);
            } else {
                grid.getTickManager().sleepDevice(node);
            }
        });
    }

    /** 从 {@link #planItem} 解出样板。落盘时世界还没赋给方块实体，所以推迟到这里。 */
    private void ensurePlan() {
        if (plan != null || planItem.isEmpty() || level == null) {
            return;
        }
        if (PatternDetailsHelper.decodePattern(planItem, level) instanceof AugmentedPattern decoded) {
            plan = decoded;
        } else {
            // 样板内容坏了（改了注册名/删了模组）：丢掉，别让机器永远卡在「有活」状态。
            planItem = ItemStack.EMPTY;
            plan = null;
        }
    }

    // ------------------------------------------------------------------
    // 呼吸灯与产物渲染的状态同步
    // ------------------------------------------------------------------

    private void startAnimation(GenericStack primaryOutput) {
        this.animationOutput = primaryOutput.what();
        this.animationSpeed = (byte) Math.min(Byte.MAX_VALUE, Math.max(1, (int) getProgressPerTick()));
        this.animationTick = level != null ? level.getGameTime() : 0L;
        markForUpdate();
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(powered);
        data.writeLong(animationTick);
        data.writeByte(animationSpeed);
        data.writeBoolean(animationOutput != null);
        if (animationOutput != null) {
            AEKey.writeKey(data, animationOutput);
        }
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        boolean wasPowered = powered;
        powered = data.readBoolean();
        // 只收进普通字段：这里**不能**碰任何 @OnlyIn(CLIENT) 的成员。专用服务器加载本类时会把它们剥掉，
        // 而 readFromStream 是会走服务端的（AEBaseBlockEntity#loadAdditional 解 #upd 时）。
        animationTick = data.readLong();
        animationSpeed = data.readByte();
        animationOutput = data.readBoolean() ? AEKey.readKey(data) : null;
        return powered != wasPowered || changed;
    }

    /**
     * 客户端每帧调一次：更新包里出现了新的合成时间戳就起一次浮空动画。
     *
     * <p>判定放在这里而不是 {@link #readFromStream}，是因为它要碰 {@code animationStatus} /
     * {@code lastAnimationTick} 与 {@link AssemblerAnimationStatus}——这三样都标了
     * {@code @OnlyIn(CLIENT)}，专用服务器上会被剥离。调用方只有客户端渲染器那一处，
     * 整个方法的字节码连同字段引用一起被剥掉，服务端不会留下悬空引用。</p>
     */
    @OnlyIn(Dist.CLIENT)
    public void updateAnimationStatus() {
        if (level == null || animationOutput == null || animationTick == lastAnimationTick) {
            return;
        }
        boolean fresh = animationTick > 0 && level.getGameTime() - animationTick <= ANIMATION_WINDOW;
        lastAnimationTick = animationTick;
        if (fresh) {
            setAnimationStatus(new AssemblerAnimationStatus(animationSpeed,
                    animationOutput.wrapForDisplayOrFilter()));
        }
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (reason == IGridNodeListener.State.GRID_BOOT) {
            return;
        }
        boolean poweredNow = false;
        var grid = getMainNode().getGrid();
        if (grid != null) {
            poweredNow = getMainNode().isPowered()
                    && grid.getEnergyService().extractAEPower(1.0, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 1.0E-4;
        }
        if (poweredNow != powered) {
            powered = poweredNow;
            markForUpdate();
        }
    }

    @Override
    public boolean isPowered() {
        return powered;
    }

    @Override
    public boolean isActive() {
        return powered;
    }

    @OnlyIn(Dist.CLIENT)
    public void setAnimationStatus(@Nullable AssemblerAnimationStatus status) {
        this.animationStatus = status;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public AssemblerAnimationStatus getAnimationStatus() {
        return animationStatus;
    }

    // ------------------------------------------------------------------
    // 存档
    // ------------------------------------------------------------------

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        upgrades.writeToNBT(data, "upgrades", registries);
        data.putInt("progress", progress);
        data.putInt("maxProgress", maxProgress);
        if (!planItem.isEmpty()) {
            // 只存物品形态：样板本身属于供应器，这里是一份引用，不参与掉落。
            data.put("plan", planItem.save(registries));
        }
        if (pushDirection != null) {
            data.putInt("pushDirection", pushDirection.ordinal());
        }
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        upgrades.readFromNBT(data, "upgrades", registries);
        progress = data.getInt("progress");
        maxProgress = Math.max(1, data.getInt("maxProgress"));
        plan = null;
        planItem = data.contains("plan") ? ItemStack.parseOptional(registries, data.getCompound("plan")) : ItemStack.EMPTY;
        pushDirection = data.contains("pushDirection") ? Direction.values()[data.getInt("pushDirection")] : null;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        upgrades.clear();
        plan = null;
        planItem = ItemStack.EMPTY;
        pushDirection = null;
        progress = 0;
    }
}
