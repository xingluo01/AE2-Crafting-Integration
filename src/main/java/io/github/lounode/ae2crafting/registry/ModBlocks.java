package io.github.lounode.ae2crafting.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;

/**
 * 基线方块。外观沿用 AE2 本体分子装配室，9x9 网格与催化剂槽逻辑待接入。
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AE2CraftingIntegration.MOD_ID);

    public static final DeferredBlock<Block> MOLECULAR_ASSEMBLER = BLOCKS.register(
            "molecular_assembler",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0F)));

    private ModBlocks() {
    }
}
