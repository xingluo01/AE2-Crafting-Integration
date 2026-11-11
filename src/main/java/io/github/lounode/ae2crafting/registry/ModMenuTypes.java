package io.github.lounode.ae2crafting.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;
import io.github.lounode.ae2crafting.common.menu.MolecularAssemblerMenu;

/** 菜单类型注册。菜单实体由 AE2 的 {@code MenuTypeBuilder} 建好后在这里挂到注册表。 */
public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AE2CraftingIntegration.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MolecularAssemblerMenu>> MOLECULAR_ASSEMBLER =
            MENUS.register("molecular_assembler", () -> MolecularAssemblerMenu.TYPE);

    /**
     * 终端必须有独立类型，否则客户端按 {@code menu.getType()} 会查回 AE2 的界面。
     * 注册名与 {@code AugmentedPatternEncodingTermMenu.TYPE} 里的 id 必须一致，改名等于换协议。
     */
    public static final DeferredHolder<MenuType<?>, MenuType<AugmentedPatternEncodingTermMenu>>
            AUGMENTED_PATTERN_TERMINAL =
            MENUS.register("augmented_pattern_terminal", () -> AugmentedPatternEncodingTermMenu.TYPE);

    private ModMenuTypes() {
    }
}
