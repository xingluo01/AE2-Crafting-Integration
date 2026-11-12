package io.github.lounode.ae2crafting.common.menu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.inventories.InternalInventory;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.menu.slot.RestrictedInputSlot.PlacableItemType;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;

import de.mari_023.ae2wtlib.api.AE2wtlibAPI;
import de.mari_023.ae2wtlib.api.gui.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.item.AugmentedPatternItem;
import io.github.lounode.ae2crafting.common.logic.AugmentedRecipeMatcher;
import io.github.lounode.ae2crafting.common.logic.CraftKind;
import io.github.lounode.ae2crafting.common.logic.GridBounds;
import io.github.lounode.ae2crafting.common.logic.GridMatcher;
import io.github.lounode.ae2crafting.common.pattern.AugmentedPattern;
import io.github.lounode.ae2crafting.common.pattern.AugmentedPatternData;
import io.github.lounode.ae2crafting.registry.ModDataComponents;
import io.github.lounode.ae2crafting.registry.ModItems;
import io.github.lounode.ae2crafting.registry.ModSlotSemantics;

/**
 * ME 增广样板编码终端菜单：9x9 编码台 + 标志工作方块 + 增广合成样板输出。
 *
 * <p>不再继承 AE2 的 {@code PatternEncodingTermMenu}。那个菜单绑死了 3x3 合成台、处理样板、
 * 锻造台与切石机四套模式面板（连同 9 + 81 + 27 个 FakeSlot），而本终端的界面是一整块 9x9
 * 编码区加一条样板挂带，两套结构没有交集。这里直接继承 {@link MEStorageMenu}，
 * 保留「ME 终端」该有的一切（网络库存、搜索、排序、滚动列表），只把编码台换成自己的。</p>
 *
 * <p><b>编码格为什么用 {@link PatternEncodingLogic#getEncodedInputInv()}</b>：它是 81 格
 * {@code ConfigInventory}（AE2 处理样板的上限就是 9x9），并且已经挂在宿主的存档链上——
 * 面板形态写进部件 NBT，无线形态写进物品的 {@code CUSTOM_DATA}，关窗重开、跨维度都还在。
 * 自己 new 一个库存就得给两种宿主各加一套序列化，没有必要。</p>
 *
 * <p>产物槽只是<b>预览</b>：网格变了就问 {@link AugmentedRecipeMatcher}（原版合成台按包围盒匹配，
 * 邻居的 9x9 工作台判定器排在其后），有结果就画出来。预览与「编写样板」用的是同一次匹配，
 * 不会出现「预览有产物但按钮点了没反应」。</p>
 *
 * <p>标志工作方块由宿主保管（{@link WorkstationHost}）：线缆面板存部件 NBT，无线终端存物品的
 * {@code CUSTOM_DATA}，关窗重开、退出重进都还在。</p>
 */
public class AugmentedPatternEncodingTermMenu extends MEStorageMenu {

    public static final int GRID_WIDTH = 9;
    public static final int GRID_HEIGHT = 9;
    public static final int GRID_SLOTS = GRID_WIDTH * GRID_HEIGHT;

    /** 客户端动作名。改名等于协议变更，客户端与服务端必须同时改。 */
    private static final String ACTION_ENCODE = "encode_augmented_pattern";
    private static final String ACTION_CLEAR = "clear_augmented_grid";
    private static final String ACTION_SET_SUBSTITUTION = "set_augmented_substitution";

    public static final MenuType<AugmentedPatternEncodingTermMenu> TYPE = MenuTypeBuilder
            .create((id, playerInventory, host) -> new AugmentedPatternEncodingTermMenu(id, playerInventory, host),
                    IPatternTerminalMenuHost.class)
            .buildUnregistered(ResourceLocation.fromNamespaceAndPath(
                    AE2CraftingIntegration.MOD_ID, "augmented_pattern_terminal"));

    private final PatternEncodingLogic logic;
    /** 编码台输入格，来自宿主存档链（见类注释）。 */
    private final ConfigInventory gridInventory;
    /** 标志工作方块：宿主保管，随部件 NBT / 物品 {@code CUSTOM_DATA} 落盘。 */
    private final ConfigInventory workstationInventory;
    /** 产物预览：只读展示屏，玩家取不走。 */
    private final AppEngInternalInventory previewInventory = new AppEngInternalInventory(1);

    @Nullable
    private GenericStack previewOutput;

    /** 网格内容签名，用来判断要不要重算产物预览（每 tick 都会跑一次比较）。 */
    private int lastGridSignature;

    /**
     * 编码终端的「替换已启用 / 已禁用」开关。编号避开父类占用的 0 / 1 / 5 / 6
     * （UpgradeableMenu）与 94..101（编码终端、MEStorageMenu）。
     */
    @GuiSync(20)
    public boolean substitute;

    /**
     * 已经反填进编码矩阵的样板数据。构造时先记下输出槽里已有的那张，
     * 这样只有「本次打开界面后放进来的」样板才会触发反填，关窗重开不会覆盖玩家改过的网格。
     */
    @Nullable
    private AugmentedPatternData loadedPattern;

    /** 当前是不是「无线通用终端（WUT）」形态：客户端据此决定要不要画终端切换按钮。 */
    private final boolean universalTerminal;

    public AugmentedPatternEncodingTermMenu(int id, Inventory playerInventory, IPatternTerminalMenuHost host) {
        super(TYPE, id, playerInventory, host, true);

        this.logic = host.getLogic();
        this.gridInventory = logic.getEncodedInputInv();
        // 宿主自己保管这一格（部件 NBT / 物品 CUSTOM_DATA）；宿主不是本模组的实现时退回临时库存，
        // 只影响存档，不影响编码与匹配。
        this.workstationInventory = host instanceof WorkstationHost workstationHost
                ? workstationHost.getWorkstationInventory()
                : ConfigInventory.configStacks(1).build();

        var gridWrapper = gridInventory.createMenuWrapper();
        for (int i = 0; i < GRID_SLOTS; i++) {
            var slot = new FakeSlot(gridWrapper, i);
            slot.setHideAmount(true);
            addSlot(slot, ModSlotSemantics.AUGMENTED_GRID);
        }

        var workstationSlot = new FakeSlot(workstationInventory.createMenuWrapper(), 0);
        workstationSlot.setHideAmount(true);
        addSlot(workstationSlot, ModSlotSemantics.WORKSTATION);

        addSlot(new PreviewSlot(previewInventory, 0), SlotSemantics.CRAFTING_RESULT);
        addSlot(new RestrictedInputSlot(PlacableItemType.BLANK_PATTERN, logic.getBlankPatternInv(), 0),
                SlotSemantics.BLANK_PATTERN);
        // 样板输出槽只收增广合成样板：这里是「编写样板」的出口，放一张 AE2 原版样板进去
        // 既编不了也对应不上本模组的产物语义，还会挡住下一次编码。
        addSlot(new RestrictedInputSlot(PlacableItemType.ENCODED_PATTERN, logic.getEncodedPatternInv(), 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && stack.getItem() instanceof AugmentedPatternItem;
            }
        }, SlotSemantics.ENCODED_PATTERN);

        // 无线形态（独立终端或 WUT）才有的「量子纠缠奇点」槽。AE2WTLib 的
        // ScrollingUpgradesPanel 把这个槽当作面板列表的第一格：没装量子桥卡且为空时会隐藏它，
        // 装了卡就多显示一格。少了这一格，面板会把第一个普通升级槽误当成奇点槽。
        // 面板形态的宿主不是 WTMenuHost、没有这个子库存，跳过即可。
        if (host instanceof WTMenuHost wtHost) {
            addSlot(new RestrictedInputSlot(PlacableItemType.QE_SINGULARITY,
                    wtHost.getSubInventory(WTMenuHost.INV_SINGULARITY), 0),
                    AE2wtlibSlotSemantics.SINGULARITY);
            this.universalTerminal = AE2wtlibAPI.isUniversalTerminal(wtHost.getItemStack());
        } else {
            this.universalTerminal = false;
        }

        // 替换开关的初值来自 AE2 的编码逻辑：它随宿主 NBT 落盘，关窗重开仍在。
        this.substitute = logic.isSubstitution();
        this.loadedPattern = currentPatternData();

        registerClientAction(ACTION_ENCODE, this::encode);
        registerClientAction(ACTION_CLEAR, this::clear);
        registerClientAction(ACTION_SET_SUBSTITUTION, Boolean.class, this::applySubstitution);
    }

    /** 当前界面是不是由无线通用终端（WUT）切换过来的。 */
    public boolean isWUT() {
        return universalTerminal;
    }

    /** 宿主是不是无线形态（含独立无线终端与 WUT）；面板形态返回 false。 */
    public boolean isWirelessForm() {
        return getHost() instanceof WTMenuHost;
    }

    /**
     * 产物预览随格子变化重算。
     *
     * <p>放在 {@code broadcastChanges} 而不是槽位回调里：{@link FakeSlot} 写的是
     * {@code ConfigInventory}，不一定会走到 {@code onSlotChange}；而签名比较的成本只是
     * 81 次 {@code GenericStack.hashCode()}，每 tick 一次可以接受，且天然覆盖「开界面时缓存
     * 里已经有格子」这种情况。</p>
     */
    @Override
    public void broadcastChanges() {
        if (!isClientSide()) {
            // 先反填再算预览：同一 tick 里产物就能跟着新网格更新。
            loadPatternIfNeeded();
            updatePreviewIfNeeded();
        }
        super.broadcastChanges();
    }

    /**
     * 输出槽里放进（或换掉）一张增广合成样板时，把样板内容填回编码矩阵。
     *
     * <p>AE2 自己的 {@code PatternEncodingLogic#onChangeInventory} 只认它那四种样板类型，
     * 本模组的 {@link AugmentedPattern} 不在分派表里，所以这里自己监听输出槽。用组件数据的
     * 相等性做去重，避免每 tick 都把玩家刚改过的网格重新盖掉；取走样板时清掉记录，
     * 下次放回同一张仍会回填。</p>
     *
     * <p>只在服务端写：81 格与标志物库存都挂在宿主的存档链上，槽位同步会带给客户端。
     * 编码按钮刚产出的那张不算「玩家放进来」，在 {@link #encode()} 里同步记账跳过。</p>
     */
    private void loadPatternIfNeeded() {
        var data = currentPatternData();
        if (data == null || data.equals(loadedPattern)) {
            loadedPattern = data;
            return;
        }
        loadedPattern = data;
        applyEncodedPattern(data);
    }

    /** 输出槽当前样板携带的数据；不是本模组的增广样板（或空槽）时返回 {@code null}。 */
    @Nullable
    private AugmentedPatternData currentPatternData() {
        var stack = logic.getEncodedPatternInv().getStackInSlot(0);
        if (!(stack.getItem() instanceof AugmentedPatternItem)) {
            return null;
        }
        return stack.get(ModDataComponents.AUGMENTED_PATTERN.get());
    }

    /** 把样板的网格、标志工作方块与替换状态写回编码台。 */
    private void applyEncodedPattern(AugmentedPatternData data) {
        // 编码时写进样板的是 9x9 居中后的网格；小于 9x9 的旧样板按同一套中心对齐规则铺开。
        // data.at() 对越界坐标返回 null，不用额外做边界判断。
        int offsetX = (GRID_WIDTH - data.width()) / 2;
        int offsetY = (GRID_HEIGHT - data.height()) / 2;
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                gridInventory.setStack(y * GRID_WIDTH + x, data.at(x - offsetX, y - offsetY));
            }
        }

        // 产物容纳物按顺位接在最后一个原料之后，玩家在编码台上仍能看到、能改。
        var container = data.container();
        if (container != null) {
            int last = -1;
            for (int i = 0; i < GRID_SLOTS; i++) {
                if (gridInventory.getStack(i) != null) {
                    last = i;
                }
            }
            int slot = last + 1;
            if (slot >= GRID_SLOTS) {
                for (int i = GRID_SLOTS - 1; i >= 0; i--) {
                    if (gridInventory.getStack(i) == null) {
                        slot = i;
                        break;
                    }
                }
            }
            if (slot < GRID_SLOTS) {
                gridInventory.setStack(slot, container);
            }
        }

        var workstation = data.workstation();
        workstationInventory.setStack(0,
                workstation == null ? null : new GenericStack(workstation, 1));
        applySubstitution(data.substitute());
    }

    /**
     * 清除按钮的服务端动作：清 9x9 编码网格与标志工作方块。
     *
     * <p>标志物那一格由宿主自己落盘（部件 NBT / 物品 {@code CUSTOM_DATA}），必须走
     * {@link ConfigInventory#setStack} 才会触发宿主的 changeListener；漏掉它不只是界面上还留着
     * 工作台，关窗重开也会把旧标志物读回来。空白样板槽与样板输出槽不动。</p>
     *
     * <p>public 供游戏内测试直接调用；客户端请先走 {@link #requestClear()} 发动作。</p>
     */
    public void clear() {
        if (isClientSide()) {
            return;
        }
        gridInventory.clear();
        workstationInventory.setStack(0, null);
    }

    /** 界面按钮走这条：{@code sendClientAction} 是 protected，屏幕不在同一个包里。 */
    public void requestClear() {
        sendClientAction(ACTION_CLEAR);
    }

    /** 界面读替换开关的当前状态。 */
    public boolean isSubstitute() {
        return substitute;
    }

    /** 界面切换替换开关：客户端发动作，服务端落库；反填时直接走 {@link #applySubstitution}。 */
    public void setSubstitute(boolean value) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_SUBSTITUTION, value);
        } else {
            applySubstitution(value);
        }
    }

    private void applySubstitution(boolean value) {
        this.substitute = value;
        // 交给 AE2 的编码逻辑落盘：writeToNBT 里有 substitute 键，关窗重开仍在。
        logic.setSubstitution(value);
    }

    private void updatePreviewIfNeeded() {
        var grid = snapshotGrid();
        var station = stationItem();
        int signature = signatureOf(grid, station);
        if (signature == lastGridSignature) {
            return;
        }
        lastGridSignature = signature;
        var match = matchOutput(AugmentedRecipeMatcher.toItemStacks(grid), station);
        previewOutput = match != null ? GenericStack.fromItemStack(match.output()) : null;
        previewInventory.setItemDirect(0,
                previewOutput != null ? GenericStack.wrapInItemStack(previewOutput) : ItemStack.EMPTY);
    }

    /** 编码台的 81 格快照，行优先。空格的元素是 {@code null}（{@code ConfigInventory} 的原样表示）。 */
    private List<@Nullable GenericStack> snapshotGrid() {
        var grid = new ArrayList<GenericStack>(GRID_SLOTS);
        for (int i = 0; i < GRID_SLOTS; i++) {
            grid.add(gridInventory.getStack(i));
        }
        return grid;
    }

    /**
     * 标志物槽里的那一件物品。
     *
     * <p>只取 1 个：判定器比的是物品本身（{@code avaritia:extreme_crafting_table} 之类），
     * 数量没有意义。非物品键（流体等）按「没放」处理。</p>
     */
    private ItemStack stationItem() {
        var stack = workstationInventory.getStack(0);
        if (stack == null || stack.amount() <= 0 || !(stack.what() instanceof AEItemKey key)) {
            return ItemStack.EMPTY;
        }
        return key.toStack(1);
    }

    /**
     * 网格 + 标志物的签名。标志物也进签名：换一张工作台等于换一个 tier，产物必须跟着变。
     */
    private static int signatureOf(List<@Nullable GenericStack> grid, ItemStack station) {
        int signature = station.isEmpty() ? 0 : station.hashCode();
        for (var stack : grid) {
            signature = signature * 31 + (stack == null ? 0 : stack.hashCode());
        }
        return signature;
    }

    /**
     * 拿整张网格 + 标志物去问判定器链。
     *
     * <p>返回整个 {@link GridMatcher.Match} 而不只是产物：编码时要看 {@code kind()} 才能决定
     * 这张网格能不能居中——组合合成的「中心 + 螺旋」是位置敏感的坐标系，居中会把中心格挪走。</p>
     */
    @Nullable
    private GridMatcher.Match matchOutput(List<ItemStack> items, ItemStack station) {
        return AugmentedRecipeMatcher.match(getPlayer().level(), items, GRID_WIDTH, GRID_HEIGHT, station);
    }

    /**
     * 把当前 9x9 网格写成一张增广合成样板。
     *
     * <p>只要求空白样板槽有货。输出槽里已有样板时直接覆盖——玩家再点一次编码就是想换一张，
     * 旧样板带组件、覆盖等于销毁，这是按钮的既定语义。</p>
     *
     * <p>这里重新跑一次匹配而不是复用预览缓存：预览是「每 tick 签名变了才更新」的懒值，
     * 按钮点击与它之间可能隔着一次未广播的槽位改动。多算一次匹配远比编出错样板便宜。</p>
     *
     * <p><b>写进样板的网格是居中后的</b>：玩家把图案摆在编码台的哪个角落都无所谓，样板里
     * 一律以 9×9 的中心对齐。装配室执行时再按目标工作台的尺寸居中铺开——这样同一张样板
     * 换到 5×5 / 7×7 / 11×11 的台上都能落到正确位置。唯一的例外是组合合成器：它的网格是
     * 「中心格 + 螺旋」的位置敏感坐标系，必须原样写入。</p>
     */
    public void encode() {
        if (isClientSide()) {
            return;
        }

        var grid = snapshotGrid();
        var station = stationItem();
        var items = AugmentedRecipeMatcher.toItemStacks(grid);
        var match = matchOutput(items, station);
        if (match == null) {
            return;
        }
        var output = GenericStack.fromItemStack(match.output());
        if (output == null) {
            return;
        }
        if (logic.getBlankPatternInv().getStackInSlot(0).isEmpty()) {
            return;
        }

        var bounds = GridBounds.ofItems(items, GRID_WIDTH, GRID_HEIGHT);
        var centered = match.kind() == CraftKind.COMBINATION || bounds.isEmpty() ? grid
                : bounds.centerIn(grid, GRID_WIDTH, GRID_WIDTH, GRID_HEIGHT, null);

        // 终焉厨锅：网格里顺位最后的一件是产物容纳物。它每次合成都会消耗，
        // 编进样板的独立字段而不是占着 9×9 的一格，装配室就不会把它铺进槽位。
        @Nullable
        GenericStack container = null;
        List<@Nullable GenericStack> encodedGrid = centered;
        if (match.kind() == CraftKind.COOKING_POT) {
            int last = -1;
            for (int i = 0; i < centered.size(); i++) {
                if (centered.get(i) != null) {
                    last = i;
                }
            }
            if (last >= 0) {
                container = centered.get(last);
                var withoutContainer = new ArrayList<GenericStack>(centered);
                withoutContainer.set(last, null);
                encodedGrid = withoutContainer;
            }
        }

        var pattern = new ItemStack(ModItems.AUGMENTED_CRAFTING_PATTERN.get());
        // 标志工作方块一起写进样板：装配室据此决定接不接这张样板（见 MolecularAssemblerBlockEntity）。
        var workstationKey = station.isEmpty() ? null : AEItemKey.of(station);
        AugmentedPattern.encode(pattern, GRID_WIDTH, GRID_HEIGHT, encodedGrid, List.of(output),
                workstationKey, substitute, container);

        var consumed = logic.getBlankPatternInv().extractItem(0, 1, false);
        if (consumed.isEmpty()) {
            return;
        }
        logic.getEncodedPatternInv().setItemDirect(0, pattern);
        // 这张是自己刚编出来的，不该触发反填把玩家摆在角落的图案改成居中后的样子。
        loadedPattern = pattern.get(ModDataComponents.AUGMENTED_PATTERN.get());
    }

    /** 界面按钮走这条：{@code sendClientAction} 是 protected，屏幕不在同一个包里。 */
    public void requestEncode() {
        sendClientAction(ACTION_ENCODE);
    }

    public PatternEncodingLogic getLogic() {
        return logic;
    }

    /**
     * 81 个编码格在 {@code menu.slots} 里的下标，顺序与网格行优先一致。
     *
     * <p>给配方转移用：JEI 的 {@code InventoryAction.SET_FILTER} 认的是<b>菜单槽下标</b>
     * （服务端 {@code AEBaseMenu#setFilter} 拿它去 {@code getSlot}），不是
     * {@code ConfigInventory} 的内部序号。玩家背包槽在超类构造里就已经加进来了，
     * 所以两者的偏移不固定，只能按语义查。</p>
     */
    public int[] getGridSlotIndices() {
        var slots = getSlots(ModSlotSemantics.AUGMENTED_GRID);
        int[] indices = new int[GRID_SLOTS];
        for (int i = 0; i < GRID_SLOTS; i++) {
            indices[i] = slots.get(i).index;
        }
        return indices;
    }

    /** 标志工作方块槽的菜单槽下标。 */
    public int getWorkstationSlotIndex() {
        return getSlots(ModSlotSemantics.WORKSTATION).get(0).index;
    }

    /** 产物预览槽：能看不能拿，也不接受客户端回写（内容只由服务端按网格算出来）。 */
    private static final class PreviewSlot extends AppEngSlot {

        PreviewSlot(InternalInventory inventory, int index) {
            super(inventory, index);
            setHideAmount(true);
            // 不设 Icon.BACKGROUND_PRIMARY_OUTPUT：那是 AE2 画在自己槽位底图上的内嵌凹槽，
            // 本模组的贴图已经把产物槽画好了，叠上去会在正中多出一块灰底。
            setNotDraggable();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
