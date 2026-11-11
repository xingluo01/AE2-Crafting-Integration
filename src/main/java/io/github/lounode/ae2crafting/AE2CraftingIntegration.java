package io.github.lounode.ae2crafting;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import io.github.lounode.ae2crafting.integration.ModIntegrations;
import io.github.lounode.ae2crafting.registry.ModBlockEntities;
import io.github.lounode.ae2crafting.registry.ModBlocks;
import io.github.lounode.ae2crafting.registry.ModCapabilities;
import io.github.lounode.ae2crafting.registry.ModCreativeTabs;
import io.github.lounode.ae2crafting.registry.ModDataComponents;
import io.github.lounode.ae2crafting.registry.ModItems;
import io.github.lounode.ae2crafting.registry.ModMenuTypes;
import io.github.lounode.ae2crafting.registry.ModSlotSemantics;
import io.github.lounode.ae2crafting.registry.ModTerminals;
import io.github.lounode.ae2crafting.registry.ModUpgrades;

/**
 * 模组入口。
 *
 * <p>基线三件套 + 装配室方块实体与菜单。装配室界面由 AEjson 组织；
 * 9x9 配方匹配的分发与邻居工作台适配见 {@code common/logic} 与 {@code integration}，
 * 样板执行（装配室 tick）与 ME 网络交互仍待接入。</p>
 */
@Mod(AE2CraftingIntegration.MOD_ID)
public final class AE2CraftingIntegration {

    public static final String MOD_ID = "ae2_crafting_integration";

    public AE2CraftingIntegration(IEventBus modBus) {
        ModSlotSemantics.init();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        // 模组总线事件，不能用 @EventBusSubscriber 默认的游戏总线挂，原因见 ModCapabilities 类注释。
        modBus.addListener(ModCapabilities::register);
        modBus.addListener(this::commonSetup);
    }

    /** 升级与无线链接登记必须等注册表冻结，见 {@link ModUpgrades} / {@link ModTerminals}。 */
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModUpgrades::register);
        event.enqueueWork(ModTerminals::register);
        // 9x9 工作台判定器：只有装了对应邻居才注册，隔离规则见 ModIntegrations 类注释。
        event.enqueueWork(ModIntegrations::registerRecipeMatchers);
    }
}
