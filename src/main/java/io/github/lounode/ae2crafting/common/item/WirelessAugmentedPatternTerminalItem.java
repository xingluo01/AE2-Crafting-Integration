package io.github.lounode.ae2crafting.common.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import appeng.menu.locator.ItemMenuHostLocator;

import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;

import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;

/**
 * 无线增广样板编码终端。
 *
 * <p>菜单类型是 {@link AugmentedPatternEncodingTermMenu#TYPE}（而不是 AE2 的
 * {@code PatternEncodingTermMenu#TYPE}）：一个菜单类型只绑一个界面，只有换成自有类型，
 * 客户端才会打开本模组的 {@code AugmentedPatternEncodingTermScreen} 与它读取的
 * {@code screens/ae2_crafting_integration/pattern_encoding_terminal.json}。
 * 编码行为由 {@link WirelessAugmentedPatternTerminalHost} 挂的 {@code PatternEncodingLogic} 提供。</p>
 *
 * <p><b>继承 {@link ItemWT}（AE2WTLib）而不是 {@code WirelessTerminalItem}</b>：无线通用终端
 * （WUT）的 {@code ae2wtlib:upgrade} / {@code combine} 融合配方要求目标终端在
 * {@code WTDefinition} 里登记过，而登记只收 {@code ItemWT}。换成这套基类后，本终端既能在
 * 独立形态下右键打开，也能被 WUT 吸收、之后从终端选择器里切回来。</p>
 *
 * <p>电量沿用 AE2 的无线终端配置（{@code ItemWT} 内部取 {@code AEConfig#getWirelessTerminalBattery}），
 * 因此充能器、量子桥卡与耗尽逻辑都与 AE2WTLib 的终端一致。</p>
 */
public class WirelessAugmentedPatternTerminalItem extends ItemWT {

    public WirelessAugmentedPatternTerminalItem() {
        super();
    }

    /** WUT / {@code ItemWT#open} 走这条带定位器的路径。 */
    @Override
    public MenuType<?> getMenuType(ItemMenuHostLocator locator, Player player) {
        return AugmentedPatternEncodingTermMenu.TYPE;
    }

    /** AE2 的 {@code WirelessTerminalItem} 路径（如链接断开后重开）走无参版本。 */
    @Override
    public MenuType<?> getMenuType() {
        return AugmentedPatternEncodingTermMenu.TYPE;
    }

    /**
     * 直接返回本模组的宿主，不走 {@code ItemWT} 默认的
     * {@code WTDefinition.of(stack).wTMenuHostFactory()} 二次转发——工厂是我们自己注册的，
     * 绕一圈只会多一次「按物品栈查定义」的机会，而且独立形态下定义还没登记时会拿到 null。
     */
    @Override
    public WTMenuHost getMenuHost(Player player, ItemMenuHostLocator locator,
            @Nullable BlockHitResult hitResult) {
        return new WirelessAugmentedPatternTerminalHost(this, player, locator,
                (p, subMenu) -> tryOpen(p, locator, true));
    }
}
