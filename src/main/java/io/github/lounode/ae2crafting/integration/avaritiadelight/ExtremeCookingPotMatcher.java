package io.github.lounode.ae2crafting.integration.avaritiadelight;

import io.github.lounode.ae2crafting.integration.cooking.CookingPotMatcher;

/**
 * 无尽乐事终焉厨锅：配方类型 {@code avaritia_delight:extreme_cooking}。
 *
 * <p>81 格原料、容器顺位、反射读容器都与农夫乐事本体一致，实现收在
 * {@link CookingPotMatcher}，这里只绑定类型 id 与锅的物品 id。</p>
 */
public final class ExtremeCookingPotMatcher extends CookingPotMatcher {

    public ExtremeCookingPotMatcher() {
        super("avaritia_delight:extreme_cooking", AvaritiaDelightMachines.EXTREME_COOKING_POT);
    }
}
