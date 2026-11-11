package io.github.lounode.ae2crafting.registry;

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
    }

    private ModUpgrades() {
    }
}
