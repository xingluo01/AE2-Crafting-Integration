package io.github.lounode.ae2crafting.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;

import io.github.lounode.ae2crafting.common.block.entity.AssemblerAcceleration;

/**
 * 升级卡兼容性登记。
 *
 * <p>AE2 的升级体系要求「升级卡 × 可升级对象 × 上限」先登记，升级库存才会放行：
 * 没登记的卡塞不进去，{@code getMaxInstallable} 返回 0，界面上还会把空槽提示成不兼容。</p>
 *
 * <p><b>调用时机必须是 {@code FMLCommonSetupEvent} 的 enqueueWork</b>：AE2 自己的
 * {@code InitUpgrades.init()} 也在那里，且此时物品注册表已冻结，{@code AEItems.SPEED_CARD}
 * 才能安全取到实例。在模组构造里调用会拿到未绑定物品的 holder。</p>
 */
public final class ModUpgrades {

    public static void register() {
        // 上限即升级槽数量。本模组只开 4 张：第 4 张对应本体第 5 张的 50×5.0，插满即每 tick 一次合成。
        Upgrades.add(AEItems.SPEED_CARD, ModBlocks.MOLECULAR_ASSEMBLER.get(),
                AssemblerAcceleration.MAX_CARDS);

        // 无线终端沿用 AE2 无线终端的标准配置：2 张能源卡 + 1 张量子桥卡。
        // 槽位数由 ItemMenuHost 的升级库存决定（固定 2），这里只登记「能放什么」。
        var wireless = ModItems.WIRELESS_TERMINAL.get();
        Upgrades.add(AEItems.ENERGY_CARD, wireless, 2);
        // 量子桥卡出自 ae2wtlib（可选运行期模组），按注册名查，没装就静默跳过——
        // 不能在编译期直接引用它的物品类，否则没装 ae2wtlib 时会 NoClassDefFoundError。
        var quantumBridgeCard = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("ae2wtlib", "quantum_bridge_card"));
        if (quantumBridgeCard != Items.AIR) {
            Upgrades.add(quantumBridgeCard, wireless, 1);
        }
    }

    private ModUpgrades() {
    }
}
