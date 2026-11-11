package io.github.lounode.ae2crafting.common.block.entity;

/**
 * 装配室加速档位表。
 *
 * <p>两列数值逐项取自 AE2 本体分子装配室 {@code MolecularAssemblerBlockEntity#tickingRequest}：
 * {@code speed}（每 tick 的基础进度）与 {@code acceleratorTax}（能量倍率）。0..3 张与本体完全一致，
 * <b>第 4 张取本体第 5 张的 {@code 50 × 5.0}</b>——本体把「断档式跃升」放在第 5 张，
 * 本模组只开 4 个升级槽，于是把这一跃升挪到第 4 张。</p>
 *
 * <p>由此得到每张卡的推进量 {@code 10 → 16.9 → 28.9 → 40 → 250}，插满 4 张时单 tick 推进 250 点，
 * 直接越过 {@link #FULL_PROGRESS}，即<b>每 tick 完成一次合成</b>；0..3 张分别是 10 / 6 / 4 / 3 tick 一次。</p>
 *
 * <p>这里只放纯计算，不碰注册表与物品实例，便于单测直接覆盖。</p>
 */
public final class AssemblerAcceleration {

    /** 与 AE2 本体一致：进度满 100 完成一次合成。 */
    public static final int FULL_PROGRESS = 100;

    /** 升级槽数量，同时也是可安装加速卡的上限。 */
    public static final int MAX_CARDS = 4;

    /** 第 0..4 张对应的基础进度（本体 {@code speed} 列）。 */
    private static final int[] SPEED = { 10, 13, 17, 20, 50 };
    /** 第 0..4 张对应的能量倍率（本体 {@code acceleratorTax} 列）。 */
    private static final double[] TAX = { 1.0, 1.3, 1.7, 2.0, 5.0 };

    /** 超出范围的卡数一律夹到 0..{@link #MAX_CARDS}，避免升级库存异常时数组越界。 */
    public static int clampCards(int cards) {
        if (cards <= 0) {
            return 0;
        }
        return Math.min(cards, MAX_CARDS);
    }

    /** 每 tick 推进的进度点数。 */
    public static double progressPerTick(int cards) {
        int index = clampCards(cards);
        return SPEED[index] * TAX[index];
    }

    /** 完成一次合成需要的 tick 数（向上取整，最少 1）。 */
    public static int ticksPerCraft(int cards) {
        return Math.max(1, (int) Math.ceil(FULL_PROGRESS / progressPerTick(cards)));
    }

    private AssemblerAcceleration() {
    }
}
