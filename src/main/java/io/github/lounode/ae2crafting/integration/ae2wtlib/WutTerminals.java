package io.github.lounode.ae2crafting.integration.ae2wtlib;

import de.mari_023.ae2wtlib.api.gui.Icon;
import de.mari_023.ae2wtlib.api.registration.AddTerminalEvent;

import io.github.lounode.ae2crafting.common.item.WirelessAugmentedPatternTerminalHost;
import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;
import io.github.lounode.ae2crafting.registry.ModItems;

/**
 * 把无线增广样板编码终端登记进 AE2WTLib 的无线通用终端（WUT）体系。
 *
 * <p>{@code ae2wtlib:upgrade} / {@code combine} 融合配方在数据加载时会
 * {@code WTDefinition.of(terminalName)}，名字没登记过就是 {@code NullPointerException}
 * ——所以「只加配方 JSON」是行不通的，必须先有这条 WTDefinition。</p>
 *
 * <p><b>为什么在模组构造里就能注册</b>：{@link AddTerminalEvent#register} 只是把回调存进列表，
 * {@code AddTerminalEvent.run()} 由 {@code ae2wtlib_api} 在 {@code RegisterEvent(ITEM)} 时才执行。
 * 回调真正运行时 {@code DeferredItem} 已经绑定，可以安全 {@code .get()}。放到
 * {@code FMLCommonSetupEvent} 反而太晚，会抛
 * {@code "Cannot register terminal registration handler after terminal registration already happened"}。</p>
 *
 * <p>宿主工厂直接用 {@link WirelessAugmentedPatternTerminalHost} 的构造：签名与
 * {@code WTMenuHostFactory} 一致，WUT 切到本终端时能拿到带编码台与标志物槽的同一个宿主。</p>
 */
public final class WutTerminals {

    /** 融合配方 JSON 里的 {@code terminalName}，也是 WUT 物品上那枚组件标记的名字。 */
    public static final String TERMINAL_NAME = "augmented_pattern_encoding";

    public static void register() {
        AddTerminalEvent.register(event -> event.builder(
                TERMINAL_NAME,
                WirelessAugmentedPatternTerminalHost::new,
                AugmentedPatternEncodingTermMenu.TYPE,
                ModItems.WIRELESS_TERMINAL.get(),
                Icon.PATTERN_ENCODING)
                // upgradeCount 默认 2，与「槽位维持为 2」一致，不要在这里改。
                .addTerminal());
    }

    private WutTerminals() {
    }
}
