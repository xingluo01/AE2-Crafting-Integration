package io.github.lounode.ae2crafting.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.blockentity.AEBaseBlockEntity;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.block.entity.MolecularAssemblerBlockEntity;

/**
 * 方块实体注册。
 *
 * <p>AE2 的 {@code AEBaseEntityBlock#newBlockEntity} 不做 {@code new}，而是转调
 * {@code blockEntityType.create(...)}，因此注册时必须把类型回填给方块；漏了这一步 {@code newBlockEntity}
 * 会返回 null，方块放下去就是空壳，且不会报错。</p>
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2CraftingIntegration.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MolecularAssemblerBlockEntity>> MOLECULAR_ASSEMBLER =
            BLOCK_ENTITIES.register("molecular_assembler", () -> {
                var type = BlockEntityType.Builder
                        .of(MolecularAssemblerBlockEntity::new, ModBlocks.MOLECULAR_ASSEMBLER.get())
                        .build(null);

                // AE2 靠「方块实体类型 → 代表物品」这张表给 getName()、记忆卡导出与网格节点外观取图标。
                // 不登记不报错，只是机器在 Jade / 记忆卡里显示成空气。
                AEBaseBlockEntity.registerBlockEntityItem(type, ModItems.MOLECULAR_ASSEMBLER.get());

                // 装配用的 ticker 目前为 null（还没有逐 tick 的配方逻辑）；将来实现
                // ServerTickingBlockEntity 时，把 lambda 填到这两个参数里即可自动收到 tick。
                ModBlocks.MOLECULAR_ASSEMBLER.get().setBlockEntity(
                        MolecularAssemblerBlockEntity.class, type, null, null);

                return type;
            });

    private ModBlockEntities() {
    }
}
