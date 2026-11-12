package io.github.lounode.ae2crafting.integration.farmersdelight;

import io.github.lounode.ae2crafting.integration.cooking.CookingPotMatcher;

/**
 * 农夫乐事本体厨锅：配方类型 {@code farmersdelight:cooking}。
 *
 * <p>与无尽乐事终焉厨锅共用 {@link CookingPotMatcher} 的 81 格 + 容器顺位逻辑；
 * 这里只绑定类型 id 与锅的物品 id。</p>
 */
public final class FarmersDelightCookingMatcher extends CookingPotMatcher {

    public FarmersDelightCookingMatcher() {
        super("farmersdelight:cooking", FarmersDelightMachines.COOKING_POT);
    }
}
