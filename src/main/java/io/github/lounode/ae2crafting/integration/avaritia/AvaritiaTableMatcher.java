package io.github.lounode.ae2crafting.integration.avaritia;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import committee.nova.mods.avaritia.api.common.crafting.TierInput;
import committee.nova.mods.avaritia.init.registry.ModRecipeTypes;

import io.github.lounode.ae2crafting.common.logic.GridBounds;
import io.github.lounode.ae2crafting.common.logic.GridMatcher;

/**
 * 无尽（Re-Avaritia）表格工作台配方：{@code avaritia:shaped_table} / {@code avaritia:shapeless_table}。
 *
 * <p><b>tier 由标志物决定，不是由网格猜</b>：{@link TierInput} 继承 {@code CraftingInput} 并在
 * {@code of} 里先裁包围盒，{@code matches} 对 tier 做等值判断（{@code tier == 0} 表示不限档）。
 * 所以「放哪张台」是匹配的输入之一，没放标志物时本判定器直接放弃。</p>
 *
 * <p><b>居中填充</b>：四张台边长 5/7/9/11，编码台固定 9×9。先把非空格裁成包围盒，再以工作台
 * 中心对齐铺进去——玩家把图案摆在编码台的哪个角落都不影响结果，装不进这张台就干脆不匹配
 * （{@link GridBounds#centerIn} 带尺寸检查）。</p>
 *
 * <p>读取顺序注意：原版判定器排在前面，所以在一张无尽台上摆 3×3 木板仍然得到原版木板；
 * 本判定器只接手原版认不出的网格。</p>
 */
public final class AvaritiaTableMatcher implements GridMatcher {

    @Override
    public @Nullable Match match(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station) {
        var table = AvaritiaWorkstations.tableOf(station);
        if (table == null) {
            return null;
        }

        var bounds = GridBounds.ofItems(items, width, height);
        if (bounds.isEmpty() || bounds.width() > table.size() || bounds.height() > table.size()) {
            return null;
        }

        var centered = bounds.centerIn(items, width, table.size(), table.size(), ItemStack.EMPTY);
        var input = TierInput.of(table.size(), table.size(), centered, table.tier());

        var type = ModRecipeTypes.CRAFTING_TABLE_RECIPE.get();
        for (var holder : level.getRecipeManager().getAllRecipesFor(type)) {
            var recipe = holder.value();
            if (recipe.matches(input, level)) {
                return new Match(recipe.assemble(input, level.registryAccess()), holder.id());
            }
        }
        return null;
    }

    /** 不带标志物的四参版本：无法确定 tier，直接放弃，交给链上其它判定器。 */
    @Override
    public @Nullable Match match(Level level, List<ItemStack> items, int width, int height) {
        return null;
    }
}
