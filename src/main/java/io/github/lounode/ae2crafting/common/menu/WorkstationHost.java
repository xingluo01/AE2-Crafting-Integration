package io.github.lounode.ae2crafting.common.menu;

import appeng.util.ConfigInventory;

/**
 * 由编码终端的宿主实现：交出「标志工作方块」那一格库存。
 *
 * <p>这一格不属于 AE2 的 {@code PatternEncodingLogic}（它只管模式、输入输出与样板槽），
 * 所以存档得由宿主自己做：线缆面板写部件 NBT，无线终端写物品的 {@code CUSTOM_DATA}。
 * 菜单只认这个接口，两种形态共用同一套读写逻辑，换宿主不必改菜单。</p>
 *
 * <p><b>为什么是 1 格 {@code ConfigInventory} 而不是普通物品栏</b>：与 81 格编码台一致，
 * 它自带 {@code createMenuWrapper()}，能直接架 {@code FakeSlot}，客户端的物品同步也走
 * 同一条槽位包通道；归还的库存对象在宿主生命周期内不变，菜单每次打开拿到的是同一份。</p>
 */
public interface WorkstationHost {

    /** 标志工作方块的库存（恒为 1 格）。实现方负责在内容变动时触发存档。 */
    ConfigInventory getWorkstationInventory();
}
