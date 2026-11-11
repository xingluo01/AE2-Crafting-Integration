package io.github.lounode.ae2crafting.registry;

import appeng.api.features.GridLinkables;
import appeng.items.tools.powered.WirelessTerminalItem;

/**
 * 终端类物品的运行期登记。
 *
 * <p>无线终端要能被无线接入点「链接」，必须在 {@link GridLinkables} 里按物品登记
 * {@code IGridLinkableHandler}。AE2 只在 {@code InitGridLinkables} 里给它自己那两个无线终端登记，
 * 漏了这一条时物品照常存在、也能打开界面，只是永远连不上网络——实机表现是右键接入点没反应。</p>
 *
 * <p>和升级卡登记一样，必须等注册表冻结后再跑，所以只从 {@code FMLCommonSetupEvent#enqueueWork} 调用。</p>
 */
public final class ModTerminals {

    public static void register() {
        GridLinkables.register(ModItems.WIRELESS_TERMINAL.get(), WirelessTerminalItem.LINKABLE_HANDLER);
    }

    private ModTerminals() {
    }
}
