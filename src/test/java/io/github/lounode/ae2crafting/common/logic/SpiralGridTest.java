package io.github.lounode.ae2crafting.common.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

/**
 * 组合合成器把「中心 + 螺旋」当坐标系用，坐标表一旦重复或漏格，配方输入就会错位。
 * 这里全是纯算术，不需要游戏注册表。
 */
class SpiralGridTest {

    @Test
    void coversEveryCellExactlyOnceStartingAtCenter() {
        var indices = SpiralGrid.indices();
        assertEquals(81, indices.length, "9x9 必须正好 81 个坐标");
        assertEquals(SpiralGrid.CENTER_Y * SpiralGrid.SIZE + SpiralGrid.CENTER_X, indices[0],
                "第一个格子必须是中心 (4,4)");

        var seen = new HashSet<Integer>();
        for (int index : indices) {
            assertTrue(index >= 0 && index < 81, "坐标越界：" + index);
            assertTrue(seen.add(index), "坐标重复：" + index);
        }
        assertEquals(81, seen.size());
    }

    @Test
    void flattenAndScatterAreInverse() {
        var grid = new ArrayList<String>(SpiralGrid.SLOTS);
        for (int i = 0; i < SpiralGrid.SLOTS; i++) {
            grid.add("c" + i);
        }
        var flat = SpiralGrid.flatten(grid);
        assertEquals("c" + (SpiralGrid.CENTER_Y * SpiralGrid.SIZE + SpiralGrid.CENTER_X), flat.get(0));
        assertEquals(grid, SpiralGrid.scatter(flat, ""));
    }

    @Test
    void scatterFillsUnusedPositionsWithEmpty() {
        var flat = new ArrayList<String>();
        flat.add("center");
        var back = SpiralGrid.scatter(flat, ".");
        assertEquals(SpiralGrid.SLOTS, back.size());
        assertEquals("center", back.get(SpiralGrid.CENTER_Y * SpiralGrid.SIZE + SpiralGrid.CENTER_X));
        assertEquals(SpiralGrid.SLOTS - 1, back.stream().filter("."::equals).count());
    }
}
