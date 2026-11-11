package io.github.lounode.ae2crafting.integration.avaritia;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import committee.nova.mods.avaritia.common.crafting.input.ExtremeSmithingRecipeInput;
import committee.nova.mods.avaritia.init.registry.ModRecipeTypes;

import io.github.lounode.ae2crafting.common.logic.GridBounds;
import io.github.lounode.ae2crafting.common.logic.GridMatcher;

/**
 * 无尽锻造（{@code avaritia:extreme_smithing}）判定器。
 *
 * <p>锻造不是网格配方：{@link ExtremeSmithingRecipeInput} 是五格——模板、基底、三个添加物，
 * 且 {@code matches} 要求五格<b>全部非空</b>（三个添加物各自匹配同一个 {@code additions} 试剂）。
 * 模组自己的界面把五格摆成十字（{@code ExtremeSmithingMenu#createInputSlotDefinitions}）：
 * 模板在左、基底居中、添加物在右/上/下。</p>
 *
 * <pre>
 *       [添加]
 * [模板] [基底] [添加]
 *       [添加]
 * </pre>
 *
 * <p>编码台是 9×9，所以先在网格里取非空格的包围盒，按「优先居中」把它居中放进一个 3×3 的
 * 概念区域，再按上面的十字读取。四个角必须为空，否则视为「不是十字」而不是硬凑一个锻造配方——
 * 宁可不出产物，也不要把一张 3×3 表格配方误编成锻造样板。</p>
 */
public final class AvaritiaSmithingMatcher implements GridMatcher {

    /** 十字在 3×3 概念区域里的下标（行优先）。 */
    private static final int TOP = 1;
    private static final int LEFT = 3;
    private static final int CENTER = 4;
    private static final int RIGHT = 5;
    private static final int BOTTOM = 7;
    private static final int TOP_LEFT = 0;
    private static final int TOP_RIGHT = 2;
    private static final int BOTTOM_LEFT = 6;
    private static final int BOTTOM_RIGHT = 8;

    @Override
    public @Nullable Match match(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station) {
        if (!AvaritiaWorkstations.isExtremeSmithingTable(station)) {
            return null;
        }

        var bounds = GridBounds.ofItems(items, width, height);
        if (bounds.isEmpty() || bounds.width() > 3 || bounds.height() > 3) {
            return null;
        }

        var centered = bounds.centerIn(items, width, 3, 3, ItemStack.EMPTY);
        if (!centered.get(TOP_LEFT).isEmpty() || !centered.get(TOP_RIGHT).isEmpty()
                || !centered.get(BOTTOM_LEFT).isEmpty() || !centered.get(BOTTOM_RIGHT).isEmpty()) {
            return null;
        }

        var template = centered.get(LEFT);
        var base = centered.get(CENTER);
        var addition1 = centered.get(RIGHT);
        var addition2 = centered.get(TOP);
        var addition3 = centered.get(BOTTOM);
        if (template.isEmpty() || base.isEmpty() || addition1.isEmpty()
                || addition2.isEmpty() || addition3.isEmpty()) {
            return null;
        }

        var input = new ExtremeSmithingRecipeInput(template, base, addition1, addition2, addition3);
        var type = ModRecipeTypes.EXTREME_SMITHING_RECIPE.get();
        for (var holder : level.getRecipeManager().getAllRecipesFor(type)) {
            var recipe = holder.value();
            if (recipe.matches(input, level)) {
                return new Match(recipe.assemble(input, level.registryAccess()), holder.id());
            }
        }
        return null;
    }

    /** 不带标志物的四参版本：锻造台是必需的，直接放弃。 */
    @Override
    public @Nullable Match match(Level level, List<ItemStack> items, int width, int height) {
        return null;
    }
}
