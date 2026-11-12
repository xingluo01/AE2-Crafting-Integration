package io.github.lounode.ae2crafting.common.logic;

import java.util.Arrays;

/**
 * 「外部配方格 → 本模组 9×9 编码台」的格位换算。
 *
 * <p>配方查看器（JEI）按<b>行优先</b>顺序给出一个类别的输入槽：先从左到右，再从上到下。
 * 3×3 的原版合成是 9 个，合成拓展的终极台是 81 个，无尽重生的类别则比台面小一圈
 * （5×5 的幽匿台在 JEI 里只画 3×3）。这些顺序本身不带坐标，所以要落到 81 个编码格上，
 * 得先知道每组输入是几行几列、居中之后第一格落在哪。</p>
 *
 * <p>换算结果是一个长度 81 的 {@code int[]}：下标是编码台的格号（同样行优先），
 * 值是「该格取第几个输入槽」，{@link #EMPTY} 表示这一格留空。方向只有一个——
 * 输入槽序号 → 编码格号，反过来查会丢信息（同一个输入槽不能落两格）。</p>
 *
 * <p><b>为什么居中而不是左上对齐</b>：编码器写样板前会把非空格裁成包围盒再居中
 * （{@link GridBounds#centerIn}），匹配器也一律按包围盒找配方。所以居中只是让玩家
 * 在界面上看到的位置和最终样板一致，不影响匹配结果。但对合成拓展与无尽重生这两家来说，
 * 配方查看器自己就已经把图案居中了（它们用 {@code (网格 - 图案) / 2} 算偏移），
 * 这里再居中一次，落到 81 格上的位置和它们在 JEI 里看到的完全一样。</p>
 */
public final class GridPlacement {

    /** 映射表里表示「这一格没有输入」。 */
    public static final int EMPTY = -1;

    private GridPlacement() {
    }

    /**
     * 把 {@code side × side} 的方形输入槽居中铺进 {@code gridWidth × gridHeight}。
     *
     * <p>输入槽必须按行优先编号（0 是左上角）。放不下时抛 {@link IllegalArgumentException}，
     * 由调用方先判尺寸再决定报「配方太大」还是换布局——这里不做静默裁剪，
     * 裁掉的格子等于吞掉玩家的材料。</p>
     *
     * @param side       配方网格边长（1..9 才有意义）
     * @param gridWidth  编码台宽度
     * @param gridHeight 编码台高度
     * @return 长度 {@code gridWidth * gridHeight} 的映射表
     */
    public static int[] squareCentered(int side, int gridWidth, int gridHeight) {
        if (side <= 0) {
            throw new IllegalArgumentException("配方边长必须为正：" + side);
        }
        if (side > gridWidth || side > gridHeight) {
            throw new IllegalArgumentException(
                    "配方 " + side + "x" + side + " 放不进 " + gridWidth + "x" + gridHeight + " 的编码台");
        }

        int offsetX = (gridWidth - side) / 2;
        int offsetY = (gridHeight - side) / 2;
        int[] map = emptyMap(gridWidth, gridHeight);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                map[(offsetY + y) * gridWidth + (offsetX + x)] = y * side + x;
            }
        }
        return map;
    }

    /**
     * 十字五格，居中铺进编码台。
     *
     * <p>顺序与无尽重生的 JEI 类别一致：<b>模板（左）、基底（中）、上方添加物（上）、
     * 右侧添加物（右）、下方添加物（下）</b>。这个顺序同时是
     * {@code AvaritiaSmithingMatcher} 的读取顺序，两边必须一起改。</p>
     */
    public static int[] crossCentered(int gridWidth, int gridHeight) {
        if (gridWidth < 3 || gridHeight < 3) {
            throw new IllegalArgumentException(
                    "十字五格至少要 3x3 的台面：" + gridWidth + "x" + gridHeight);
        }

        int centerX = gridWidth / 2;
        int centerY = gridHeight / 2;
        int[] map = emptyMap(gridWidth, gridHeight);
        map[centerY * gridWidth + centerX - 1] = 0; // 模板
        map[centerY * gridWidth + centerX] = 1;     // 基底
        map[(centerY - 1) * gridWidth + centerX] = 2; // 添加物·上
        map[centerY * gridWidth + centerX + 1] = 3; // 添加物·右
        map[(centerY + 1) * gridWidth + centerX] = 4; // 添加物·下
        return map;
    }

    /**
     * 组合合成器的「中心 + 顺时针螺旋」布局：输入 0 落中心 (4,4)，其余按
     * {@link SpiralGrid} 的顺序由内向外铺开。
     *
     * <p>配方要的是一张线性表（中心物品 + 每个基座一个无序输入），JEI 类别也只画
     * 中心和一个聚合槽；转移时必须按这个固定坐标系还原，不能居中压缩。</p>
     */
    public static int[] spiralCentered(int gridWidth, int gridHeight) {
        if (gridWidth != SpiralGrid.SIZE || gridHeight != SpiralGrid.SIZE) {
            throw new IllegalArgumentException(
                    "螺旋布局只支持 9x9 编码台：" + gridWidth + "x" + gridHeight);
        }
        int[] map = new int[gridWidth * gridHeight];
        for (int order = 0; order < map.length; order++) {
            map[SpiralGrid.indexAt(order)] = order;
        }
        return map;
    }

    /**
     * 行优先顺位布局：输入 0 落左上角，依次填满整张网格。
     *
     * <p>终焉厨锅的 81 格输入就是这个顺序，容器按约定接在最后一个原料之后。</p>
     */
    public static int[] sequential(int gridWidth, int gridHeight) {
        int[] map = new int[gridWidth * gridHeight];
        for (int i = 0; i < map.length; i++) {
            map[i] = i;
        }
        return map;
    }

    /** 映射表里有几个非空格子。用于断言「输入槽数与类别尺寸是否自洽」。 */
    public static int placedCount(int[] map) {
        int count = 0;
        for (int slot : map) {
            if (slot != EMPTY) {
                count++;
            }
        }
        return count;
    }

    private static int[] emptyMap(int gridWidth, int gridHeight) {
        int[] map = new int[gridWidth * gridHeight];
        Arrays.fill(map, EMPTY);
        return map;
    }
}
