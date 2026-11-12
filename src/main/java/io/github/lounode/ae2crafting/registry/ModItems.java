package io.github.lounode.ae2crafting.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.items.parts.PartItem;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.item.AugmentedPatternItem;
import io.github.lounode.ae2crafting.common.item.WirelessAugmentedPatternTerminalItem;
import io.github.lounode.ae2crafting.common.part.AugmentedPatternEncodingTerminalPart;
import io.github.lounode.ae2crafting.common.pattern.AugmentedPattern;

/**
 * 物品注册。
 *
 * <p>两个编码终端借 AE2 的壳：面板形态是 {@link PartItem}，对应
 * {@link AugmentedPatternEncodingTerminalPart}（继承 AE2 的样板编码终端 Part）；
 * 无线形态是 {@link WirelessAugmentedPatternTerminalItem}（继承 AE2 的无线终端基类）。
 * 两者的菜单/界面/纹理当前都用 AE2 样板编码终端那一套，9x9 自有界面待接。</p>
 *
 * <p>增广合成样板是 {@link AugmentedPatternItem}——它<b>必须</b>继承 AE2 的
 * {@code EncodedPatternItem}：{@code AEPatternDecoder} 只认这一种类型，换成普通 {@code Item}
 * 子类会不被 {@code PatternDetailsHelper#isEncodedPattern}/{@code decodePattern} 看见，
 * 表现是「样板放不进供应器、也不算样板」。继承它只是为了自定义 tooltip 排版
 * （严格标志物行 + 合并输入 + 坐标），解码器仍传 {@link AugmentedPattern}。</p>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2CraftingIntegration.MOD_ID);

    public static final DeferredItem<BlockItem> MOLECULAR_ASSEMBLER = ITEMS.register(
            "molecular_assembler",
            () -> new BlockItem(ModBlocks.MOLECULAR_ASSEMBLER.get(), new Item.Properties()));

    /** ME 增广样板编码终端：放到线缆上的部件形态（注册 id 沿用 AE2 原名）。 */
    public static final DeferredItem<PartItem<AugmentedPatternEncodingTerminalPart>> PATTERN_ENCODING_TERMINAL = ITEMS
            .register(
                    "pattern_encoding_terminal",
                    () -> new PartItem<>(new Item.Properties(),
                            AugmentedPatternEncodingTerminalPart.class,
                            AugmentedPatternEncodingTerminalPart::new));

    /** 无线增广样板编码终端：手持打开的无线形态（注册 id 沿用 AE2 原名）。 */
    public static final DeferredItem<WirelessAugmentedPatternTerminalItem> WIRELESS_TERMINAL = ITEMS.register(
            "wireless_terminal",
            // ItemWT 内部自己定 stacksTo(1) 与电量，不接受外部 Item.Properties。
            WirelessAugmentedPatternTerminalItem::new);

    public static final DeferredItem<AugmentedPatternItem> AUGMENTED_CRAFTING_PATTERN = ITEMS.register(
            "augmented_crafting_pattern",
            () -> new AugmentedPatternItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
