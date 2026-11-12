package io.github.lounode.ae2crafting.common.logic;

/**
 * 一次配方命中的语义类别。
 *
 * <p>判定器住在 {@code integration/<邻居>} 里，装配室住在 {@code common/}，两边不能直接传邻居的
 * 配方类型。这个枚举就是中立的交接语言：判定器告诉装配室「这是哪一类机器」，装配室据此决定
 * 要不要按发电机折减速度、要不要按基座数量校验输入槽位。</p>
 */
public enum CraftKind {

    /** 普通网格合成（原版、合成拓展表格、无尽表格与锻造），不受任何附加规则影响。 */
    NORMAL,
    /** 合成拓展末影合成器：耗时按末影发电机数量折减。 */
    ENDER_CRAFTER,
    /** 合成拓展通量合成器：耗时按通量发电机数量折减。 */
    FLUX_CRAFTER,
    /** 合成拓展组合合成器：输入槽位由基座数量解锁，网格按中心 + 螺旋还原。 */
    COMBINATION,
    /** 无尽乐事终焉厨锅：容器作为最后一个原材料参与计算。 */
    COOKING_POT;

    /** 这类机器是不是靠「发电机」缩短耗时。 */
    public boolean isAlternatorDriven() {
        return this == ENDER_CRAFTER || this == FLUX_CRAFTER;
    }
}
