package io.github.lounode.ae2crafting.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import appeng.api.AECapabilities;

/**
 * 把本模组的方块实体接进 AE2 的能力面。
 *
 * <p><b>为什么必须自己登记</b>：AE2 只在 {@code InitCapabilityProviders} 里遍历
 * {@code AEBlockEntities} 自己的类型列表做批量登记，第三方的 {@code BlockEntityType} 不在那张表里。
 * 漏登记不报错，只表现为「线缆连不上」「管道看不到库存」这类静默失效。</p>
 *
 * <p><b>为什么不用 {@code @EventBusSubscriber}</b>：{@link RegisterCapabilitiesEvent} 是模组总线事件，
 * 注解默认挂在游戏总线上会永远收不到——不报错、不警告。这里由入口类显式
 * {@code modBus.addListener(ModCapabilities::register)}。</p>
 */
public final class ModCapabilities {

    public static void register(RegisterCapabilitiesEvent event) {
        // 网格节点宿主：相邻线缆/设备靠它拿到本机节点。没有它，机器永远不上网，
        // 而 AENetworkedInvBlockEntity 的节点已经建好了，只是没人能发现。
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.MOLECULAR_ASSEMBLER.get(),
                (blockEntity, context) -> blockEntity);

        // 物品能力：暴露催化剂栏与产物槽（9x9 网格刻意不给外部注入）。
        // AE2 对自家装配室的同一能力也只允许从产物槽抽取，这里用 isItemValid 达成同样效果。
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.MOLECULAR_ASSEMBLER.get(),
                (blockEntity, side) -> blockEntity.getExposedInventory().toItemHandler());
    }

    private ModCapabilities() {
    }
}
