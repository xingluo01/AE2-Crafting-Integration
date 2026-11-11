package io.github.lounode.ae2crafting.game;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import appeng.api.features.GridLinkables;
import appeng.api.parts.IPartItem;
import appeng.items.parts.PartItem;
import appeng.items.tools.powered.WirelessTerminalItem;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;
import io.github.lounode.ae2crafting.common.part.AugmentedPatternEncodingTerminalPart;
import io.github.lounode.ae2crafting.registry.ModBlocks;
import io.github.lounode.ae2crafting.registry.ModItems;
import io.github.lounode.ae2crafting.registry.ModMenuTypes;

/**
 * 基线注册的运行时断言：三类对象在真实注册表里能取到，且两个终端拿在手里就是 AE2 认的类型。
 *
 * <p>纯 JUnit 看不到注册表，这几条只能靠游戏内跑。启动方式：{@code ./gradlew runGameTestServer}。
 * 模板 {@code empty} 由 {@code src/test/resources/data/ae2_crafting_integration/structure/empty.nbt} 提供，
 * 缺这个空结构会在开测前抛 {@code Missing test structure}。</p>
 */
@GameTestHolder(AE2CraftingIntegration.MOD_ID)
@PrefixGameTestTemplate(false)
public class ModLoadGameTest {

    @GameTest(template = "empty")
    public void baselineObjectsAreRegistered(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.MOLECULAR_ASSEMBLER.get() != null, "molecular assembler block missing");
        helper.assertTrue(ModItems.PATTERN_ENCODING_TERMINAL.get() != null, "panel terminal item missing");
        helper.assertTrue(ModItems.WIRELESS_TERMINAL.get() != null, "wireless terminal item missing");
        helper.succeed();
    }

    /**
     * 终端接线：面板形态必须是能建 Part 的 {@link IPartItem}，无线形态必须是 AE2 无线终端，
     * 且已在 {@link GridLinkables} 里登记（漏登记时界面照开、只是永远连不上无线接入点）。
     *
     * <p>菜单类型必须是本模组的自有类型：客户端按 {@code menu.getType()} 查界面工厂，
     * 这条断言挡的就是「界面文档复制过来了但一直没人读」——那种情况编译、启动全都正常。</p>
     */
    @GameTest(template = "empty")
    public void terminalsAreWiredToAe2(GameTestHelper helper) {
        var partItem = ModItems.PATTERN_ENCODING_TERMINAL.get();
        helper.assertTrue(partItem instanceof IPartItem, "panel terminal is not an IPartItem");
        helper.assertTrue(partItem instanceof PartItem<?>, "panel terminal is not a PartItem");
        helper.assertTrue(AugmentedPatternEncodingTerminalPart.class.equals(partItem.getPartClass()),
                "panel terminal creates the wrong part class");

        var wireless = ModItems.WIRELESS_TERMINAL.get();
        helper.assertTrue(wireless instanceof WirelessTerminalItem, "wireless terminal is not a WirelessTerminalItem");
        helper.assertTrue(wireless.getMenuType() == AugmentedPatternEncodingTermMenu.TYPE,
                "wireless terminal does not open the augmented pattern encoding menu");
        helper.assertTrue(ModMenuTypes.AUGMENTED_PATTERN_TERMINAL.get() == AugmentedPatternEncodingTermMenu.TYPE,
                "augmented pattern terminal menu type is not registered");
        helper.assertTrue(GridLinkables.get(wireless) != null, "wireless terminal is not registered for grid linking");
        helper.succeed();
    }
}
