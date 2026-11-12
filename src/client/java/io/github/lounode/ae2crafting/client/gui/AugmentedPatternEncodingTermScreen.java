package io.github.lounode.ae2crafting.client.gui;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.ActionItems;
import appeng.client.gui.Icon;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.core.localization.ButtonToolTips;
import appeng.menu.SlotSemantics;

import de.mari_023.ae2wtlib.api.gui.ScrollingUpgradesPanel;
import de.mari_023.ae2wtlib.api.terminal.IUniversalTerminalCapable;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;

import io.github.lounode.ae2crafting.client.AE2CraftingClient;
import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;

/**
 * ME 增广样板编码终端界面。
 *
 * <p>直接继承 {@link MEStorageScreen} 而不是 AE2 的 {@code PatternEncodingTermScreen}：
 * 后者在构造里写死四套模式面板（合成/处理/锻造/切石）和 4 个模式页签，本界面只有一块 9x9
 * 编码区，没有模式可切。</p>
 *
 * <p><b>升级槽走 AE2WTLib 的 {@link ScrollingUpgradesPanel}</b>：无线通用终端（WUT）融合的
 * 终端越多，升级槽越多（每个终端固定 2 格，再加一格量子纠缠奇点），AE2 自带的
 * {@code UpgradesPanel} 只会越画越长。实现 {@link IUniversalTerminalCapable} 后，
 * {@code addUpgradePanel} 会把奇点槽与全部升级槽收进一个可滚动面板；AEjson 把
 * {@code widgets.upgrades} 挪到 (-9999,-9999) 藏起 AE2 的那一份，避免画两遍。</p>
 *
 * <p><b>可见行数固定 2</b>：AE2WTLib 自带终端在 {@code init()} 里写的是
 * {@code setMaxRows(Math.max(2, getVisibleRows()))}（随窗口高度变化），这里按需求写死 2——
 * 无论窗口多高，升级面板最多显示两行，多出来的槽走滚动条。</p>
 *
 * <p>背景不是一张整图，而是 {@code terminalStyle} 的五片纵向切片：AE2 的终端行数随窗口高度
 * 变化，切片拼出来的界面才能跟着长高。所有固定部件（9x9 网格、产物槽、样板槽）在 AEjson 里
 * 用 {@code bottom} 定位，永远贴住底边，行数变化时不会错位。</p>
 */
public class AugmentedPatternEncodingTermScreen
        extends MEStorageScreen<AugmentedPatternEncodingTermMenu>
        implements IUniversalTerminalCapable {

    private static final String STYLE_DOC = "/screens/ae2_crafting_integration/pattern_encoding_terminal.json";

    /** 升级槽面板固定可见 2 行，不随窗口高度变化；多出来的槽由 upgradeScrollbar 滚。 */
    private static final int UPGRADE_PANEL_MAX_ROWS = 2;

    /** 面板形态（ME 线缆部件）没有 WTMenuHost，也就不挂滚动升级面板。 */
    @Nullable
    private final ScrollingUpgradesPanel upgradesPanel;

    /** 替换开关：状态在服务端，{@link #updateBeforeRender()} 每帧按菜单里的同步值刷新。 */
    private final ToggleButton substitutionsButton;

    public AugmentedPatternEncodingTermScreen(AugmentedPatternEncodingTermMenu menu,
            Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, StyleManager.loadStyleDoc(STYLE_DOC));

        // 图标按钮，位置与尺寸全由 AEjson 的 widgets.encodePattern 决定。
        widgets.add("encodePattern", new ActionButton(ActionItems.ENCODE, menu::requestEncode));

        // 两个半尺寸小按钮，照 AE2 合成编码面板的口径：S_ 前缀的 8x8 图标、不画按钮底框。
        // AEjson 把它们竖排在标志物槽右侧，按 8px 步进紧邻，判定区 6x6 与图标左上角对齐。
        var clearButton = new ActionButton(ActionItems.S_CLOSE, menu::requestClear);
        clearButton.setHalfSize(true);
        clearButton.setDisableBackground(true);
        widgets.add("clearPattern", clearButton);

        substitutionsButton = new ToggleButton(Icon.S_SUBSTITUTION_ENABLED, Icon.S_SUBSTITUTION_DISABLED,
                menu::setSubstitute);
        substitutionsButton.setHalfSize(true);
        substitutionsButton.setDisableBackground(true);
        substitutionsButton.setTooltipOn(List.of(ButtonToolTips.SubstitutionsOn.text(),
                ButtonToolTips.SubstitutionsDescEnabled.text()));
        substitutionsButton.setTooltipOff(List.of(ButtonToolTips.SubstitutionsOff.text(),
                ButtonToolTips.SubstitutionsDescDisabled.text()));
        substitutionsButton.setState(menu.isSubstitute());
        widgets.add("craftingSubstitutions", substitutionsButton);

        if (menu.isWirelessForm()) {
            if (menu.isWUT()) {
                // WUT 里要能切回其它已融合的终端。
                addToLeftToolbar(cycleTerminalButton());
            }
            this.upgradesPanel = addUpgradePanel(widgets, menu);
        } else {
            this.upgradesPanel = null;
        }
    }

    /** 接口里唯一需要自己实现的方法；面板形态返回 null，此时所有用到它的方法都有形态守卫。 */
    @Override
    @Nullable
    public WTMenuHost getHost() {
        return getMenu().getHost() instanceof WTMenuHost host ? host : null;
    }

    @Override
    public void init() {
        super.init();
        if (upgradesPanel != null) {
            upgradesPanel.setMaxRows(UPGRADE_PANEL_MAX_ROWS);
        }
    }

    /** 替换开关由服务端 @GuiSync 推过来，界面只跟着画，不在客户端自己记账。 */
    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        substitutionsButton.setState(getMenu().isSubstitute());
    }

    /**
     * 产物预览槽上的滚轮切换候选：多候选时（多模组厨锅最容易撞车）滚一格换一条配方。
     * 其余位置的滚轮仍交给 ME 列表，不抢它的滚动。
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (getMenu().candidateCount() > 1 && isOverPreviewSlot(mouseX, mouseY)) {
            getMenu().requestCycleCandidate(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isOverPreviewSlot(double mouseX, double mouseY) {
        var slots = getMenu().getSlots(SlotSemantics.CRAFTING_RESULT);
        if (slots.isEmpty()) {
            return false;
        }
        var slot = slots.getFirst();
        return mouseX >= leftPos + slot.x && mouseX < leftPos + slot.x + 16
                && mouseY >= topPos + slot.y && mouseY < topPos + slot.y + 16;
    }

    /** WUT 的终端切换热键；面板形态不转发，避免走到 getHost() 的 null 上。 */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        return handled || (upgradesPanel != null && checkForTerminalKeys(keyCode, scanCode));
    }

    @Override
    protected void openHelp() {
        AE2CraftingClient.openTerminalGuide(getPlayer());
    }
}
