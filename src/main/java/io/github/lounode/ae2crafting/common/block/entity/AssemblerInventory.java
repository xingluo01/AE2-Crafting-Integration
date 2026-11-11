package io.github.lounode.ae2crafting.common.block.entity;

import net.minecraft.world.item.ItemStack;

import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;

import io.github.lounode.ae2crafting.common.menu.AssemblerSlots;

/**
 * 装配室自身容器：81 格合成网格 + 9 格催化剂 + 1 格产物，共 91 格。
 *
 * <p>用 AE2 的 {@link AppEngInternalInventory} 而非自写 {@code Container}：AE2 的
 * {@code AEBaseInvBlockEntity} 把 {@code getInternalInventory()} 当作持久化、掉落与能力暴露的唯一来源，
 * 槽位变更经 {@link InternalInventoryHost#saveChangedInventory} 汇总到 {@code AEBaseBlockEntity#saveChanges}
 * （服务端合并到 tick 末尾标脏），自写容器拿不到这条链路。</p>
 */
public final class AssemblerInventory extends AppEngInternalInventory {

    /** 单格上限取 AE2 惯例 64，避免原版堆叠上限更高的物品被截断成 1。 */
    private static final int SLOT_LIMIT = 64;

    public AssemblerInventory(InternalInventoryHost host) {
        super(host, AssemblerSlots.MACHINE_SLOTS, SLOT_LIMIT);
    }

    /** 产物槽只出不进；网格与催化剂栏接受任意物品。 */
    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isInsertable(slot);
    }

    /**
     * 槽位可放行规则。
     *
     * <p>抽成静态纯函数是为了能单测：真正实例化容器会碰 {@code ItemStack.EMPTY}，
     * 那要求原版注册表 bootstrap，而 NeoForge 的 bootstrap 在纯 JUnit 里跑不起来。</p>
     */
    public static boolean isInsertable(int slot) {
        return slot != AssemblerSlots.output();
    }
}
