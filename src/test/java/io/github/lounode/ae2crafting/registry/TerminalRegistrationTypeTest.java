package io.github.lounode.ae2crafting.registry;

import org.junit.jupiter.api.Test;

import appeng.api.parts.IPartItem;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.items.parts.PartItem;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.me.common.MEStorageMenu;
import appeng.parts.encoding.PatternEncodingTerminalPart;

import io.github.lounode.ae2crafting.common.item.WirelessAugmentedPatternTerminalHost;
import io.github.lounode.ae2crafting.common.item.WirelessAugmentedPatternTerminalItem;
import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;
import io.github.lounode.ae2crafting.common.part.AugmentedPatternEncodingTerminalPart;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 两个编码终端的继承链锁定。
 *
 * <p>终端能用的前提全在这几条继承关系上，而它们降级时编译一样过，只是实机表现为
 * 「放不到线缆上 / 手持右键没反应」：</p>
 *
 * <ul>
 *   <li>{@link PartItem} + {@link IPartItem}：物品才带 {@code useOn} 放置部件、并能创建 Part 实例；
 *       换成普通 {@code Item} 就只是块贴图。</li>
 *   <li>{@link PatternEncodingTerminalPart}：编码台逻辑、菜单类型、界面与部件模型全部来自它；
 *       换成裸 {@code AbstractTerminalPart} 就没有编码格。</li>
 *   <li>{@link WirelessTerminalItem}：无线形态的电量、充能、网格链接与右键打开都在这条链上。</li>
 *   <li>{@link IPatternTerminalMenuHost}：{@link AugmentedPatternEncodingTermMenu} 构造时按这个接口取
 *       {@code PatternEncodingLogic}，宿主不实现它菜单直接构不出来。</li>
 * </ul>
 */
class TerminalRegistrationTypeTest {

    @Test
    void meTerminalIsAnAe2EncodingPart() {
        assertTrue(IPartItem.class.isAssignableFrom(PartItem.class));
        assertTrue(PatternEncodingTerminalPart.class
                .isAssignableFrom(AugmentedPatternEncodingTerminalPart.class));
    }

    @Test
    void wirelessTerminalUsesAe2WirelessBase() {
        assertTrue(WirelessTerminalItem.class.isAssignableFrom(WirelessAugmentedPatternTerminalItem.class));
        assertTrue(IPatternTerminalMenuHost.class.isAssignableFrom(WirelessAugmentedPatternTerminalHost.class));
    }

    /**
     * 自有菜单仍必须是 ME 终端菜单（{@link MEStorageMenu}）：网络库存、搜索、排序与滚动列表
     * 全靠这条继承链，掉到 {@code AEBaseMenu} 就变成一个只能编码、读不到 ME 网络的普通工作台。
     *
     * <p>但<b>不再继承</b> AE2 的 {@code PatternEncodingTermMenu}——那个菜单绑死 3x3 合成台与
     * 四套模式面板（9 + 81 + 27 个 FakeSlot），和本界面的 9x9 编码区没有交集。</p>
     */
    @Test
    void customMenuTypeStillIsAStorageTerminalMenu() {
        assertTrue(MEStorageMenu.class.isAssignableFrom(AugmentedPatternEncodingTermMenu.class));
    }
}
