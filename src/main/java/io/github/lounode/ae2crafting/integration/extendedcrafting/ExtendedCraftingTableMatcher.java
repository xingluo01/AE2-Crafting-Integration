package io.github.lounode.ae2crafting.integration.extendedcrafting;

import java.util.HashMap;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import com.blakebr0.extendedcrafting.api.TableCraftingInput;
import com.blakebr0.extendedcrafting.crafting.recipe.ShapedTableRecipe;
import com.blakebr0.extendedcrafting.init.ModRecipeTypes;

import io.github.lounode.ae2crafting.common.logic.GridBounds;
import io.github.lounode.ae2crafting.common.logic.GridMatcher;

/**
 * 合成拓展（Extended Crafting）工作台配方：基础 3×3、高级 5×5、精英 7×7、终极 9×9。
 *
 * <p><b>为什么要把整张 9×9 塞进去</b>：{@link TableCraftingInput#of} 内部走
 * {@code CraftingInput.ofPositioned}，先裁包围盒再交给 {@code ShapedRecipePattern}，
 * 所以「把原料摆在 9×9 的哪个角落」不影响匹配。裁法与 {@code GridBounds} 是同一套口径。</p>
 *
 * <p><b>tier 必须逐档试</b>：{@code ShapedTableRecipe#matches} 里写的是
 * {@code recipe.tier != 0 && recipe.tier != input.tier() → false}，即<b>等值</b>匹配而不是
 * 「不低于」。所以按配方自己的 {@code getTier()} 建输入，一档一个输入框，命中即返回；
 * 这也意味着编码器不检查玩家有没有那张工作台——真正的门槛留给装配室执行时。</p>
 *
 * <p>同类名只允许出现在 {@code integration/} 里，缺这个模组时由
 * {@code ModIntegrations} 挡在 {@code isLoaded} 之后。</p>
 */
public final class ExtendedCraftingTableMatcher implements GridMatcher {

    @Override
    public @Nullable Match match(Level level, List<ItemStack> items, int width, int height) {
        var bounds = GridBounds.ofItems(items, width, height);
        if (bounds.isEmpty()) {
            return null;
        }

        var type = ModRecipeTypes.TABLE.get();
        // 一档一个输入：同档的配方复用同一个 TableCraftingInput，省掉重复的包围盒裁剪。
        var inputsByTier = new HashMap<Integer, TableCraftingInput>();

        for (var holder : level.getRecipeManager().getAllRecipesFor(type)) {
            var recipe = holder.value();
            // 有形状配方的宽高必须与包围盒完全相等（ShapedRecipePattern#matches 的口径），
            // 先比宽高就能跳过绝大多数配方，省掉 matches 内部的图案逐格比较。
            // 无形状配方没有尺寸可预筛，仍走 matches。
            if (recipe instanceof ShapedTableRecipe shaped
                    && (shaped.getWidth() != bounds.width() || shaped.getHeight() != bounds.height())) {
                continue;
            }
            var input = inputsByTier.computeIfAbsent(recipe.getTier(),
                    tier -> TableCraftingInput.of(width, height, items, tier));
            if (recipe.matches(input, level)) {
                // 余料按裁剪后的输入返回，摊回整张 9×9 供装配室逐格回填。
                var remaining = bounds.scatter(recipe.getRemainingItems(input), width, height, ItemStack.EMPTY);
                return new Match(recipe.assemble(input, level.registryAccess()), holder.id(), remaining);
            }
        }
        return null;
    }
}
