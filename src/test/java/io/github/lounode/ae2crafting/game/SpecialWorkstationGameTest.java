package io.github.lounode.ae2crafting.game;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.logic.CraftRules;

/**
 * 特殊工作台兼容的登记链路。
 *
 * <p>整套规则靠<b>物品 id 字符串</b>工作，写错一个字母不会报错，只会静默退化成普通合成。
 * 这个测试在开发运行期（邻居模组都在）核对 id 真实存在、并且确实被登记进 {@link CraftRules}。</p>
 */
@GameTestHolder(AE2CraftingIntegration.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpecialWorkstationGameTest {

    private static final String[] WORKSTATIONS = {
            "extendedcrafting:ender_crafter",
            "extendedcrafting:flux_crafter",
            "extendedcrafting:crafting_core",
            "avaritia_delight:extreme_cooking_pot",
            "farmersdelight:cooking_pot"
    };

    private static final String[] CATALYSTS = {
            "extendedcrafting:ender_alternator",
            "extendedcrafting:flux_alternator",
            "extendedcrafting:pedestal"
    };

    @GameTest(template = "empty")
    public void workstationIdsExistAndAreRegistered(GameTestHelper helper) {
        for (var id : WORKSTATIONS) {
            var stack = item(helper, id);
            helper.assertTrue(CraftRules.isSpecialWorkstation(stack),
                    id + " 没有登记成特殊工作台");
        }
        for (var id : CATALYSTS) {
            var stack = item(helper, id);
            helper.assertTrue(CraftRules.catalystOf(stack) != null,
                    id + " 没有登记成发电机或基座");
        }
        helper.assertTrue(BuiltInRegistries.RECIPE_TYPE
                        .getOptional(ResourceLocation.fromNamespaceAndPath("avaritia_delight", "extreme_cooking"))
                        .isPresent(),
                "终焉厨锅的配方类型没注册，判定器会永远返回 null");
        helper.succeed();
    }

    /** 多态合成的 API 类必须真的在运行期存在，否则反射选择器会永远静默回退。 */
    @GameTest(template = "empty")
    public void polymorphApiIsPresent(GameTestHelper helper) {
        try {
            Class.forName("com.illusivesoulworks.polymorph.api.PolymorphApi");
        } catch (ClassNotFoundException e) {
            helper.fail("Polymorph API 类不存在，反射选择器会永远回退到第一个候选");
            return;
        }
        helper.succeed();
    }

    /** 取一件真实物品；id 写错时这里先失败，而不是等到界面上「怎么没反应」。 */
    private static ItemStack item(GameTestHelper helper, String id) {
        var key = ResourceLocation.parse(id);
        var holder = BuiltInRegistries.ITEM.getOptional(key);
        helper.assertTrue(holder.isPresent(), "物品 id 不存在：" + id);
        return new ItemStack(holder.orElseThrow());
    }
}
