package io.github.lounode.ae2crafting.client.integration.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;

import org.jetbrains.annotations.Nullable;

import com.blakebr0.extendedcrafting.api.crafting.ITableRecipe;
import com.blakebr0.extendedcrafting.init.ModBlocks;

import committee.nova.mods.avaritia.api.common.crafting.ITierCraftingRecipe;

import io.github.lounode.ae2crafting.common.logic.RecipeGridCategories.Station;

/**
 * 「这份配方该配哪张工作台」——配方转移时自动填标志物槽的那一步。
 *
 * <p><b>为什么要读配方自己的 tier，而不是看玩家点的哪个类别</b>：两家的配方查看器都会把
 * 「可升级配方」同时列进更高档的类别（{@code tier >= recipe.getTier()}），但运行时的
 * {@code matches} 判的是<b>等值</b>档位。所以在终极台页签里点一个幽匿级配方，标志物必须放
 * 幽匿台才能编码成功；按类别推断会直接编不出产物。配方没写 tier（返回 0，表示不限档）时
 * 才退回按类别边长推断的档位。</p>
 *
 * <p><b>为什么所有模组引用都藏在 {@code *Unchecked} 方法里</b>：{@code instanceof ITableRecipe}
 * 和 {@code ModBlocks.*.get()} 都会解析邻居的类。{@code ModList.isLoaded} 的判断必须先执行，
 * 再让 JVM 去解析这些符号引用；把它们拆进独立方法，是为了让「方法体解析」发生在守卫之后，
 * 而不是随外层方法一起被验证。</p>
 */
final class JeiTransferStations {

    private static final String EXTENDED_CRAFTING = "extendedcrafting";
    private static final String AVARITIA = "avaritia";

    /** 有效档位 1..4；0 或不认识的档位一律用兜底值。 */
    static int normalizeTier(int recipeTier, int fallbackTier) {
        return recipeTier >= 1 && recipeTier <= 4 ? recipeTier : fallbackTier;
    }

    /**
     * 配方自己声明的档位；不是这两家的配方、或没写 tier 时返回 0。
     */
    static int recipeTier(Object recipeBase) {
        if (!(recipeBase instanceof RecipeHolder<?> holder)) {
            return 0;
        }
        int tier = extendedCraftingTier(holder.value());
        return tier != 0 ? tier : avaritiaTier(holder.value());
    }

    /**
     * 标志物槽该放什么。{@code null} 表示<b>不动</b>（原版合成不需要工作台，玩家原来放什么就留着）。
     */
    static @Nullable ItemStack station(Station kind, int recipeTier, int fallbackTier) {
        int tier = normalizeTier(recipeTier, fallbackTier);
        return switch (kind) {
            case NONE -> null;
            case EXTENDED_CRAFTING -> extendedCraftingStation(tier);
            case AVARITIA_TABLE -> avaritiaStation(tier);
            case AVARITIA_SMITHING -> avaritiaSmithingStation();
            case ENDER_CRAFTER -> enderCrafterStation();
            case FLUX_CRAFTER -> fluxCrafterStation();
            case COMBINATION -> combinationStation();
            case COOKING_POT -> cookingPotStation();
            case FARMERS_COOKING_POT -> farmersCookingPotStation();
        };
    }

    private static ItemStack enderCrafterStation() {
        if (!ModList.get().isLoaded(EXTENDED_CRAFTING)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(ModBlocks.ENDER_CRAFTER.get());
    }

    private static ItemStack fluxCrafterStation() {
        if (!ModList.get().isLoaded(EXTENDED_CRAFTING)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(ModBlocks.FLUX_CRAFTER.get());
    }

    private static ItemStack combinationStation() {
        if (!ModList.get().isLoaded(EXTENDED_CRAFTING)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(ModBlocks.CRAFTING_CORE.get());
    }

    /**
     * 终焉厨锅按物品 id 查：无尽乐事没有编译期依赖，这里不能出现它的类名。
     * 查不到（模组没装或改了 id）时返回空栈，转移只铺原料、不动标志物。
     */
    private static ItemStack cookingPotStation() {
        return BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.fromNamespaceAndPath("avaritia_delight", "extreme_cooking_pot"))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    /** 农夫乐事本体厨锅，同样按 id 查：它只在 localRuntime。 */
    private static ItemStack farmersCookingPotStation() {
        return BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.fromNamespaceAndPath("farmersdelight", "cooking_pot"))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private static int extendedCraftingTier(Recipe<?> recipe) {
        if (!ModList.get().isLoaded(EXTENDED_CRAFTING)) {
            return 0;
        }
        return extendedCraftingTierUnchecked(recipe);
    }

    private static int extendedCraftingTierUnchecked(Recipe<?> recipe) {
        return recipe instanceof ITableRecipe table ? table.getTier() : 0;
    }

    private static int avaritiaTier(Recipe<?> recipe) {
        if (!ModList.get().isLoaded(AVARITIA)) {
            return 0;
        }
        return avaritiaTierUnchecked(recipe);
    }

    private static int avaritiaTierUnchecked(Recipe<?> recipe) {
        return recipe instanceof ITierCraftingRecipe table ? table.getTier() : 0;
    }

    private static ItemStack extendedCraftingStation(int tier) {
        if (!ModList.get().isLoaded(EXTENDED_CRAFTING)) {
            return ItemStack.EMPTY;
        }
        return extendedCraftingStationUnchecked(tier);
    }

    /** 合成拓展四档台：基础 3×3 / 高级 5×5 / 精英 7×7 / 终极 9×9。 */
    private static ItemStack extendedCraftingStationUnchecked(int tier) {
        return switch (tier) {
            case 1 -> new ItemStack(ModBlocks.BASIC_TABLE.get());
            case 2 -> new ItemStack(ModBlocks.ADVANCED_TABLE.get());
            case 3 -> new ItemStack(ModBlocks.ELITE_TABLE.get());
            default -> new ItemStack(ModBlocks.ULTIMATE_TABLE.get());
        };
    }

    private static ItemStack avaritiaStation(int tier) {
        if (!ModList.get().isLoaded(AVARITIA)) {
            return ItemStack.EMPTY;
        }
        return avaritiaStationUnchecked(tier);
    }

    /** 无尽重生四档台：幽匿 5×5 / 下界 7×7 / 末地 9×9 / 终极 11×11。 */
    private static ItemStack avaritiaStationUnchecked(int tier) {
        // 两家的入口类都叫 ModBlocks，只能有一个用短名；无尽这边写全限定名。
        return switch (tier) {
            case 1 -> new ItemStack(committee.nova.mods.avaritia.init.registry.ModBlocks.sculk_crafting_table.get());
            case 2 -> new ItemStack(committee.nova.mods.avaritia.init.registry.ModBlocks.nether_crafting_table.get());
            case 3 -> new ItemStack(committee.nova.mods.avaritia.init.registry.ModBlocks.end_crafting_table.get());
            default -> new ItemStack(committee.nova.mods.avaritia.init.registry.ModBlocks.extreme_crafting_table.get());
        };
    }

    private static ItemStack avaritiaSmithingStation() {
        if (!ModList.get().isLoaded(AVARITIA)) {
            return ItemStack.EMPTY;
        }
        return avaritiaSmithingStationUnchecked();
    }

    private static ItemStack avaritiaSmithingStationUnchecked() {
        return new ItemStack(committee.nova.mods.avaritia.init.registry.ModBlocks.extreme_smithing_table.get());
    }

    private JeiTransferStations() {
    }
}
