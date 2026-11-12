package io.github.lounode.ae2crafting.common.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;

/**
 * 一张网格里所有非空格的**最小外接矩形**。
 *
 * <p>为什么需要它：原版 {@code ShapedRecipePattern#matches} 要求 {@code input.width() == pattern.width}
 * 且尺寸完全相等（不是「放得下就行」），所以直接把 9×9 全图喂给配方匹配必然一次也命中不了。
 * 原版工作台的官方做法就是先裁到包围盒再查配方——{@code TransientCraftingContainer#asPositionedCraftInput}
 * 用 {@code CraftingInput.ofPositioned} 做的就是这件事。这里把同一套裁法抽出来，编码终端与装配室共用。</p>
 *
 * <p>{@code left/top} 是包围盒在整张网格里的偏移，装配室以后按它把配方结果写回原位时要靠这两个值。</p>
 *
 * <p><b>为什么不直接收 {@code List<ItemStack>}</b>：纯 JUnit 里 {@code ItemStack} 的静态初始化就会
 * 抛 {@code ExceptionInInitializerError}（{@code ItemStack.EMPTY} 依赖物品注册表），拿它写单测等于
 * 一条也跑不起来。所以判定「这格空不空」作为参数传进来，{@link #ofItems} 只是用
 * {@code ItemStack::isEmpty} 套了一层；测试用字符串列表就能验裁法。</p>
 *
 * @param width  包围盒宽（格）
 * @param height 包围盒高（格）
 * @param left   包围盒左缘在网格里的列号
 * @param top    包围盒上缘在网格里的行号
 */
public record GridBounds(int width, int height, int left, int top) {

    /** 全空网格的结果，{@link #isEmpty()} 为 {@code true}。 */
    public static final GridBounds EMPTY = new GridBounds(0, 0, 0, 0);

    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }

    public int right() {
        return left + width - 1;
    }

    public int bottom() {
        return top + height - 1;
    }

    /**
     * 算包围盒。{@code items} 必须是行优先、长度不少于 {@code gridWidth * gridHeight}。
     *
     * <p>{@code null} 元素按空格处理：{@code ConfigInventory#getStack} 对空格返回的就是 {@code null}，
     * 上层没转换干净时也不该在这里炸。</p>
     *
     * @throws IllegalArgumentException 长度不够——少算一格会让 {@link #crop} 越界，宁可当场炸
     */
    public static <T> GridBounds of(List<T> items, int gridWidth, int gridHeight, Predicate<T> isEmpty) {
        var expected = gridWidth * gridHeight;
        if (items.size() < expected) {
            throw new IllegalArgumentException(
                    "grid has " + items.size() + " items, expected " + expected + " for " + gridWidth + "x" + gridHeight);
        }

        int minX = gridWidth;
        int minY = gridHeight;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                var item = items.get(y * gridWidth + x);
                if (item != null && !isEmpty.test(item)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return EMPTY;
        }
        return new GridBounds(maxX - minX + 1, maxY - minY + 1, minX, minY);
    }

    /** 物品网格版，判定「空格」的名义是 {@link ItemStack#isEmpty()}。 */
    public static GridBounds ofItems(List<ItemStack> items, int gridWidth, int gridHeight) {
        return of(items, gridWidth, gridHeight, ItemStack::isEmpty);
    }

    /**
     * 把包围盒从整张网格里裁出来，行优先。包围盒为空时返回空列表（而不是 0×0 的怪东西）。
     */
    public <T> List<T> crop(List<T> items, int gridWidth) {
        if (isEmpty()) {
            return List.of();
        }
        var cropped = new ArrayList<T>(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cropped.add(items.get((top + y) * gridWidth + (left + x)));
            }
        }
        return cropped;
    }

    /**
     * 把包围盒里的内容**居中**放进 {@code dstWidth × dstHeight} 的新网格。
     *
     * <p>这是 9×9 编码台与各种尺寸工作台之间的换算规则：无尽重生的四张台是 5/7/9/11 边长，
     * 合成拓展是 3/5/7/9，都比编码台小或一样大。玩家在编码台上把图案摆在哪个角落不该影响结果，
     * 所以一律先取包围盒、再以目标网格的中心对齐——这就是「填充样式优先居中」。</p>
     *
     * <p>落不到的空格用 {@code empty} 填充：{@code ItemStack} 一侧必须传 {@link ItemStack#EMPTY}
     * （{@code CraftingInput.ofPositioned} 会直接对元素调 {@code isEmpty()}，{@code null} 会 NPE），
     * {@code GenericStack} 一侧传 {@code null}（{@code ConfigInventory} 的空格表示）。</p>
     *
     * @param gridWidth 源网格的宽（{@code items} 的行宽）；高由 {@code items.size() / gridWidth} 决定
     * @throws IllegalArgumentException 包围盒装不进目标网格。宁可当场炸，也不要悄悄截掉原料
     */
    public <T> List<T> centerIn(List<T> items, int gridWidth, int dstWidth, int dstHeight, T empty) {
        var centered = new ArrayList<T>(dstWidth * dstHeight);
        for (int i = 0; i < dstWidth * dstHeight; i++) {
            centered.add(empty);
        }
        if (isEmpty()) {
            return centered;
        }
        if (width > dstWidth || height > dstHeight) {
            throw new IllegalArgumentException(
                    "bounds " + width + "x" + height + " do not fit into " + dstWidth + "x" + dstHeight);
        }

        // 中心对齐：把包围盒的中心移到目标网格的中心。整数除法向下取整，与 AE2 的 LEFT_ALIGNED 偏移口径无关。
        int dx = (dstWidth - width) / 2 - left;
        int dy = (dstHeight - height) / 2 - top;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                centered.set((top + y + dy) * dstWidth + (left + x + dx),
                        items.get((top + y) * gridWidth + (left + x)));
            }
        }
        return centered;
    }

    /**
     * {@link #crop} 的逆运算：把包围盒坐标系下的一张表写回整张网格，其余格填 {@code empty}。
     *
     * <p>配方余料（铁桶剩空桶、无尽的不消耗催化剂）按<b>裁剪后的输入</b>返回，而装配室要按
     * 9×9 网格逐格回填，所以需要这一层换算。返回列表的长度是 {@code gridWidth * gridHeight}。</p>
     *
     * @param gridHeight 整张网格的高，用于校验长度
     */
    public <T> List<T> scatter(List<T> values, int gridWidth, int gridHeight, T empty) {
        var result = new ArrayList<T>(gridWidth * gridHeight);
        for (int i = 0; i < gridWidth * gridHeight; i++) {
            result.add(empty);
        }
        if (isEmpty()) {
            return result;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.set((top + y) * gridWidth + (left + x), values.get(y * width + x));
            }
        }
        return result;
    }

    /**
     * {@link #centerIn} 的逆运算：把居中后的 {@code dstWidth × dstHeight} 表写回源网格。
     *
     * <p>无尽表格配方吃的是居中到台面尺寸的输入，余料也是按台面返回的；回填 9×9 时要按同一套
     * 中心对齐偏移反推。超出包围盒的目标格（居中的留白）直接忽略。</p>
     *
     * <p><b>余料表的坐标系有两种，靠长度区分</b>：</p>
     * <ul>
     *   <li>原版口径（{@code CraftingRecipe#getRemainingItems} 的默认实现）按<b>输入尺寸</b>返回，
     *       也就是 {@code dstWidth × dstHeight}，下标是台面坐标；</li>
     *   <li>部分模组（无尽重生的表格配方）按<b>配方 pattern 尺寸</b>返回，也就是包围盒尺寸，
     *       下标是包围盒坐标。</li>
     * </ul>
     * <p>长度相同时（包围盒恰好铺满台面）两套公式退化成同一个，所以这个判别是安全的。
     * 早先只按台面坐标取，遇到第二种会拿 {@code 11×11} 的下标去读 81 格的表，
     * 表现为 {@code Index 81 out of bounds for length 81}。</p>
     */
    public <T> List<T> scatterCentered(List<T> values, int gridWidth, int gridHeight,
            int dstWidth, int dstHeight, T empty) {
        var result = new ArrayList<T>(gridWidth * gridHeight);
        for (int i = 0; i < gridWidth * gridHeight; i++) {
            result.add(empty);
        }
        if (isEmpty()) {
            return result;
        }
        int offsetX = (dstWidth - width) / 2;
        int offsetY = (dstHeight - height) / 2;
        boolean boundsSpace = values.size() != dstWidth * dstHeight;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int source = boundsSpace
                        ? y * width + x
                        : (y + offsetY) * dstWidth + (x + offsetX);
                if (source >= 0 && source < values.size()) {
                    result.set((top + y) * gridWidth + (left + x), values.get(source));
                }
            }
        }
        return result;
    }
}
