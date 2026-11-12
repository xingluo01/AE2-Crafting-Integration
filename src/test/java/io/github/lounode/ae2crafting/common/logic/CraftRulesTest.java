package io.github.lounode.ae2crafting.common.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 特殊工作台的倍率与槽位规则。走纯算术重载，避开 {@code ItemStack} 的注册表初始化。
 */
class CraftRulesTest {

    @Test
    void alternatorDrivenCraftersStartAtFiftyTimes() {
        assertEquals(50, CraftRules.durationMultiplier(CraftKind.ENDER_CRAFTER, 0));
        assertEquals(50, CraftRules.durationMultiplier(CraftKind.FLUX_CRAFTER, 0));
    }

    @Test
    void eachAlternatorRemovesOneAndCapsAtFortyNine() {
        assertEquals(49, CraftRules.durationMultiplier(CraftKind.ENDER_CRAFTER, 1));
        assertEquals(25, CraftRules.durationMultiplier(CraftKind.FLUX_CRAFTER, 25));
        assertEquals(1, CraftRules.durationMultiplier(CraftKind.ENDER_CRAFTER, 49));
        assertEquals(1, CraftRules.durationMultiplier(CraftKind.ENDER_CRAFTER, 200));
        assertEquals(50, CraftRules.durationMultiplier(CraftKind.ENDER_CRAFTER, -3));
    }

    @Test
    void otherKindsIgnoreAlternators() {
        assertEquals(1, CraftRules.durationMultiplier(CraftKind.NORMAL, 0));
        assertEquals(1, CraftRules.durationMultiplier(CraftKind.NORMAL, 49));
        assertEquals(1, CraftRules.durationMultiplier(CraftKind.COMBINATION, 0));
        assertEquals(1, CraftRules.durationMultiplier(CraftKind.COOKING_POT, 49));
    }

    @Test
    void fluxCrafterChargesPerAlternator() {
        assertEquals(0L, CraftRules.fluxPowerCost(CraftKind.ENDER_CRAFTER, 49));
        assertEquals(0L, CraftRules.fluxPowerCost(CraftKind.NORMAL, 49));
        assertEquals(0L, CraftRules.fluxPowerCost(CraftKind.FLUX_CRAFTER, 0));
        assertEquals(8_000L, CraftRules.fluxPowerCost(CraftKind.FLUX_CRAFTER, 1));
        assertEquals(392_000L, CraftRules.fluxPowerCost(CraftKind.FLUX_CRAFTER, 49));
        assertEquals(392_000L, CraftRules.fluxPowerCost(CraftKind.FLUX_CRAFTER, 200));
    }

    @Test
    void pedestalsUnlockSlotsFromOneUpToEightyOne() {
        assertEquals(1, CraftRules.availableInputSlots(0));
        assertEquals(2, CraftRules.availableInputSlots(1));
        assertEquals(50, CraftRules.availableInputSlots(49));
        assertEquals(81, CraftRules.availableInputSlots(80));
        assertEquals(81, CraftRules.availableInputSlots(200));
        assertEquals(1, CraftRules.availableInputSlots(-1));
    }
}
