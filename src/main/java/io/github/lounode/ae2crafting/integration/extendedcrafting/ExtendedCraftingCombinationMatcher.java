package io.github.lounode.ae2crafting.integration.extendedcrafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;

import com.blakebr0.extendedcrafting.init.ModRecipeTypes;

import io.github.lounode.ae2crafting.common.logic.CraftKind;
import io.github.lounode.ae2crafting.common.logic.GridMatcher;
import io.github.lounode.ae2crafting.common.logic.SpiralGrid;

/**
 * 合成拓展组合合成器（Crafting Core）：中心主物品 + 基座提供的无序输入。
 *
 * <p>配方的 {@code matches} 要的是一张<b>线性</b> {@code CraftingInput}：下标 0 是中心台的
 * 主物品，之后每个非空物品对应一个基座，用 {@code RecipeMatcher.findMatches} 无序比对。所以
 * 9×9 编码台按「中心 (4,4) 放主物品、其余顺时针螺旋展开」压成线性表，恰好把用户摆的图案
 * 翻译成配方认识的形状。</p>
 *
 * <p>基座不在这里计数：编码端按整张网格匹配，装配室执行时再按标志物栏里的基座数校验
 * 「基础 1 槽 + 每个基座 1 槽」是否够用，不够就拒绝发配。</p>
 */
public final class ExtendedCraftingCombinationMatcher implements GridMatcher {

    @Nullable
    @Override
    public Match match(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station) {
        if (!ExtendedCraftingMachines.isCraftingCore(station)
                || width != SpiralGrid.SIZE || height != SpiralGrid.SIZE) {
            return null;
        }

        var spiral = SpiralGrid.flatten(items);
        var compact = new ArrayList<ItemStack>(SpiralGrid.SLOTS);
        for (var item : spiral) {
            if (item != null && !item.isEmpty()) {
                compact.add(item);
            }
        }
        if (compact.isEmpty()) {
            return null;
        }

        // 宽 1、高 N 的线性输入；下标 0 就是螺旋起点（中心格）。
        var input = CraftingInput.of(1, compact.size(), compact);
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.COMBINATION.get())) {
            var recipe = holder.value();
            if (!recipe.matches(input, level)) {
                continue;
            }
            var remaining = spiralRemaining(recipe.getRemainingItems(input), spiral);
            return Match.special(recipe.assemble(input, level.registryAccess()), holder.id(),
                    remaining, CraftKind.COMBINATION, input.ingredientCount());
        }
        return null;
    }

    /**
     * 余料按线性输入返回，长度与 {@code compact} 一致；摊回螺旋顺序时跳过原来的空格，
     * 再由 {@link SpiralGrid#scatter} 写回行优先的 9×9。
     */
    private static List<ItemStack> spiralRemaining(List<ItemStack> remaining,
            List<@Nullable ItemStack> spiral) {
        var bySpiral = new ArrayList<ItemStack>(Collections.nCopies(SpiralGrid.SLOTS, ItemStack.EMPTY));
        int index = 0;
        for (int i = 0; i < SpiralGrid.SLOTS; i++) {
            var item = spiral.get(i);
            if (item != null && !item.isEmpty()) {
                if (index < remaining.size()) {
                    bySpiral.set(i, remaining.get(index));
                }
                index++;
            }
        }
        return SpiralGrid.scatter(bySpiral, ItemStack.EMPTY);
    }

    @Nullable
    @Override
    public Match match(Level level, List<ItemStack> items, int width, int height) {
        return null;
    }
}
