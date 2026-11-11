package io.github.lounode.ae2crafting.registry;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.block.AEBaseBlock;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.block.MolecularAssemblerBlock;

/**
 * 基线方块。外观沿用 AE2 本体分子装配室，9x9 网格与催化剂槽的配方逻辑待接入。
 *
 * <p>属性直接用 AE2 的 {@link AEBaseBlock#metalProps()}，与本体装配室同硬度、同音符与同金属音效；
 * {@code noOcclusion} 是因为模型走 cutout，需要关闭相邻面剔除。</p>
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AE2CraftingIntegration.MOD_ID);

    public static final DeferredBlock<MolecularAssemblerBlock> MOLECULAR_ASSEMBLER = BLOCKS.register(
            "molecular_assembler",
            () -> new MolecularAssemblerBlock(AEBaseBlock.metalProps().noOcclusion()));

    private ModBlocks() {
    }
}
