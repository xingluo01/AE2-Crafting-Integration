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
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.menu.slot.RestrictedInputSlot.PlacableItemType;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.logic.AugmentedRecipeMatcher;
import io.github.lounode.ae2crafting.common.logic.GridBounds;
import io.github.lounode.ae2crafting.common.pattern.AugmentedPattern;
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
        addSlot(new RestrictedInputSlot(PlacableItemType.ENCODED_PATTERN, logic.getEncodedPatternInv(), 0),
                SlotSemantics.ENCODED_PATTERN);

        registerClientAction(ACTION_ENCODE, this::encode);
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
            updatePreviewIfNeeded();
        }
        super.broadcastChanges();
    }

    private void updatePreviewIfNeeded() {
        var grid = snapshotGrid();
        var station = stationItem();
        int signature = signatureOf(grid, station);
        if (signature == lastGridSignature) {
            return;
        }
        lastGridSignature = signature;
        previewOutput = matchOutput(AugmentedRecipeMatcher.toItemStacks(grid), station);
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
     * 拿整张网格 + 标志物去问判定器链。返回值已经是 {@code assemble} 过的成品，不是配方结果模板。
     */
    @Nullable
    private GenericStack matchOutput(List<ItemStack> items, ItemStack station) {
        var match = AugmentedRecipeMatcher.match(getPlayer().level(), items, GRID_WIDTH, GRID_HEIGHT, station);
        return match != null ? GenericStack.fromItemStack(match.output()) : null;
    }

    /**
     * 把当前 9x9 网格写成一张增广合成样板。
     *
     * <p>与 AE2 一样要求「空白样板槽有货、已编码槽为空」：已编码槽非空时先让玩家取走，
     * 而不是悄悄覆盖——样板带组件，覆盖等于凭空销毁玩家的编码结果。</p>
     *
     * <p>这里重新跑一次匹配而不是复用预览缓存：预览是「每 tick 签名变了才更新」的懒值，
     * 按钮点击与它之间可能隔着一次未广播的槽位改动。多算一次匹配远比编出错样板便宜。</p>
     *
     * <p><b>写进样板的网格是居中后的</b>：玩家把图案摆在编码台的哪个角落都无所谓，样板里
     * 一律以 9×9 的中心对齐。装配室执行时再按目标工作台的尺寸居中铺开——这样同一张样板
     * 换到 5×5 / 7×7 / 11×11 的台上都能落到正确位置。</p>
     */
    public void encode() {
        if (isClientSide()) {
            return;
        }

        var grid = snapshotGrid();
        var station = stationItem();
        var items = AugmentedRecipeMatcher.toItemStacks(grid);
        var output = matchOutput(items, station);
        if (output == null) {
            return;
        }
        if (!logic.getEncodedPatternInv().getStackInSlot(0).isEmpty()) {
            return;
        }
        if (logic.getBlankPatternInv().getStackInSlot(0).isEmpty()) {
            return;
        }

        var bounds = GridBounds.ofItems(items, GRID_WIDTH, GRID_HEIGHT);
        var centered = bounds.isEmpty() ? grid
                : bounds.centerIn(grid, GRID_WIDTH, GRID_WIDTH, GRID_HEIGHT, null);

        var pattern = new ItemStack(ModItems.AUGMENTED_CRAFTING_PATTERN.get());
        AugmentedPattern.encode(pattern, GRID_WIDTH, GRID_HEIGHT, centered, List.of(output));

        var consumed = logic.getBlankPatternInv().extractItem(0, 1, false);
        if (consumed.isEmpty()) {
            return;
        }
        logic.getEncodedPatternInv().setItemDirect(0, pattern);
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
