package io.github.lounode.ae2crafting.common.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * tooltip 里的坐标换算：行优先下标 → 1-based (列, 行)。
 *
 * <p>刻意只测纯算术，不碰 {@code ItemStack} / 注册表（纯 JUnit 环境里
 * {@code ItemStack} 的类初始化会直接抛 {@code ExceptionInInitializerError}）。</p>
 */
class AugmentedPatternTooltipTest {

    @Test
    void indexZeroIsTopLeft() {
        var pos = AugmentedPattern.positionOf(0, 9);
        assertEquals(1, pos.column());
        assertEquals(1, pos.row());
    }

    @Test
    void indexWrapsAtRowEnd() {
        assertEquals(9, AugmentedPattern.positionOf(8, 9).column());
        assertEquals(1, AugmentedPattern.positionOf(8, 9).row());
        assertEquals(1, AugmentedPattern.positionOf(9, 9).column());
        assertEquals(2, AugmentedPattern.positionOf(9, 9).row());
    }

    @Test
    void arbitraryIndexMatchesColumnRow() {
        var pos = AugmentedPattern.positionOf(2 * 9 + 3, 9);
        assertEquals(4, pos.column());
        assertEquals(3, pos.row());
    }

    @Test
    void widthIsHonoured() {
        var pos = AugmentedPattern.positionOf(7, 5);
        assertEquals(3, pos.column());
        assertEquals(2, pos.row());
    }
}
