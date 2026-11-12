package io.github.lounode.ae2crafting.integration.farmersdelight;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import io.github.lounode.ae2crafting.common.logic.CraftRules;

/**
 * 农夫乐事本体厨锅的物品 id 登记。
 *
 * <p>模组只在 {@code localRuntime}，没有编译期依赖，所以这里只写字符串 id；
 * 配方类型 {@code farmersdelight:cooking} 由 {@link FarmersDelightCookingMatcher} 按 id 查。</p>
 */
public final class FarmersDelightMachines {

    public static final String COOKING_POT = "farmersdelight:cooking_pot";
    private static final ResourceLocation COOKING_POT_ID = ResourceLocation.parse(COOKING_POT);

    private FarmersDelightMachines() {
    }

    public static void registerRules() {
        CraftRules.registerSpecialWorkstation(COOKING_POT);
    }

    public static boolean isCookingPot(@Nullable ItemStack station) {
        return COOKING_POT_ID.equals(CraftRules.itemKey(station));
    }
}
