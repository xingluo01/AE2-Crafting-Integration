package io.github.lounode.ae2crafting.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.items.parts.PartItem;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
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
 * <p>增广合成样板由 {@link PatternDetailsHelper#encodedPatternItemBuilder} 生成而不是手写
 * {@code Item} 子类：AE2 的 {@code AEPatternDecoder} 只认 {@code EncodedPatternItem} 这一种类型，
 * 自己实现的物品不会被 {@code PatternDetailsHelper#isEncodedPattern}/{@code decodePattern} 看见，
 * 表现是「样板放不进供应器、也不算样板」。</p>
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
            () -> new WirelessAugmentedPatternTerminalItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> AUGMENTED_CRAFTING_PATTERN = ITEMS.register(
            "augmented_crafting_pattern",
            () -> PatternDetailsHelper.<AugmentedPattern>encodedPatternItemBuilder(AugmentedPattern::new)
                    .itemProperties(new Item.Properties().stacksTo(1))
                    .build());

    private ModItems() {
    }
}
