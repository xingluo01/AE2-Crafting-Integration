package io.github.lounode.ae2crafting.integration.avaritia;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import committee.nova.mods.avaritia.init.registry.ModBlocks;

/**
 * 无尽重生的「标志工作方块」→ 工作台档位。
 *
 * <p>四张表格工作台是正方形，边长与 tier 一一对应（{@code ModCraftTier.size}）：</p>
 * <ul>
 *   <li>幽匿合成台 5×5 → tier 1</li>
 *   <li>下界合成台 7×7 → tier 2</li>
 *   <li>末地合成台 9×9 → tier 3</li>
 *   <li>终极合成台 11×11 → tier 4</li>
 * </ul>
 *
 * <p><b>为什么 tier 是 {@code (size - 3) / 2} 而不是 {@code size / 2}</b>：配方数据包里的
 * {@code tier} 取值是 1..4（本模组内置 77 条表格配方里 1/2/3/4 分别有 5/10/9/53 条），
 * 而 {@code ShapedTableCraftingRecipe#getTier()} 在配方没显式写 tier 时按 pattern 尺寸推断：
 * 宽高都 &lt;4 → 1、&lt;6 → 2、&lt;8 → 3、否则 4。两处口径一致指向「第 n 档台做第 n 档配方」，
 * 每档台比该档配方最大尺寸大 2（正好留一圈边，这也是居中填充的由来）。</p>
 *
 * <p>模组自己的 {@code ModCraftContainer#asCraftInput()} 用的是 {@code Math.floorDiv(width, 2)}，
 * 会让四张台整体偏移一位（终极台算出 tier 5，而数据包里没有 tier 5 的配方）。
 * 这里刻意不照抄那个偏移：编码与装配室要的是「放哪张台能做哪档配方」的直觉语义。
 * 该差异记在 {@code docs/ARCHITECTURE.md} 的「无尽重生」一节。</p>
 *
 * <p>类名只允许出现在 {@code integration/} 里：缺这个模组时 {@code ModIntegrations} 不会加载本类，
 * 而 {@code ModBlocks} 也永远不会被初始化。</p>
 */
public final class AvaritiaWorkstations {

    private AvaritiaWorkstations() {
    }

    /**
     * 一张表格工作台。
     *
     * @param size 网格边长（格）
     * @param tier 配方 tier（1..4，与数据包里的 {@code tier} 字段同口径）
     */
    public record Table(int size, int tier) {
    }

    /**
     * 标志物对应的表格工作台；不是表格工作台（含空槽、无尽锻造台、别的模组方块）时返回 {@code null}。
     */
    @Nullable
    public static Table tableOf(@Nullable ItemStack station) {
        if (station == null || station.isEmpty()) {
            return null;
        }
        var item = station.getItem();
        if (item == ModBlocks.sculk_crafting_table.get().asItem()) {
            return new Table(5, 1);
        }
        if (item == ModBlocks.nether_crafting_table.get().asItem()) {
            return new Table(7, 2);
        }
        if (item == ModBlocks.end_crafting_table.get().asItem()) {
            return new Table(9, 3);
        }
        if (item == ModBlocks.extreme_crafting_table.get().asItem()) {
            return new Table(11, 4);
        }
        return null;
    }

    /** 标志物是不是无尽锻造台。锻造不是网格配方，单独一条判定链。 */
    public static boolean isExtremeSmithingTable(@Nullable ItemStack station) {
        return station != null && !station.isEmpty()
                && station.getItem() == ModBlocks.extreme_smithing_table.get().asItem();
    }
}
