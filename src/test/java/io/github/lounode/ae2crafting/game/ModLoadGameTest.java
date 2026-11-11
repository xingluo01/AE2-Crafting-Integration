package io.github.lounode.ae2crafting.game;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.registry.ModBlocks;
import io.github.lounode.ae2crafting.registry.ModItems;

/**
 * 基线注册的运行时断言：三类对象在真实注册表里能取到。
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
}
