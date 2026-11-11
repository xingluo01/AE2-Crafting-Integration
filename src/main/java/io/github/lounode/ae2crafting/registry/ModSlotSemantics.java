package io.github.lounode.ae2crafting.registry;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

/**
 * 本模组自定义槽位语义。
 *
 * <p>注册名必须与 {@code molecular_assembler.json} / {@code pattern_encoding_terminal.json}
 * 里 {@code slots} 的键逐字一致；AE2 要求自定义语义带本模组 namespace 前缀。名字即协议，
 * 改名要同步改 JSON。</p>
 *
 * <p>漏注册的后果不是「槽位画歪」，而是<b>打开界面直接抛异常</b>：
 * {@code AEBaseScreen#positionSlots} 对 JSON 里的每个键调
 * {@code SlotSemantics.getOrThrow(key)}，所以新增键必须同时在这里登记。</p>
 */
public final class ModSlotSemantics {

    public static final SlotSemantic GRID =
            SlotSemantics.register("ae2_crafting_integration:grid", false);
    public static final SlotSemantic CATALYST =
            SlotSemantics.register("ae2_crafting_integration:catalyst", false);
    public static final SlotSemantic OUTPUT =
            SlotSemantics.register("ae2_crafting_integration:output", false);

    /** 编码终端的 9x9 增广合成编码槽（81 格幽灵槽）。 */
    public static final SlotSemantic AUGMENTED_GRID =
            SlotSemantics.register("ae2_crafting_integration:augmented_grid", false);
    /** 编码终端的「标志工作方块」槽：标记这套配方属于哪张工作台。 */
    public static final SlotSemantic WORKSTATION =
            SlotSemantics.register("ae2_crafting_integration:workstation", false);

    /** 触发静态字段注册：AE2 解析 ScreenStyle 时按名字查语义，必须先于界面构造完成。 */
    public static void init() {
    }

    private ModSlotSemantics() {
    }
}
