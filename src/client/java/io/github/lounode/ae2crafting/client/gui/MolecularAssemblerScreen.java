package io.github.lounode.ae2crafting.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.ProgressBar;

import io.github.lounode.ae2crafting.client.AE2CraftingClient;
import io.github.lounode.ae2crafting.client.gui.widgets.CoveringProgressBar;
import io.github.lounode.ae2crafting.common.menu.MolecularAssemblerMenu;

/**
 * 通用分子装配室界面。
 *
 * <p>槽位坐标、背景、标题、进度条底图全部来自 AEjson；本类只补三件 JSON 表达不了的事：
 * 绑定进度条控件、补画右侧挂带、把指南按钮指向本模组的指南书。</p>
 *
 * <p><b>9x9 槽底不在这里画</b>：网格槽实现了 {@code IOptionalSlot} 且
 * {@code isRenderDisabled()} 恒真，{@code AEBaseScreen#renderBg} 会按
 * {@code isSlotEnabled()} 以 1.0 / 0.2 的 alpha 画 {@code Icon.SLOT_BACKGROUND}——
 * 这正是 AE2 原版分子装配室对「空格子」的处理：没被样板填的格子几乎隐形，
 * 填了料的格子才显出完整槽框。之前自己叠一层半透明黑反而比原版重。</p>
 *
 * <p>继承 {@link UpgradeableScreen} 而不是 {@code AEBaseScreen}：父类会把右侧升级面板
 * （{@code widgets.upgrades}）挂上，左侧竖直工具栏则由 {@code AEBaseScreen} 自动加指南按钮。</p>
 */
public class MolecularAssemblerScreen extends UpgradeableScreen<MolecularAssemblerMenu> {

    private static final String SIDE_PANEL = "sidePanel";

    private final ProgressBar progressBar;

    public MolecularAssemblerScreen(MolecularAssemblerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title,
                StyleManager.loadStyleDoc("/screens/ae2_crafting_integration/molecular_assembler.json"));

        this.progressBar = new CoveringProgressBar(menu, style.getImage("progressBar"),
                ProgressBar.Direction.HORIZONTAL);
        widgets.add("progressBar", progressBar);
    }

    @Override
    protected void openHelp() {
        AE2CraftingClient.openGuide(getPlayer());
    }

    /**
     * AE2 原版分子装配室也在这里把进度 tooltip 换成百分比；本模组照做，只是百分比按
     * {@code current / max} 算而不是直接把进度值当百分数（档位表改了也不会显示错）。
     */
    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        int max = getMenu().getMaxProgress();
        int current = Math.min(getMenu().getCurrentProgress(), max);
        int percent = max > 0 ? current * 100 / max : 0;
        progressBar.setFullMsg(Component.literal(percent + "%"));
    }

    /**
     * AE2 把 {@code renderBg} 定为 final，留 {@code drawBG} 给子类补背景层。
     */
    @Override
    public void drawBG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY,
            float partialTicks) {
        super.drawBG(graphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
        drawSidePanel(graphics, offsetX, offsetY);
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
}
