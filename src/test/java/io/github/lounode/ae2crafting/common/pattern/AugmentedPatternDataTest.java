package io.github.lounode.ae2crafting.common.pattern;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 增广样板的数据校验。
 *
 * <p>只覆盖能在纯 JUnit 里跑的路径：构造一个完整实例需要 {@code GenericStack}，
 * 而它依赖已 bootstrap 的物品注册表。校验失败的用例在触碰 {@code GenericStack} 之前就会抛出，
 * 因此不需要启动游戏。</p>
 */
class AugmentedPatternDataTest {

    @Test
    void rejectsDimensionOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new AugmentedPatternData(0, 9, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new AugmentedPatternData(10, 9, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new AugmentedPatternData(9, 10, List.of(), List.of()));
    }

    @Test
    void rejectsGridThatDoesNotMatchDimensions() {
        // 9×9 需要 81 格；给 3 格必须被拒，否则解码时 at() 会数组越界。
        assertThrows(IllegalArgumentException.class,
                () -> new AugmentedPatternData(9, 9, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new AugmentedPatternData(1, 1, List.of(), List.of()));
    }
}
