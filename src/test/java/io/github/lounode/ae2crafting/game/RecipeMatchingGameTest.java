package io.github.lounode.ae2crafting.game;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import appeng.api.stacks.GenericStack;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.logic.AugmentedRecipeMatcher;

/**
 * 9×9 网格的配方匹配，跑在真实服务器世界上（真 {@code RecipeManager}、真配方表）。
 *
 * <p>纯 JUnit 里没有世界也没有配方，{@code ShapedRecipePattern#matches} 要求输入宽高与配方宽高
 * <b>完全相等</b>这件事只有在真实配方上才验得出来——它正是「9×9 全图直接喂给原版匹配必然失败、
 * 必须先裁包围盒」这条设计的根据。</p>
 *
 * <p>启动方式：{@code ./gradlew runGameTestServer}。</p>
 */
@GameTestHolder(AE2CraftingIntegration.MOD_ID)
@PrefixGameTestTemplate(false)
public class RecipeMatchingGameTest {

    private static final int GRID = 9;

    /**
     * 2×2 木板放在 9×9 网格的 (3,2)，必须认成工作台。
     *
     * <p>这条同时锁住两件事：包围盒裁得对（第 3 行第 2 列起、2×2），以及「偏移摆放」不影响匹配。
     * 如果把整张 9×9 直接喂给 {@code getRecipeFor}，原版配方一次也不会命中。</p>
     */
    @GameTest(template = "empty")
    public void vanillaRecipeMatchesAnywhereInTheGrid(GameTestHelper helper) {
        var grid = emptyGrid();
        for (int y = 2; y < 4; y++) {
            for (int x = 3; x < 5; x++) {
                grid.set(y * GRID + x, new ItemStack(Items.OAK_PLANKS));
            }
        }

        var match = AugmentedRecipeMatcher.match(helper.getLevel(), grid, GRID, GRID);

        helper.assertTrue(match != null, "9x9 网格里偏移摆放的 2x2 木板配方没有被认出来");
        helper.assertTrue(match.output().is(Items.CRAFTING_TABLE), "产物不是工作台：" + match.output());
        helper.assertTrue(match.recipeId() != null, "命中结果缺少配方 id，装配室以后没法追溯来源");
        helper.succeed();
    }

    /** 无序配方同样要过：单格橡木原木 → 4 个橡木木板（1×1 包围盒）。 */
    @GameTest(template = "empty")
    public void shapelessRecipeWithSingleSlotMatches(GameTestHelper helper) {
        var grid = emptyGrid();
        grid.set(4 * GRID + 4, new ItemStack(Items.OAK_LOG));

        var match = AugmentedRecipeMatcher.match(helper.getLevel(), grid, GRID, GRID);

        helper.assertTrue(match != null, "单格原木没有匹配到木板配方");
        helper.assertTrue(match.output().is(Items.OAK_PLANKS), "产物不是木板：" + match.output());
        helper.assertTrue(match.output().getCount() == 4, "木板数量应为 4，实际 " + match.output().getCount());
        helper.succeed();
    }

    /**
     * 包围盒超过 3×3 时原版判定器必须让位（返回 null），而不是硬拿 9×9 去撞配方。
     * 9×9 工作台的配方由 {@code integration/*} 的判定器接手，这里先锁住「原版不越权」。
     */
    @GameTest(template = "empty")
    public void gridsWiderThanThreeAreLeftToNeighbourMatchers(GameTestHelper helper) {
        var grid = emptyGrid();
        grid.set(0, new ItemStack(Items.STONE));
        grid.set(8 * GRID + 8, new ItemStack(Items.STONE));

        var match = AugmentedRecipeMatcher.match(helper.getLevel(), grid, GRID, GRID);

        helper.assertTrue(match == null, "包围盒 9x9 却被原版合成台认了，产物 " + (match == null ? "空" : match.output()));
        helper.succeed();
    }

    /**
     * 编码台的 {@code ConfigInventory} 快照 → 配方匹配用的物品表。
     *
     * <p>空格在库存里是 {@code null}、数量可能为 0、也可能是流体键，这三类都必须变成
     * {@link ItemStack#EMPTY}，否则 {@code CraftingInput} 的 ingredientCount 会算错、
     * 原版判定器拿到的「空格」其实是一个数量 0 的幽灵物品。</p>
     */
    @GameTest(template = "empty")
    public void gridSnapshotKeepsAmountsAndBlanksOutNonItems(GameTestHelper helper) {
        var converted = AugmentedRecipeMatcher.toItemStacks(Arrays.asList(
                GenericStack.fromItemStack(new ItemStack(Items.STONE, 3)),
                null));

        helper.assertTrue(converted.size() == 2, "快照长度变了：" + converted.size());
        helper.assertTrue(converted.get(0).is(Items.STONE), "第一格不是石头：" + converted.get(0));
        helper.assertTrue(converted.get(0).getCount() == 3, "数量没保留：" + converted.get(0).getCount());
        helper.assertTrue(converted.get(1).isEmpty(), "空格没有变成 EMPTY：" + converted.get(1));
        helper.succeed();
    }

    private static ArrayList<ItemStack> emptyGrid() {
        var grid = new ArrayList<ItemStack>(GRID * GRID);
        for (int i = 0; i < GRID * GRID; i++) {
            grid.add(ItemStack.EMPTY);
        }
        return grid;
    }
}
