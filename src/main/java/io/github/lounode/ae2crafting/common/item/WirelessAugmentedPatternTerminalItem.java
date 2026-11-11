package io.github.lounode.ae2crafting.common.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import appeng.core.AEConfig;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.me.items.PatternEncodingTermMenu;

import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;

/**
 * 无线增广样板编码终端。
 *
 * <p>菜单类型是 {@link AugmentedPatternEncodingTermMenu#TYPE}（而不是 AE2 的
 * {@link PatternEncodingTermMenu#TYPE}）：一个菜单类型只绑一个界面，只有换成自有类型，
 * 客户端才会打开本模组的 {@code AugmentedPatternEncodingTermScreen} 与它读取的
 * {@code screens/ae2_crafting_integration/pattern_encoding_terminal.json}。
 * 编码行为仍由 {@link WirelessAugmentedPatternTerminalHost} 挂上去的
 * {@code PatternEncodingLogic} 提供。</p>
 *
 * <p>电量沿用 AE2 的无线终端配置（{@code AEConfig#getWirelessTerminalBattery}），
 * 因此充能器、无线终端升级与耗尽逻辑都和本体一致。</p>
 */
public class WirelessAugmentedPatternTerminalItem extends WirelessTerminalItem {

    public WirelessAugmentedPatternTerminalItem(Item.Properties properties) {
        // 延迟取配置：创造页注册物品时 AEConfig 还没读盘，真取电量时已经就绪。
        super(() -> AEConfig.instance().getWirelessTerminalBattery().getAsDouble(), properties);
    }

    @Override
    public MenuType<?> getMenuType() {
        return AugmentedPatternEncodingTermMenu.TYPE;
    }

    @Override
    public WirelessTerminalMenuHost<?> getMenuHost(Player player, ItemMenuHostLocator locator,
            @Nullable BlockHitResult hitResult) {
        return new WirelessAugmentedPatternTerminalHost(this, player, locator,
                (p, menu) -> openFromInventory(p, locator, true));
    }
}
