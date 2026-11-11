package io.github.lounode.ae2crafting.common.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 判定器链的顺序与短路。
 *
 * <p>这些用例都**不碰真实配方表**：需要真实 {@code RecipeManager} 的路径在
 * {@code game/RecipeMatchingGameTest} 里跑（纯 JUnit 没有世界，也没有配方）。</p>
 *
 * <p>格子用全 {@code null} 表示（{@code ConfigInventory#getStack} 对空格就返回 {@code null}），
 * 产物用 {@code null} 占位——纯 JUnit 里连 {@code ItemStack.EMPTY} 都取不到，
 * {@link ItemStack} 的静态初始化会抛 {@code ExceptionInInitializerError}。</p>
 */
class AugmentedRecipeMatcherTest {

    private static final int GRID = 9;

    /**
     * 空网格时整条默认链必须返回 null，且**不能碰 level**——这里传的就是 {@code null}。
     * 原版判定器若先摸世界再判空，这条会直接 NPE。
     */
    @Test
    void defaultChainBailsOutBeforeTouchingTheLevel() {
        assertNull(AugmentedRecipeMatcher.match(null, emptyGrid(), GRID, GRID));
    }

    @Test
    void firstNonNullMatchWinsAndStopsTheChain() {
        var first = new GridMatcher.Match(null, null);
        var second = new GridMatcher.Match(null, ResourceLocation.parse("ae2_crafting_integration:never"));
        var secondCalled = new AtomicInteger();

        var result = AugmentedRecipeMatcher.matchWithChain(null, emptyGrid(), GRID, GRID, List.of(
                (level, items, width, height) -> first,
                (level, items, width, height) -> {
                    secondCalled.incrementAndGet();
                    return second;
                }));

        assertSame(first, result);
        assertEquals(0, secondCalled.get(), "命中后不应继续问后面的判定器");
    }

    @Test
    void skipsMatchersThatDoNotRecogniseTheGrid() {
        var recognised = new GridMatcher.Match(null, null);

        var result = AugmentedRecipeMatcher.matchWithChain(null, emptyGrid(), GRID, GRID, List.of(
                (level, items, width, height) -> null,
                (level, items, width, height) -> recognised));

        assertSame(recognised, result, "前一个判定器返回 null 时必须继续往下问");
    }

    @Test
    void noMatcherRecognisingTheGridYieldsNull() {
        assertNull(AugmentedRecipeMatcher.matchWithChain(null, emptyGrid(), GRID, GRID, List.of()));
    }

    private static ArrayList<ItemStack> emptyGrid() {
        return new ArrayList<>(Collections.nCopies(GRID * GRID, (ItemStack) null));
    }
}
