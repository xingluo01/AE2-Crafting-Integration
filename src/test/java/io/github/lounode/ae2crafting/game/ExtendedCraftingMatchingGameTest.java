package io.github.lounode.ae2crafting.game;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.blakebr0.extendedcrafting.crafting.recipe.ShapedTableRecipe;
import com.blakebr0.extendedcrafting.init.ModRecipeTypes;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.logic.AugmentedRecipeMatcher;

/**
 * 合成拓展的 9×9 工作台配方在这条匹配链上跑通：**从真实配方表里随机挑一条 &gt;3×3 的有序配方**，
 * 把它的原料按 pattern 原样铺进 9×9 网格的偏移位置，必须还原出同一个产物。
 *
 * <p>为什么不在测试里写死某条配方：配方属于数据包，整合包换一套配方就会让写死的 ID 失效。
 * 动态挑一条反而是更强的断言——它同时覆盖「包围盒裁剪」「tier 等值匹配」「链上原版让位给邻居」
 * 三件事，而且与具体配方内容无关。</p>
 *
 * <p>启动方式：{@code ./gradlew runGameTestServer}（开发运行期已带 Extended Crafting）。</p>
 */
@GameTestHolder(AE2CraftingIntegration.MOD_ID)
@PrefixGameTestTemplate(false)
public class ExtendedCraftingMatchingGameTest {

    private static final int GRID = 9;
    /** 故意不放在左上角：包围盒裁剪与偏移摆放要一起验。 */
    private static final int ORIGIN_X = 1;
    private static final int ORIGIN_Y = 1;

    @GameTest(template = "empty")
    public void tableRecipeRoundTripsThroughTheMatcherChain(GameTestHelper helper) {
        var level = helper.getLevel();
        var candidate = findWideRecipe(helper, level);
        if (candidate == null) {
            return;
        }

        var grid = emptyGrid();
        if (!placePattern(candidate.value(), grid)) {
            helper.assertTrue(false, "配方 " + candidate.id() + " 有取不到物品的原料，无法铺进网格");
            return;
        }

        var match = AugmentedRecipeMatcher.match(level, grid, GRID, GRID);

        helper.assertTrue(match != null, "9x9 工作台配方 " + candidate.id() + " 没有被认出来");
        helper.assertTrue(match.recipeId() != null && match.recipeId().equals(candidate.id()),
                "命中的是另一条配方：" + match.recipeId() + "，期望 " + candidate.id());
        var expected = candidate.value().getResultItem(level.registryAccess());
        helper.assertTrue(match.output().is(expected.getItem()),
                "产物不符：" + match.output() + "，期望 " + expected);
        helper.succeed();
    }

    /** 装了的邻居必须挂上链：原版一条 + 合成拓展一条 + 无尽一条。 */
    @GameTest(template = "empty")
    public void installedTableModsAreOnTheChain(GameTestHelper helper) {
        var chain = AugmentedRecipeMatcher.matchers();
        helper.assertTrue(chain.size() >= 3,
                "匹配链只有 " + chain.size() + " 条判定器，邻居的工作台没有注册进来");
        helper.succeed();
    }

    /**
     * 挑一条「包围盒超过 3×3、产物非空、原料都取得到物品」的有序表格配方。
     * 找不到时让测试失败：开发运行期带的就是合成拓展的默认数据包，缺配方说明环境不对。
     */
    private static RecipeHolder<ShapedTableRecipe> findWideRecipe(GameTestHelper helper, ServerLevel level) {
        List<RecipeHolder<ShapedTableRecipe>> candidates = new ArrayList<>();
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.TABLE.get())) {
            if (holder.value() instanceof ShapedTableRecipe shaped
                    && (shaped.getWidth() > 3 || shaped.getHeight() > 3)
                    && !shaped.getResultItem(level.registryAccess()).isEmpty()) {
                candidates.add(new RecipeHolder<>(holder.id(), shaped));
            }
        }
        helper.assertTrue(!candidates.isEmpty(),
                "合成拓展里没有找到任何 >3x3 的有序表格配方，测试前提不成立");
        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    /** 按 pattern 把原料铺到偏移位置；有原料取不到物品时返回 false。 */
    private static boolean placePattern(ShapedTableRecipe recipe, ArrayList<ItemStack> grid) {
        var ingredients = recipe.getIngredients();
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
                grid.set((ORIGIN_Y + y) * GRID + ORIGIN_X + x, options[0].copy());
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
