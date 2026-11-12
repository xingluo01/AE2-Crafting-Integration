package io.github.lounode.ae2crafting.integration.extendedcrafting;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.blakebr0.extendedcrafting.init.ModRecipeTypes;

import io.github.lounode.ae2crafting.common.logic.CraftKind;
import io.github.lounode.ae2crafting.common.logic.GridBounds;
import io.github.lounode.ae2crafting.common.logic.GridMatcher;

/**
 * 合成拓展末影合成器 / 通量合成器的 3×3 网格配方。
 *
 * <p>这两台机器吃的是 {@code Recipe<CraftingInput>}，与原版合成台同形状，所以必须靠标志物
 * 分流：原版判定器在 {@code CraftRules} 登记的特殊工作台面前让路，这里才接手。图案先按非空格
 * 包围盒裁成 3×3 以内再匹配，摆在 9×9 编码台的哪个角落都不影响结果。</p>
 *
 * <p>耗时不做在这里：装配室执行时按标志物栏里的发电机数量折减速度，编码端无需知道加成。</p>
 */
public final class ExtendedCraftingAlternatorMatcher implements GridMatcher {

    /** 末影/通量合成器的容器是 3×3。 */
    private static final int MAX_SIZE = 3;

    private final CraftKind kind;

    public ExtendedCraftingAlternatorMatcher(CraftKind kind) {
        if (!kind.isAlternatorDriven()) {
            throw new IllegalArgumentException("Not an alternator-driven crafter: " + kind);
        }
        this.kind = kind;
    }

    @Nullable
    @Override
    public Match match(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station) {
        var actual = ExtendedCraftingMachines.kindOf(station);
        if (actual != kind) {
            return null;
        }
        var bounds = GridBounds.ofItems(items, width, height);
        if (bounds.isEmpty() || bounds.width() > MAX_SIZE || bounds.height() > MAX_SIZE) {
            return null;
        }
        var input = CraftingInput.of(bounds.width(), bounds.height(), bounds.crop(items, width));

        var match = kind == CraftKind.ENDER_CRAFTER
                ? tryMatch(level, ModRecipeTypes.ENDER_CRAFTER.get(), input, bounds, width, height)
                : tryMatch(level, ModRecipeTypes.FLUX_CRAFTER.get(), input, bounds, width, height);
        return match;
    }

    private <T extends Recipe<CraftingInput>> Match tryMatch(Level level, RecipeType<T> type,
            CraftingInput input, GridBounds bounds, int width, int height) {
        for (RecipeHolder<T> holder : level.getRecipeManager().getAllRecipesFor(type)) {
            var recipe = holder.value();
            if (!recipe.matches(input, level)) {
                continue;
            }
            var remaining = bounds.scatter(recipe.getRemainingItems(input), width, height, ItemStack.EMPTY);
            return Match.special(recipe.assemble(input, level.registryAccess()),
                    holder.id(), remaining, kind, 0);
        }
        return null;
    }

    @Nullable
    @Override
    public Match match(Level level, List<ItemStack> items, int width, int height) {
        // 没有标志物就无从判断是哪台机器，交给判定器链后面的普通判定器。
        return null;
    }
}
