package io.github.lounode.ae2crafting.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.ProgressBar;

import io.github.lounode.ae2crafting.client.AE2CraftingClient;
import io.github.lounode.ae2crafting.common.menu.MolecularAssemblerMenu;
import io.github.lounode.ae2crafting.registry.ModSlotSemantics;

/**
 * 通用分子装配室界面。
 *
 * <p>槽位坐标、背景、标题、进度条与槽底样式全部来自 AEjson；本类只补三件 JSON 表达不了的事：
 * 绑定进度条控件、把 9x9 空白底板铺上槽底、以及把指南按钮指向本模组的指南书。</p>
 *
 * <p>继承 {@link UpgradeableScreen} 而不是 {@code AEBaseScreen}：父类会把右侧升级面板
 * （{@code widgets.upgrades}，位置来自 {@code common.json}）挂上，左侧竖直工具栏则由
 * {@code AEBaseScreen} 在构造时自动加指南按钮。</p>
 */
public class MolecularAssemblerScreen extends UpgradeableScreen<MolecularAssemblerMenu> {

    private static final String SIDE_PANEL = "sidePanel";
    private static final String SLOT_BACKGROUND = "slotBackground";

    /** 构造时拷一份，避免循环里反复改写 AEjson 里同一个 Blitter 实例的位置。 */
    private final Blitter gridSlotBackground;

    public MolecularAssemblerScreen(MolecularAssemblerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title,
                StyleManager.loadStyleDoc("/screens/ae2_crafting_integration/molecular_assembler.json"));

        this.gridSlotBackground = style.getImage(SLOT_BACKGROUND).copy();
        widgets.add("progressBar", new ProgressBar(
                menu, style.getImage("progressBar"), ProgressBar.Direction.HORIZONTAL));
    }

    @Override
    protected void openHelp() {
        AE2CraftingClient.openGuide(getPlayer());
    }

    /**
     * AE2 把 {@code renderBg} 定为 final，留 {@code drawBG} 给子类补背景层。
     */
    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY,
            float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        drawSidePanel(graphics, offsetX, offsetY);
        drawGridSlots(graphics, offsetX, offsetY);
    }

    /**
     * 输出槽与进度条所在的浮层在贴图里脱离主面板，背景 {@code srcRect} 只截 176 宽，这里补画右挂部分。
     * 位置同样读 AEjson 的 {@code widgets.sidePanel}，代码不重复坐标。
     */
    private void drawSidePanel(GuiGraphics graphics, int offsetX, int offsetY) {
        var position = style.getWidget(SIDE_PANEL);
        style.getImage(SIDE_PANEL)
                .dest(offsetX + position.getLeft(), offsetY + position.getTop())
                .blit(graphics);
    }

    /**
     * 9x9 底板在贴图里是纯色空白，槽底直接复用背包栏那一格（{@code images.slotBackground}）：
     * 16×16、顶 1px 深线、四周 2px 背景间隔，与 AE2 {@code Icon.SLOT_BACKGROUND} 同规格。
     *
     * <p>不用 {@code graphics.fill} 手绘：上一版按 18×18 四边描边画，盖掉了底板的 203 底色，
     * 相邻边框又叠成 2px 深线，视觉上比 AE2 的槽位重一整圈。</p>
     */
    private void drawGridSlots(GuiGraphics graphics, int offsetX, int offsetY) {
        for (var slot : getMenu().getSlots(ModSlotSemantics.GRID)) {
            gridSlotBackground
                    .dest(offsetX + slot.x, offsetY + slot.y)
                    .blit(graphics);
        }
    }
}
