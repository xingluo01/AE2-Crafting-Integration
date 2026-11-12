package io.github.lounode.ae2crafting.common.logic;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * 9×9 网格的「中心 + 顺时针螺旋」展开顺序。
 *
 * <p>组合合成器（Crafting Core）的配方输入不是二维图案，而是一张线性表：下标 0 是中心台的
 * 主物品，之后每个基座提供一个无序输入。要把它搬进本模组的 9×9 编码台，就必须给这张线性表
 * 定一个固定的落位规则——中心格 (4,4) 放主物品，其余按顺时针由内向外一圈圈铺开。</p>
 *
 * <p>螺旋只覆盖 9×9 内的格子，第一个元素是中心，之后不会重复、不会越界；81 个坐标正好铺满
 * 整张网格，对应「基础 1 槽 + 最多 80 个基座」。</p>
 */
public final class SpiralGrid {

    public static final int SIZE = 9;
    public static final int SLOTS = SIZE * SIZE;
    public static final int CENTER_X = SIZE / 2;
    public static final int CENTER_Y = SIZE / 2;

    /** 右 → 下 → 左 → 上：顺时针。必须在 {@link #INDICES} 之前初始化，build() 要用它。 */
    private static final int[][] DIRECTIONS = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };

    /** 行优先下标，按螺旋访问顺序排列。 */
    private static final int[] INDICES = build();

    private SpiralGrid() {
    }

    private static int[] build() {
        var result = new int[SLOTS];
        int count = 0;
        int x = CENTER_X;
        int y = CENTER_Y;
        result[count++] = y * SIZE + x;

        int direction = 0;
        for (int step = 1; count < SLOTS; step++) {
            for (int repeat = 0; repeat < 2 && count < SLOTS; repeat++) {
                for (int i = 0; i < step && count < SLOTS; i++) {
                    x += DIRECTIONS[direction][0];
                    y += DIRECTIONS[direction][1];
                    if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
                        result[count++] = y * SIZE + x;
                    }
                }
                direction = (direction + 1) % DIRECTIONS.length;
            }
        }
        return result;
    }

    /** 螺旋第 {@code order} 个格子在行优先网格里的下标。 */
    public static int indexAt(int order) {
        return INDICES[order];
    }

    /** 完整的螺旋下标表（副本，调用方改不动内部常量）。 */
    public static int[] indices() {
        return INDICES.clone();
    }

    /**
     * 把 9×9 网格按螺旋顺序压成线性表，空格用 {@code null} 占位、保留位置。
     *
     * <p>组合合成配方要的是「紧凑的非空输入表」，所以判定器会再滤掉空格；但余料回填需要
     * 原来的位置，所以这里不压缩，坐标由 {@link #scatter} 还原。</p>
     */
    public static <T> List<@Nullable T> flatten(List<@Nullable T> grid) {
        var result = new ArrayList<@Nullable T>(SLOTS);
        for (var index : INDICES) {
            result.add(grid.get(index));
        }
        return result;
    }

    /** {@link #flatten} 的逆运算：把螺旋顺序的表写回行优先的 9×9 网格。 */
    public static <T> List<T> scatter(List<@Nullable T> values, T empty) {
        var result = new ArrayList<T>(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            result.add(empty);
        }
        for (int i = 0; i < Math.min(SLOTS, values.size()); i++) {
            var value = values.get(i);
            result.set(INDICES[i], value == null ? empty : value);
        }
        return result;
    }
}
