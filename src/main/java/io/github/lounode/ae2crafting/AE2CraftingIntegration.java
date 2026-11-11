package io.github.lounode.ae2crafting;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import io.github.lounode.ae2crafting.registry.ModBlocks;
import io.github.lounode.ae2crafting.registry.ModCreativeTabs;
import io.github.lounode.ae2crafting.registry.ModItems;

/**
 * 模组入口。
 *
 * <p>当前只注册三类基线对象：分子装配室方块、面板形态编码终端物品、无线终端物品。
 * 外观取 AE2 本体资源；9x9 合成、样板编码与 ME 网络交互逻辑待后续接入。
 */
@Mod(AE2CraftingIntegration.MOD_ID)
public final class AE2CraftingIntegration {

    public static final String MOD_ID = "ae2_crafting_integration";

    public AE2CraftingIntegration(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
    }
}
