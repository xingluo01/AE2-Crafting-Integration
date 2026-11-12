package io.github.lounode.ae2crafting.common.logic;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * 同一组输入命中多条配方时的选择器。
 *
 * <p>本模组的判定器链默认「第一个非 {@code null} 的命中获胜」。装了多态合成（Polymorph）
 * 这类模组时，玩家可能已经在别处选过配方；这里留一个钩子，让判定器把候选交出去，
 * 由外部实现决定选哪条。返回 {@code null} 表示没选，判定器回退第一个候选。</p>
 *
 * <p>接口放在 common 层、只依赖原版类型：{@code integration/polymorph} 用反射实现它，
 * 缺模组时整条链退回「第一个获胜」，没有编译期引用。</p>
 */
public interface RecipeSelection {

    @Nullable
    <I extends RecipeInput, T extends Recipe<I>> RecipeHolder<T> select(
            RecipeType<T> type, I input, List<RecipeHolder<T>> candidates);
}
