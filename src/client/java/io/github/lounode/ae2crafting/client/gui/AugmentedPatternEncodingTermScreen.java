package io.github.lounode.ae2crafting.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.api.config.ActionItems;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.ActionButton;

import io.github.lounode.ae2crafting.client.AE2CraftingClient;
import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;

/**
 * ME 增广样板编码终端界面。
 *
 * <p>直接继承 {@link MEStorageScreen} 而不是 AE2 的 {@code PatternEncodingTermScreen}：
 * 后者在构造里写死四套模式面板（合成/处理/锻造/切石）和 4 个模式页签，本界面只有一块 9x9
 * 编码区，没有模式可切。</p>
 *
 * <p>背景不是一张整图，而是 {@code terminalStyle} 的五片纵向切片：AE2 的终端行数随窗口高度
 * 变化，切片拼出来的界面才能跟着长高。所有固定部件（9x9 网格、产物槽、样板槽）在 AEjson 里
 * 用 {@code bottom} 定位，永远贴住底边，行数变化时不会错位。</p>
 */
public class AugmentedPatternEncodingTermScreen
        extends MEStorageScreen<AugmentedPatternEncodingTermMenu> {

    private static final String STYLE_DOC = "/screens/ae2_crafting_integration/pattern_encoding_terminal.json";

    public AugmentedPatternEncodingTermScreen(AugmentedPatternEncodingTermMenu menu,
            Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, StyleManager.loadStyleDoc(STYLE_DOC));

        // 图标按钮，位置与尺寸全由 AEjson 的 widgets.encodePattern 决定。
        widgets.add("encodePattern", new ActionButton(ActionItems.ENCODE, menu::requestEncode));
    }

    @Override
    protected void openHelp() {
        AE2CraftingClient.openTerminalGuide(getPlayer());
    }
}
