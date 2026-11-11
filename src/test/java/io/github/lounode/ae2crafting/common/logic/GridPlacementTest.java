package io.github.lounode.ae2crafting.common.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * 配方格 → 编码台的格位换算。
 *
 * <p>这张映射表是「JEI 里看到的第 N 个输入槽」到「编码台第几格」的唯一依据，
 * 错一位就是配方整体平移一格。所以不测「有没有返回数组」，只测具体落点。</p>
 */
class GridPlacementTest {

    private static final int GRID = 9;

    @Test
    void squareCenteredPutsA3x3PatternInTheMiddle() {
        int[] map = GridPlacement.squareCentered(3, GRID, GRID);

        assertEquals(GRID * GRID, map.length);
        assertEquals(9, GridPlacement.placedCount(map));
        // 9-3 = 6，左右上下各留 3 格。
        assertEquals(0, map[3 * GRID + 3]);
        assertEquals(4, map[4 * GRID + 4]);
        assertEquals(8, map[5 * GRID + 5]);
        // 留下的边角必须是空。
        assertEquals(GridPlacement.EMPTY, map[0]);
        assertEquals(GridPlacement.EMPTY, map[2 * GRID + 4]);
        assertEquals(GridPlacement.EMPTY, map[8 * GRID + 8]);
    }

    @Test
    void squareCenteredIsFlushWithAnOddOffset() {
        // (9-7)/2 = 1：7×7 的左上角落在 (1,1)，不是 (0,0) 也不是 (2,2)。
        int[] map = GridPlacement.squareCentered(7, GRID, GRID);

        assertEquals(49, GridPlacement.placedCount(map));
        assertEquals(0, map[GRID + 1]);
        assertEquals(48, map[7 * GRID + 7]);
    }

    @Test
    void squareCenteredFillsTheGridWhenSizesMatch() {
        int[] map = GridPlacement.squareCentered(GRID, GRID, GRID);

        assertEquals(GRID * GRID, GridPlacement.placedCount(map));
        for (int i = 0; i < map.length; i++) {
            assertEquals(i, map[i], "第 " + i + " 格应原样对应第 " + i + " 个输入槽");
        }
    }

    @Test
    void squareCenteredRejectsPatternsLargerThanTheGrid() {
        // 无尽重生的终极台是 11×11，但 JEI 类别只画 9×9；真出现 11 就必须报错而不是裁剪。
        assertThrows(IllegalArgumentException.class, () -> GridPlacement.squareCentered(11, GRID, GRID));
    }

    @Test
    void squareCenteredRejectsNonPositiveSide() {
        assertThrows(IllegalArgumentException.class, () -> GridPlacement.squareCentered(0, GRID, GRID));
    }

    @Test
    void crossCenteredPlacesTheFiveSmithingSlots() {
        int[] map = GridPlacement.crossCentered(GRID, GRID);

        assertEquals(5, GridPlacement.placedCount(map));
        assertEquals(0, map[4 * GRID + 3], "模板在中心左侧");
        assertEquals(1, map[4 * GRID + 4], "基底在中心");
        assertEquals(2, map[3 * GRID + 4], "添加物在中心上方");
        assertEquals(3, map[4 * GRID + 5], "添加物在中心右侧");
        assertEquals(4, map[5 * GRID + 4], "添加物在中心下方");
        // 四个角必须留空：无尽锻造的四角放了东西就该被拒绝。
        assertEquals(GridPlacement.EMPTY, map[3 * GRID + 3]);
        assertEquals(GridPlacement.EMPTY, map[5 * GRID + 5]);
    }

    @Test
    void crossCenteredRejectsTooSmallGrid() {
        assertThrows(IllegalArgumentException.class, () -> GridPlacement.crossCentered(2, 2));
    }
}
