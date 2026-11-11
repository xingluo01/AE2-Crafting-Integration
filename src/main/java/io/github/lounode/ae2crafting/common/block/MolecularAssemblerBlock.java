package io.github.lounode.ae2crafting.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;

import io.github.lounode.ae2crafting.common.block.entity.MolecularAssemblerBlockEntity;
import io.github.lounode.ae2crafting.common.menu.MolecularAssemblerMenu;

/**
 * 通用分子装配室方块。
 *
 * <p>继承 {@link AEBaseEntityBlock} 而不是裸 {@code Block}：方块实体的创建、ticker 装配、破坏时掉落
 * 内部库存、记忆卡读写、比较器输出都在这条链上。代价是必须在注册处调用
 * {@code setBlockEntity(...)} 把方块实体类型回填给方块，否则 {@code newBlockEntity} 会拿到 null。</p>
 */
public class MolecularAssemblerBlock extends AEBaseEntityBlock<MolecularAssemblerBlockEntity> {

    public MolecularAssemblerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        var blockEntity = getBlockEntity(level, pos);
        if (blockEntity == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            MenuOpener.open(MolecularAssemblerMenu.TYPE, player, MenuLocators.forBlockEntity(blockEntity));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
