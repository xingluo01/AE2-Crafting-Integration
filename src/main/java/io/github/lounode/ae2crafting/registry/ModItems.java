package io.github.lounode.ae2crafting.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;

/**
 * 基线物品。面板形态与无线形态的编码终端目前是普通物品，AE2 part / 无线终端交互待接入。
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2CraftingIntegration.MOD_ID);

    public static final DeferredItem<BlockItem> MOLECULAR_ASSEMBLER = ITEMS.register(
            "molecular_assembler",
            () -> new BlockItem(ModBlocks.MOLECULAR_ASSEMBLER.get(), new Item.Properties()));

    public static final DeferredItem<Item> PATTERN_ENCODING_TERMINAL = ITEMS.register(
            "pattern_encoding_terminal",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WIRELESS_TERMINAL = ITEMS.register(
            "wireless_terminal",
            () -> new Item(new Item.Properties()));

    private ModItems() {
    }
}
