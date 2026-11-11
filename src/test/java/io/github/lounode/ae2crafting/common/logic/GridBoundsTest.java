package io.github.lounode.ae2crafting.common.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 包围盒裁法。原版 {@code ShapedRecipePattern#matches} 要求输入宽高与配方宽高**完全相等**，
 * 所以「裁得对不对」直接决定 3×3 配方能不能在 9×9 网格的任意位置被认出来。
 *
 * <p>用字符串当格子内容：纯 JUnit 里 {@code ItemStack} 连静态初始化都会失败（见
 * {@code GridBounds} 类注释），而裁法与元素类型无关。</p>
 */
class GridBoundsTest {

    private static final int GRID = 9;
    /** 空格用 {@code null} 表示，与 {@code ConfigInventory#getStack} 的返回值一致。 */
    private static final String EMPTY = null;
    private static final String FILLED = "x";

    @Test
    void emptyGridHasNoBounds() {
        // 全 null 时 of 根本不会调用 isEmpty，等价于全空。
        assertEquals(GridBounds.EMPTY, GridBounds.of(emptyGrid(), GRID, GRID, String::isEmpty));
    }

    @Test
    void nullAndEmptyBothCountAsEmpty() {
        var grid = emptyGrid();
        grid.set(0, "");
        grid.set(4 * GRID + 4, FILLED);

        var bounds = GridBounds.of(grid, GRID, GRID, String::isEmpty);

        assertEquals(new GridBounds(1, 1, 4, 4), bounds);
    }

    @Test
    void singleItemIsOneByOneAtItsOwnCell() {
        var grid = emptyGrid();
        grid.set(4 * GRID + 5, FILLED);

        var bounds = GridBounds.of(grid, GRID, GRID, String::isEmpty);

        assertEquals(new GridBounds(1, 1, 5, 4), bounds);
        assertEquals(List.of(FILLED), bounds.crop(grid, GRID));
    }

    @Test
    void boundsHugTheOutermostItems() {
        var grid = emptyGrid();
        // 左上 (2,1)、右下 (4,3)：包围盒 3×3，左上角对齐 (2,1)。
        grid.set(1 * GRID + 2, FILLED);
        grid.set(3 * GRID + 4, FILLED);

        var bounds = GridBounds.of(grid, GRID, GRID, String::isEmpty);

        assertEquals(new GridBounds(3, 3, 2, 1), bounds);
        assertEquals(4, bounds.right());
        assertEquals(3, bounds.bottom());
    }

    @Test
    void cropKeepsRowMajorLayoutAndDropsTheRest() {
        var grid = emptyGrid();
        grid.set(1 * GRID + 2, "stone");
        grid.set(2 * GRID + 3, "dirt");

        var cropped = GridBounds.of(grid, GRID, GRID, String::isEmpty).crop(grid, GRID);

        // 2×2：左上石头、右下泥土，另外两格是空格——位置关系不能被打乱。
        assertEquals(4, cropped.size());
        assertEquals("stone", cropped.get(0));
        assertEquals(EMPTY, cropped.get(1));
        assertEquals(EMPTY, cropped.get(2));
        assertEquals("dirt", cropped.get(3));
    }

    @Test
    void emptyBoundsCropToNothing() {
        assertEquals(List.of(), GridBounds.EMPTY.crop(emptyGrid(), GRID));
    }

    @Test
    void rejectsGridThatIsTooShort() {
        // 少给格子会让 crop 越界，宁可当场炸也不要静默算错包围盒。
        assertThrows(IllegalArgumentException.class,
                () -> GridBounds.of(List.of(), GRID, GRID, String::isEmpty));
    }

    /**
     * 居中填充：单件物品在 9×9 的左上角，铺进同样 9×9 的目标时应当落到正中 (4,4)。
     * 这就是「玩家把图案摆在编码台哪个角落都不影响结果」的换算规则。
     */
    @Test
    void centerInMovesTheBoundsToTheTargetCentre() {
        var grid = emptyGrid();
        grid.set(0, FILLED);

        var centered = GridBounds.of(grid, GRID, GRID, String::isEmpty)
                .centerIn(grid, GRID, GRID, GRID, "");

        assertEquals(GRID * GRID, centered.size());
        assertEquals(FILLED, centered.get(4 * GRID + 4));
        assertEquals("", centered.get(0));
    }

    /**
     * 3×3 图案放进 11×11 的工作台：目标比源大时四周补空、内容居中。
     * 无尽重生的终极台是 11×11，编码台固定 9×9，这条换算决定高级配方落在哪。
     */
    @Test
    void centerInPadsAroundASmallerPatternOnABiggerTable() {
        var grid = emptyGrid();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                grid.set(y * GRID + x, FILLED);
            }
        }

        var centered = GridBounds.of(grid, GRID, GRID, String::isEmpty)
                .centerIn(grid, GRID, 11, 11, "");

        assertEquals(121, centered.size());
        assertEquals(FILLED, centered.get(4 * 11 + 4), "左上角应当是空出来的边");
        assertEquals(FILLED, centered.get(6 * 11 + 6));
        assertEquals("", centered.get(0));
        assertEquals("", centered.get(10 * 11 + 10));
    }

    /** 装不进这张台时直接拒绝，而不是悄悄截掉原料。 */
    @Test
    void centerInRejectsABoundsThatDoesNotFit() {
        var grid = emptyGrid();
        grid.set(0, FILLED);
        grid.set(4 * GRID + 4, FILLED);

        var bounds = GridBounds.of(grid, GRID, GRID, String::isEmpty);

        assertThrows(IllegalArgumentException.class, () -> bounds.centerIn(grid, GRID, 3, 3, ""));
    }

    /** 空网格居中后应当整张都是给定的空格值，而不是 null 混进去。 */
    @Test
    void centerInFillsEmptyCellsWithTheGivenValue() {
        var centered = GridBounds.EMPTY.centerIn(emptyGrid(), GRID, 5, 5, "");

        assertEquals(25, centered.size());
        assertEquals("", centered.get(12));
    }

    private static ArrayList<String> emptyGrid() {
        return new ArrayList<>(Collections.nCopies(GRID * GRID, (String) null));
    }
}
