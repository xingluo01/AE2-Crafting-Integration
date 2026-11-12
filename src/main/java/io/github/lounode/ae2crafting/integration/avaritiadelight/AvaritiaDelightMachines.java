package io.github.lounode.ae2crafting.integration.avaritiadelight;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import io.github.lounode.ae2crafting.common.logic.CraftRules;

/**
 * 无尽乐事终焉厨锅的物品 id 登记。
 *
 * <p>模组 id 是 {@code avaritia_delight}（不是 CurseForge 项目名 {@code avaritias_delight}），
 * 资源命名空间与 id 一致。这里只登记字符串，不引用邻居类型。</p>
 */
public final class AvaritiaDelightMachines {

    public static final String EXTREME_COOKING_POT = "avaritia_delight:extreme_cooking_pot";

    private static final ResourceLocation EXTREME_COOKING_POT_ID =
            ResourceLocation.parse(EXTREME_COOKING_POT);

    private AvaritiaDelightMachines() {
    }

    public static void registerRules() {
        CraftRules.registerSpecialWorkstation(EXTREME_COOKING_POT);
    }

    public static boolean isExtremeCookingPot(@Nullable ItemStack station) {
        return EXTREME_COOKING_POT_ID.equals(CraftRules.itemKey(station));
    }
}
