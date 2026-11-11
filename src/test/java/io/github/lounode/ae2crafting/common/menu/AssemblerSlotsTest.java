package io.github.lounode.ae2crafting.common.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.lounode.ae2crafting.common.menu.AssemblerSlots;

class AssemblerSlotsTest {

    @Test
    void machineSlotCountsMatchTheNineByNineGrid() {
        assertEquals(81, AssemblerSlots.GRID_SLOTS);
        assertEquals(9, AssemblerSlots.CATALYST_SLOTS);
        assertEquals(1, AssemblerSlots.OUTPUT_SLOTS);
        assertEquals(91, AssemblerSlots.MACHINE_SLOTS);
    }

    @Test
    void gridIndexIsRowMajor() {
        assertEquals(0, AssemblerSlots.grid(0, 0));
        assertEquals(8, AssemblerSlots.grid(0, 8));
        assertEquals(9, AssemblerSlots.grid(1, 0));
        assertEquals(80, AssemblerSlots.grid(8, 8));
    }

    @Test
    void catalystAndOutputFollowTheGridWithoutGaps() {
        assertEquals(81, AssemblerSlots.catalyst(0));
        assertEquals(89, AssemblerSlots.catalyst(8));
        assertEquals(90, AssemblerSlots.output());
        assertTrue(AssemblerSlots.isMachineSlot(90));
        assertFalse(AssemblerSlots.isMachineSlot(91));
    }
}
