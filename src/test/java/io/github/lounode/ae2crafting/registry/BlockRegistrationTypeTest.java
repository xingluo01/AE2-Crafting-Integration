package io.github.lounode.ae2crafting.registry;

import org.junit.jupiter.api.Test;

import appeng.api.networking.IInWorldGridNodeHost;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.me.helpers.IGridConnectedBlockEntity;

import io.github.lounode.ae2crafting.common.block.MolecularAssemblerBlock;
import io.github.lounode.ae2crafting.common.block.entity.MolecularAssemblerBlockEntity;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 方块与方块实体的注册基类锁定。
 *
 * <p>这两条继承关系是 AE2 能力面的前提：{@code AEBaseEntityBlock} 负责把 {@code newBlockEntity}
 * 转给已回填的 {@code BlockEntityType} 并在破坏时掉落内部库存；
 * {@code AENetworkedInvBlockEntity} 提供网格节点与 {@code IInWorldGridNodeHost}
 * （没有后者，能力登记里的 {@code (be, context) -> be} 会编译不过）。</p>
 *
 * <p>降级成裸 {@code Block} / {@code BlockEntity} 时编译仍会过，只在实机表现为「不放方块实体、
 * 不上网、掉落清空」，所以在这里钉住。</p>
 */
class BlockRegistrationTypeTest {

    @Test
    void blockUsesAe2EntityBlockBase() {
        assertTrue(AEBaseEntityBlock.class.isAssignableFrom(MolecularAssemblerBlock.class));
    }

    @Test
    void blockEntityUsesAe2NetworkedInventoryBase() {
        assertTrue(AEBaseBlockEntity.class.isAssignableFrom(MolecularAssemblerBlockEntity.class));
        assertTrue(IInWorldGridNodeHost.class.isAssignableFrom(MolecularAssemblerBlockEntity.class));
        assertTrue(IGridConnectedBlockEntity.class.isAssignableFrom(MolecularAssemblerBlockEntity.class));
    }
}
