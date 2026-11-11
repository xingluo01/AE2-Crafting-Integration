package io.github.lounode.ae2crafting.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;

/** 创造模式页：三类基线对象集中放一处，便于实机查看外观。 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB, AE2CraftingIntegration.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ae2_crafting_integration"))
                    .icon(() -> new ItemStack(ModBlocks.MOLECULAR_ASSEMBLER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.MOLECULAR_ASSEMBLER.get());
                        output.accept(ModItems.PATTERN_ENCODING_TERMINAL.get());
                        output.accept(ModItems.WIRELESS_TERMINAL.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
