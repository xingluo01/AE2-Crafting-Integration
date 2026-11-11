package io.github.lounode.ae2crafting.common.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 加速档位表的行为锁定。
 *
 * <p>这些数值是玩法承诺，不是内部实现细节：{@code docs/TODO.md} 与界面上展示的
 * 「插满 4 张每 tick 一次」全部由这里保证。改动 {@link AssemblerAcceleration} 的数组
 * 会让下面每一条断言立刻失败。</p>
 */
class AssemblerAccelerationTest {

    private static final double DELTA = 1.0E-9;

    @Test
    void perTickProgressMatchesAe2MolecularAssembler() {
        // AE2 本体 0..3 张的 speed × acceleratorTax：10×1.0 / 13×1.3 / 17×1.7 / 20×2.0
        assertEquals(10.0, AssemblerAcceleration.progressPerTick(0), DELTA);
        assertEquals(16.9, AssemblerAcceleration.progressPerTick(1), DELTA);
        assertEquals(28.9, AssemblerAcceleration.progressPerTick(2), DELTA);
        assertEquals(40.0, AssemblerAcceleration.progressPerTick(3), DELTA);
        // 第 4 张取本体第 5 张的 50×5.0
        assertEquals(250.0, AssemblerAcceleration.progressPerTick(4), DELTA);
    }

    @Test
    void ticksPerCraftShrinksWithEachCard() {
        assertEquals(10, AssemblerAcceleration.ticksPerCraft(0));
        assertEquals(6, AssemblerAcceleration.ticksPerCraft(1));
        assertEquals(4, AssemblerAcceleration.ticksPerCraft(2));
        assertEquals(3, AssemblerAcceleration.ticksPerCraft(3));
    }

    @Test
    void fourCardsCraftEveryTick() {
        assertEquals(1, AssemblerAcceleration.ticksPerCraft(AssemblerAcceleration.MAX_CARDS));
        // 单 tick 的推进量必须越过满进度，否则「每 tick 一次」只是取整后的巧合。
        assertEquals(true,
                AssemblerAcceleration.progressPerTick(AssemblerAcceleration.MAX_CARDS)
                        >= AssemblerAcceleration.FULL_PROGRESS);
    }

    @Test
    void cardCountIsClampedToSupportedRange() {
        assertEquals(0, AssemblerAcceleration.clampCards(-3));
        assertEquals(0, AssemblerAcceleration.clampCards(0));
        assertEquals(AssemblerAcceleration.MAX_CARDS, AssemblerAcceleration.clampCards(9));
        // 越界输入不能抛数组越界，而是夹到两端
        assertEquals(10.0, AssemblerAcceleration.progressPerTick(-1), DELTA);
        assertEquals(250.0, AssemblerAcceleration.progressPerTick(99), DELTA);
    }
}
