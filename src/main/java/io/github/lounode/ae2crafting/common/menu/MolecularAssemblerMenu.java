package io.github.lounode.ae2crafting.common.menu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.client.Point;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.IOptionalSlot;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.block.entity.AssemblerAcceleration;
import io.github.lounode.ae2crafting.common.block.entity.MolecularAssemblerBlockEntity;
import io.github.lounode.ae2crafting.registry.ModSlotSemantics;

/**
 * 通用分子装配室菜单。
 *
 * <p>槽位只按语义注册，坐标全部由 AEjson 决定：{@link ModSlotSemantics} 的注册名
 * 必须与 {@code molecular_assembler.json} 的 {@code slots} 键一致。</p>
 *
 * <p>继承 {@link UpgradeableMenu} 而不是 {@code AEBaseMenu}，为的是拿到 AE2 标准的升级槽装配：
 * 父类构造会按 {@code getHost().getUpgrades()} 的数量补上 {@code SlotSemantics.UPGRADE} 槽，
 * 界面侧 {@code UpgradesPanel} 再把它们排到右侧。</p>
 *
 * <p><b>{@code @GuiSync} 编号必须避开 0 / 1 / 5 / 6</b>：{@link UpgradeableMenu} 已经占用
 * （{@code rsMode} / {@code fzMode} / {@code cMode} / {@code schedulingMode}）。
 * 这里从 10 起编，编号是客户端/服务端协议，不可复用、不可改。</p>
 */
public class MolecularAssemblerMenu extends UpgradeableMenu<MolecularAssemblerBlockEntity>
        implements IProgressProvider {

    /** 显式三参 lambda：父类另有一个接受 MenuType 的工厂形状，方法引用会二义。 */
    public static final MenuType<MolecularAssemblerMenu> TYPE = MenuTypeBuilder
            .create((id, playerInventory, host) -> new MolecularAssemblerMenu(id, playerInventory, host),
                    MolecularAssemblerBlockEntity.class)
            .buildUnregistered(ResourceLocation.fromNamespaceAndPath(
                    AE2CraftingIntegration.MOD_ID, "molecular_assembler"));

    @GuiSync(10)
    public int progress;
    @GuiSync(11)
    public int maxProgress = AssemblerAcceleration.FULL_PROGRESS;

    public MolecularAssemblerMenu(int id, Inventory playerInventory, MolecularAssemblerBlockEntity host) {
        super(TYPE, id, playerInventory, host);
        this.progress = host.getProgress();
        this.maxProgress = host.getMaxProgress();
    }

    /**
     * 只加机器自身的槽。玩家背包与升级槽分别由 {@link UpgradeableMenu} 的构造负责，
     * 这里重复调用 {@code createPlayerInventorySlots} 会直接抛「already created」。
     */
    @Override
    protected void setupInventorySlots() {
        var inventory = getHost().getInventory();

        for (int i = 0; i < AssemblerSlots.GRID_SLOTS; i++) {
            addSlot(new AssemblerGridSlot(inventory, AssemblerSlots.grid(i / 9, i % 9)),
                    ModSlotSemantics.GRID);
        }
        // 标志物栏：这一排是配置槽，允许手动放入——机器靠它决定「本机扮演哪张工作台」，
        // 没有它就没法开门见山地拒绝不该接的样板（见 MolecularAssemblerBlockEntity#pushPattern）。
        for (int i = 0; i < AssemblerSlots.CATALYST_SLOTS; i++) {
            addSlot(new AppEngSlot(inventory, AssemblerSlots.catalyst(i)),
                    ModSlotSemantics.CATALYST);
        }
        // 产物槽的「只出不进」由 AssemblerInventory#isItemValid 决定，AppEngSlot#mayPlace 会转发过去。
        addSlot(new AppEngSlot(inventory, AssemblerSlots.output()),
                ModSlotSemantics.OUTPUT);
    }

    /**
     * 快捷移动时让升级槽排在最前。
     *
     * <p>{@code AEBaseMenu#getQuickMoveDestinationSlots} 按
     * {@code SlotSemantic#quickMovePriority()} 倒序排目标槽，而 AE2 的 {@code UPGRADE} 语义
     * 默认优先级是 0，与机器自己的标志物栏同分。玩家在背包里 Shift 点加速卡时，卡片会先被
     * 塞进标志物栏（那一排对任何物品都放行），升级槽反而抢不到。给升级槽一个明显更高的
     * 优先级后，加速卡先去升级槽；其它物品在升级槽的 {@code RestrictedInputSlot} 处被拒，
     * 依旧按原顺序落进标志物栏。</p>
     */
    @Override
    protected int getQuickMovePriority(Slot slot) {
        if (getSlotSemantic(slot) == SlotSemantics.UPGRADE) {
            return 100;
        }
        return super.getQuickMovePriority(slot);
    }

    @Override
    public int getCurrentProgress() {
        return progress;
    }

    @Override
    public int getMaxProgress() {
        return maxProgress;
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            progress = getHost().getProgress();
            maxProgress = getHost().getMaxProgress();
        }
        super.broadcastChanges();
    }

    /**
     * 9×9 网格里的格子：**只由样板供应器填、只由机器自己清**。
     *
     * <p>玩家既放不进去也拿不出来——网格是配方的<b>位置</b>，手动挪一格就等于换了一个配方，
     * 机器却还按旧样板推进度，产物必然对不上。要换配方就让供应器重新发配。</p>
     *
     * <p>实现 {@link IOptionalSlot} 是故意的：{@code AEBaseScreen#renderBg} 会对
     * {@code isRenderDisabled()} 为真的可选槽画 {@code Icon.SLOT_BACKGROUND}，alpha 取
     * {@code isSlotEnabled() ? 1.0 : 0.2}。这正是 AE2 原版分子装配室的槽位策略——
     * 没被样板填的格子几乎隐形，填了料的格子才显出完整槽框；比自己叠一层黑底干净。</p>
     *
     * <p>{@code isSlotEnabled()} 照「这一格现在有没有东西」来算，客户端拿到的槽内容来自菜单同步，
     * 所以界面上的高亮/变暗与机器的真实状态一致。注意<b>不能</b>反过来用 {@code getItem()} 判断——
     * 父类的 {@code getItem()} 会先问 {@code isSlotEnabled()}，那样是无限递归。</p>
     */
    private static final class AssemblerGridSlot extends AppEngSlot implements IOptionalSlot {

        AssemblerGridSlot(InternalInventory inventory, int index) {
            super(inventory, index);
            setHideAmount(false);
        }

        @Override
        public boolean isSlotEnabled() {
            return !getInventory().getStackInSlot(getSlotIndex()).isEmpty();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        /** 恒真：让 AE2 接管全部 81 格的槽底绘制，启用/禁用只由 alpha 区分。 */
        @Override
        public boolean isRenderDisabled() {
            return true;
        }

        /** {@code Icon.SLOT_BACKGROUND} 是 18×18，比 16×16 的槽内容区各外扩 1px。 */
        @Override
        public Point getBackgroundPos() {
            return new Point(x - 1, y - 1);
        }
    }
}
