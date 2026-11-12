package io.github.lounode.ae2crafting.integration.extendedcrafting;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import io.github.lounode.ae2crafting.common.logic.CraftKind;
import io.github.lounode.ae2crafting.common.logic.CraftRules;

/**
 * 合成拓展特殊机器的物品 id 与登记。
 *
 * <p>只写字符串 id，不引用 {@code ModBlocks} 之类的邻居类型：登记动作发生在
 * {@code ModIntegrations} 的 {@code isLoaded} 分支里，但总在加载的 {@code CraftRules} 只认 id，
 * 缺模组时注册表为空、所有附加规则自动退化为普通合成。</p>
 */
public final class ExtendedCraftingMachines {

    public static final String ENDER_CRAFTER = "extendedcrafting:ender_crafter";
    public static final String FLUX_CRAFTER = "extendedcrafting:flux_crafter";
    public static final String CRAFTING_CORE = "extendedcrafting:crafting_core";
    public static final String ENDER_ALTERNATOR = "extendedcrafting:ender_alternator";
    public static final String FLUX_ALTERNATOR = "extendedcrafting:flux_alternator";
    public static final String PEDESTAL = "extendedcrafting:pedestal";

    private ExtendedCraftingMachines() {
    }

    /** 由 {@code ModIntegrations} 在确认模组已加载后调用一次。 */
    public static void registerRules() {
        CraftRules.registerSpecialWorkstation(ENDER_CRAFTER);
        CraftRules.registerSpecialWorkstation(FLUX_CRAFTER);
        CraftRules.registerSpecialWorkstation(CRAFTING_CORE);
        CraftRules.registerCatalyst(ENDER_ALTERNATOR, CraftRules.Catalyst.ENDER_ALTERNATOR);
        CraftRules.registerCatalyst(FLUX_ALTERNATOR, CraftRules.Catalyst.FLUX_ALTERNATOR);
        CraftRules.registerCatalyst(PEDESTAL, CraftRules.Catalyst.PEDESTAL);
    }

    public static boolean isEnderCrafter(@Nullable ItemStack station) {
        return ENDER_CRAFTER.equals(CraftRules.itemId(station));
    }

    public static boolean isFluxCrafter(@Nullable ItemStack station) {
        return FLUX_CRAFTER.equals(CraftRules.itemId(station));
    }

    public static boolean isCraftingCore(@Nullable ItemStack station) {
        return CRAFTING_CORE.equals(CraftRules.itemId(station));
    }

    /** 标志物对应的耗时规则类别；不是末影/通量合成器时返回 {@link CraftKind#NORMAL}。 */
    public static CraftKind kindOf(@Nullable ItemStack station) {
        if (isEnderCrafter(station)) {
            return CraftKind.ENDER_CRAFTER;
        }
        if (isFluxCrafter(station)) {
            return CraftKind.FLUX_CRAFTER;
        }
        return CraftKind.NORMAL;
    }
}
