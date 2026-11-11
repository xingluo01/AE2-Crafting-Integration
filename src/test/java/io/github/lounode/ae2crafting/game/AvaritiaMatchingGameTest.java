package io.github.lounode.ae2crafting.game;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import committee.nova.mods.avaritia.common.crafting.recipe.ExtremeSmithingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ShapedTableCraftingRecipe;
import committee.nova.mods.avaritia.init.registry.ModBlocks;
import committee.nova.mods.avaritia.init.registry.ModRecipeTypes;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.logic.AugmentedRecipeMatcher;
import io.github.lounode.ae2crafting.integration.avaritia.AvaritiaWorkstations;

/**
 * 无尽重生（Re-Avaritia）的编码集成：表格工作台的 tier 由标志物决定，无尽锻造走十字五格。
 *
 * <p>配方都从真实配方表里动态挑，不写死 id——整合包换数据包后测试仍然成立。</p>
 *
 * <p>启动方式：{@code ./gradlew runGameTestServer}（开发运行期已带 Re-Avaritia）。</p>
 */
@GameTestHolder(AE2CraftingIntegration.MOD_ID)
@PrefixGameTestTemplate(false)
public class AvaritiaMatchingGameTest {

    private static final int GRID = 9;
    /** 故意不放在左上角：包围盒裁剪与偏移摆放要一起验。 */
    private static final int ORIGIN_X = 1;
    private static final int ORIGIN_Y = 1;

    /**
     * 同一张网格、同一份原料，换标志物就从「认得出」变成「认不出」。
     *
     * <p>挑一条显式 tier=4 且包围盒 &gt;3×3 的有序表格配方：终极台（11×11, tier 4）必须命中，
     * 幽匿台（5×5, tier 1）必须拒绝。这条同时证明「tier 不是从网格猜的，而是标志物给的」。</p>
     */
    @GameTest(template = "empty")
    public void tableRecipeRequiresTheMatchingWorkstationTier(GameTestHelper helper) {
        var level = helper.getLevel();
        var candidate = findTieredWideRecipe(helper, level, 4);
        if (candidate == null) {
            return;
        }

        var grid = emptyGrid();
        if (!placePattern(candidate.value(), grid)) {
            helper.assertTrue(false, "配方 " + candidate.id() + " 有取不到物品的原料，无法铺进网格");
            return;
        }

        var extreme = AugmentedRecipeMatcher.match(level, grid, GRID, GRID,
                new ItemStack(ModBlocks.extreme_crafting_table.get()));
        helper.assertTrue(extreme != null, "终极台没有认出 tier=4 的表格配方 " + candidate.id());
        helper.assertTrue(extreme.recipeId() != null && extreme.recipeId().equals(candidate.id()),
                "命中的是另一条配方：" + extreme.recipeId() + "，期望 " + candidate.id());
        var expected = candidate.value().getResultItem(level.registryAccess());
        helper.assertTrue(extreme.output().is(expected.getItem()),
                "产物不符：" + extreme.output() + "，期望 " + expected);

        var sculk = AugmentedRecipeMatcher.match(level, grid, GRID, GRID,
                new ItemStack(ModBlocks.sculk_crafting_table.get()));
        helper.assertTrue(sculk == null,
                "5x5 的幽匿台不该做出 tier=4 的配方 " + candidate.id() + "，却命中了 " + sculk);

        helper.succeed();
    }

    /** 没有标志物时，无尽判定器不猜档位——原版判定器仍可处理 3×3，但 9×9 的表格配方应当无产物。 */
    @GameTest(template = "empty")
    public void tableRecipeWithoutAStationYieldsNothing(GameTestHelper helper) {
        var level = helper.getLevel();
        var candidate = findTieredWideRecipe(helper, level, 4);
        if (candidate == null) {
            return;
        }

        var grid = emptyGrid();
        if (!placePattern(candidate.value(), grid)) {
            return;
        }

        var match = AugmentedRecipeMatcher.match(level, grid, GRID, GRID);
        helper.assertTrue(match == null,
                "没放标志物却匹配出了 " + match + "，tier 不该被猜出来");
        helper.succeed();
    }

    /**
     * 无尽锻造：五格十字（模板在左、基底居中、添加物在右/上/下）。
     *
     * <p>先按模组自己的 GUI 布局摆一遍，锻造台必须命中；再把标志物换成终极合成台，
     * 同一条链必须拒绝——锻造台的配方不该在表格台上被认出来。</p>
     */
    @GameTest(template = "empty")
    public void extremeSmithingMatchesTheCrossLayout(GameTestHelper helper) {
        var level = helper.getLevel();
        var holder = findSmithingRecipe(level);
        if (holder == null) {
            helper.assertTrue(false, "数据包里没有 avaritia:extreme_smithing 配方，测试前提不成立");
            return;
        }

        var recipe = holder.value();
        var template = firstItem(recipe.template);
        var base = firstItem(recipe.base);
        var addition = firstItem(recipe.additions);
        helper.assertTrue(!template.isEmpty() && !base.isEmpty() && !addition.isEmpty(),
                "配方 " + holder.id() + " 有取不到物品的试剂");

        var grid = emptyGrid();
        grid.set(4 * GRID + 3, template);
        grid.set(4 * GRID + 4, base);
        grid.set(4 * GRID + 5, addition);
        grid.set(3 * GRID + 4, addition.copy());
        grid.set(5 * GRID + 4, addition.copy());

        var match = AugmentedRecipeMatcher.match(level, grid, GRID, GRID,
                new ItemStack(ModBlocks.extreme_smithing_table.get()));
        helper.assertTrue(match != null, "无尽锻造台没有认出十字布局的配方 " + holder.id());
        helper.assertTrue(match.recipeId() != null && match.recipeId().equals(holder.id()),
                "命中的是另一条配方：" + match.recipeId() + "，期望 " + holder.id());

        var wrongStation = AugmentedRecipeMatcher.match(level, grid, GRID, GRID,
                new ItemStack(ModBlocks.extreme_crafting_table.get()));
        helper.assertTrue(wrongStation == null,
                "终极合成台不该做出锻造配方 " + holder.id() + "，却命中了 " + wrongStation);

        helper.succeed();
    }

    /** 十字四角有东西时不是锻造：拒绝，而不是硬凑。 */
    @GameTest(template = "empty")
    public void cornerItemsBreakTheSmithingLayout(GameTestHelper helper) {
        var level = helper.getLevel();
        var holder = findSmithingRecipe(level);
        if (holder == null) {
            return;
        }

        var recipe = holder.value();
        var grid = emptyGrid();
        grid.set(4 * GRID + 3, firstItem(recipe.template));
        grid.set(4 * GRID + 4, firstItem(recipe.base));
        grid.set(4 * GRID + 5, firstItem(recipe.additions));
        grid.set(3 * GRID + 4, firstItem(recipe.additions));
        grid.set(5 * GRID + 4, firstItem(recipe.additions));
        grid.set(3 * GRID + 3, firstItem(recipe.base)); // 左上角多放一件

        var match = AugmentedRecipeMatcher.match(level, grid, GRID, GRID,
                new ItemStack(ModBlocks.extreme_smithing_table.get()));
        helper.assertTrue(match == null, "四角有杂物时不该识别成锻造，却命中了 " + match);
        helper.succeed();
    }

    /**
     * 四张表格台与档位的对应关系：5/7/9/11 边长 → tier 1..4。
     *
     * <p>单独锁一条，是因为这套映射是本轮所有行为的前提：档位算错，
     * {@code tableRecipeRequiresTheMatchingWorkstationTier} 会以「找不到 tier=4 的配方」的形式
     * 失败，看起来像数据包的问题。这里直接断言，改 {@code ModBlocks} 字段或档位时立刻红。</p>
     */
    @GameTest(template = "empty")
    public void fourTablesMapToFourTiers(GameTestHelper helper) {
        helper.assertTrue(new AvaritiaWorkstations.Table(5, 1).equals(
                AvaritiaWorkstations.tableOf(new ItemStack(ModBlocks.sculk_crafting_table.get()))),
                "幽匿合成台不是 5x5 / tier 1");
        helper.assertTrue(new AvaritiaWorkstations.Table(7, 2).equals(
                AvaritiaWorkstations.tableOf(new ItemStack(ModBlocks.nether_crafting_table.get()))),
                "下界合成台不是 7x7 / tier 2");
        helper.assertTrue(new AvaritiaWorkstations.Table(9, 3).equals(
                AvaritiaWorkstations.tableOf(new ItemStack(ModBlocks.end_crafting_table.get()))),
                "末地合成台不是 9x9 / tier 3");
        helper.assertTrue(new AvaritiaWorkstations.Table(11, 4).equals(
                AvaritiaWorkstations.tableOf(new ItemStack(ModBlocks.extreme_crafting_table.get()))),
                "终极合成台不是 11x11 / tier 4");
        helper.assertTrue(AvaritiaWorkstations.isExtremeSmithingTable(
                new ItemStack(ModBlocks.extreme_smithing_table.get())),
                "无尽锻造台没有被认出来");
        helper.assertTrue(AvaritiaWorkstations.tableOf(new ItemStack(ModBlocks.extreme_smithing_table.get())) == null,
                "无尽锻造台不该被当成表格工作台");
        helper.succeed();
    }

    private static RecipeHolder<ShapedTableCraftingRecipe> findTieredWideRecipe(GameTestHelper helper,
            ServerLevel level, int tier) {
        List<RecipeHolder<ShapedTableCraftingRecipe>> candidates = new ArrayList<>();
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.CRAFTING_TABLE_RECIPE.get())) {
            if (holder.value() instanceof ShapedTableCraftingRecipe shaped
                    && shaped.tier == tier
                    && (shaped.getWidth() > 3 || shaped.getHeight() > 3)
                    && shaped.getWidth() <= GRID && shaped.getHeight() <= GRID
                    && !shaped.getResultItem(level.registryAccess()).isEmpty()) {
                candidates.add(new RecipeHolder<>(holder.id(), shaped));
            }
        }
        helper.assertTrue(!candidates.isEmpty(),
                "无尽重生里没有找到 tier=" + tier + " 且 >3x3 的有序表格配方，测试前提不成立");
        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    private static RecipeHolder<ExtremeSmithingRecipe> findSmithingRecipe(ServerLevel level) {
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.EXTREME_SMITHING_RECIPE.get())) {
            if (!holder.value().getResultItem(level.registryAccess()).isEmpty()) {
                return holder;
            }
        }
        return null;
    }

    private static ItemStack firstItem(Ingredient ingredient) {
        var items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0].copy();
    }

    /** 按 pattern 把原料铺到偏移位置；有原料取不到物品时返回 false。 */
    private static boolean placePattern(ShapedTableCraftingRecipe recipe, ArrayList<ItemStack> grid) {
        var ingredients = recipe.getIngredients();
        int originX = Math.min(ORIGIN_X, GRID - recipe.getWidth());
        int originY = Math.min(ORIGIN_Y, GRID - recipe.getHeight());
        for (int y = 0; y < recipe.getHeight(); y++) {
            for (int x = 0; x < recipe.getWidth(); x++) {
                var ingredient = ingredients.get(y * recipe.getWidth() + x);
                if (ingredient.isEmpty()) {
                    continue;
                }
                var options = ingredient.getItems();
                if (options.length == 0) {
                    return false;
                }
                grid.set((originY + y) * GRID + originX + x, options[0].copy());
            }
        }
        return true;
    }

    private static ArrayList<ItemStack> emptyGrid() {
        var grid = new ArrayList<ItemStack>(GRID * GRID);
        for (int i = 0; i < GRID * GRID; i++) {
            grid.add(ItemStack.EMPTY);
        }
        return grid;
    }
}
