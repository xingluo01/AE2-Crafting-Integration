package io.github.lounode.ae2crafting.common.menu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;

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
            addSlot(new AppEngSlot(inventory, AssemblerSlots.grid(i / 9, i % 9)),
                    ModSlotSemantics.GRID);
        }
        for (int i = 0; i < AssemblerSlots.CATALYST_SLOTS; i++) {
            addSlot(new AppEngSlot(inventory, AssemblerSlots.catalyst(i)),
                    ModSlotSemantics.CATALYST);
        }
        // 产物槽的「只出不进」由 AssemblerInventory#isItemValid 决定，AppEngSlot#mayPlace 会转发过去。
        addSlot(new AppEngSlot(inventory, AssemblerSlots.output()),
                ModSlotSemantics.OUTPUT);
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
}
