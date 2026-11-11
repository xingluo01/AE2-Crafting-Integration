package io.github.lounode.ae2crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ModIdTest {

    @Test
    void modIdMatchesProjectMetadata() {
        assertEquals("ae2_crafting_integration", AE2CraftingIntegration.MOD_ID);
    }
}
