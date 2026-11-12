package io.github.lounode.ae2crafting.integration;

import net.neoforged.fml.ModList;

import io.github.lounode.ae2crafting.common.logic.AugmentedRecipeMatcher;
import io.github.lounode.ae2crafting.common.logic.CraftKind;
import io.github.lounode.ae2crafting.integration.avaritia.AvaritiaSmithingMatcher;
import io.github.lounode.ae2crafting.integration.avaritia.AvaritiaTableMatcher;
import io.github.lounode.ae2crafting.integration.avaritiadelight.AvaritiaDelightMachines;
import io.github.lounode.ae2crafting.integration.avaritiadelight.ExtremeCookingPotMatcher;
import io.github.lounode.ae2crafting.integration.extendedcrafting.ExtendedCraftingAlternatorMatcher;
import io.github.lounode.ae2crafting.integration.extendedcrafting.ExtendedCraftingCombinationMatcher;
import io.github.lounode.ae2crafting.integration.extendedcrafting.ExtendedCraftingMachines;
import io.github.lounode.ae2crafting.integration.extendedcrafting.ExtendedCraftingTableMatcher;

/**
 * 邻居模组的总入口：**唯一**一处用 {@code isLoaded} 分流的地方。
 *
 * <p>类常量池里可以出现 {@code integration/<邻居>} 的类名，但那些类只有在 {@code isLoaded} 为真时
 * 才会被 {@code new}（即初始化）。它们的字段与方法签名里不出现邻居的类型，邻居的类型只出现在
 * 方法体里，而方法体里的符号引用是首次执行时才解析的——所以邻居缺席时既不会加载失败，
 * 也不会把邻居的类拖进初始化。</p>
 */
public final class ModIntegrations {

    private static final String EXTENDED_CRAFTING = "extendedcrafting";
    private static final String AVARITIA = "avaritia";
    private static final String AVARITIA_DELIGHT = "avaritia_delight";

    private ModIntegrations() {
    }

    /**
     * 把已安装的 9×9 工作台挂到配方匹配链上。注册顺序即优先级，排在原版合成台之后。
     *
     * <p>由 {@code AE2CraftingIntegration#commonSetup} 在注册表冻结之后调用一次；
     * 重复调用会让同一条链里出现两份同样的判定器（结果不变，只是多跑一遍）。</p>
     */
    public static void registerRecipeMatchers() {
        if (ModList.get().isLoaded(EXTENDED_CRAFTING)) {
            ExtendedCraftingMachines.registerRules();
            // 特殊工作台的判定器必须排在普通表格之前：它们同样吃 3×3 / 9×9 图案，
            // 表格判定器不限标志物，会把同一批原料先解释成表格配方。
            AugmentedRecipeMatcher.registerIntegration(
                    new ExtendedCraftingAlternatorMatcher(CraftKind.ENDER_CRAFTER));
            AugmentedRecipeMatcher.registerIntegration(
                    new ExtendedCraftingAlternatorMatcher(CraftKind.FLUX_CRAFTER));
            AugmentedRecipeMatcher.registerIntegration(new ExtendedCraftingCombinationMatcher());
            AugmentedRecipeMatcher.registerIntegration(new ExtendedCraftingTableMatcher());
        }
        if (ModList.get().isLoaded(AVARITIA)) {
            AugmentedRecipeMatcher.registerIntegration(new AvaritiaTableMatcher());
            // 无尽锻造不是网格配方：五格十字，靠标志物「无尽锻造台」分流。排在表格之后，两者互斥。
            AugmentedRecipeMatcher.registerIntegration(new AvaritiaSmithingMatcher());
        }
        if (ModList.get().isLoaded(AVARITIA_DELIGHT)) {
            AvaritiaDelightMachines.registerRules();
            AugmentedRecipeMatcher.registerIntegration(new ExtremeCookingPotMatcher());
        }
    }
}
