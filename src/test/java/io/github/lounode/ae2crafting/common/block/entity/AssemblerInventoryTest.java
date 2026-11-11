package io.github.lounode.ae2crafting.common.block.entity;

import org.junit.jupiter.api.Test;

import appeng.api.inventories.InternalInventory;

import io.github.lounode.ae2crafting.common.menu.AssemblerSlots;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 容器契约：产物槽不能放入，其余槽放行。
 *
 * <p>这条规则同时决定界面拖放、管道注入与能力暴露三处行为，改错不会编译失败，只在实机表现成
 * 「产物槽能被塞满」。判定逻辑是 {@link AssemblerInventory#isInsertable(int)}，
 * 这里只测纯函数，不实例化容器——容器构造会碰 {@code ItemStack.EMPTY}，
 * 需要原版注册表 bootstrap，而 NeoForge 的 bootstrap 在纯 JUnit 环境跑不起来。</p>
 */
class AssemblerInventoryTest {

    @Test
    void implementsAe2InternalInventory() {
        assertTrue(InternalInventory.class.isAssignableFrom(AssemblerInventory.class));
    }

    @Test
    void outputSlotRejectsInsertion() {
        assertFalse(AssemblerInventory.isInsertable(AssemblerSlots.output()));
    }

    @Test
    void gridAndCatalystSlotsAcceptInsertion() {
        assertTrue(AssemblerInventory.isInsertable(AssemblerSlots.grid(0, 0)));
        assertTrue(AssemblerInventory.isInsertable(AssemblerSlots.grid(8, 8)));
        assertTrue(AssemblerInventory.isInsertable(AssemblerSlots.catalyst(0)));
        assertTrue(AssemblerInventory.isInsertable(AssemblerSlots.catalyst(AssemblerSlots.CATALYST_SLOTS - 1)));
    }
}
