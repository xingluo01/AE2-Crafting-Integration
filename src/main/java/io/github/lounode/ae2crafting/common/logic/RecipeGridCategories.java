package io.github.lounode.ae2crafting.common.logic;

import java.util.List;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/**
 * 配方查看器里那些「能在本模组编码台上重建」的配方类别。
 *
 * <p><b>为什么要把类别 UID 写成常量</b>：JEI 的转移按钮是<b>按类别逐个注册</b>的
 * （{@code RecipeTransferManager} 的表键是「容器类 + 类别」），想让它出现在哪一类配方上，
 * 就得在那一类上挂一个处理器。这里把「挂哪些类别、每一类画几行几列、要不要顺带放工作台」
 * 集中成一张表，注册端只负责遍历。</p>
 *
 * <p><b>为什么边长要写死而不是从槽数开方</b>：两家的无形状配方都只给实际用到的格子建槽
 * （{@code if (index < inputs.size())}），7 个材料就是 7 个输入槽，开方开不出来；
 * 有形状配方才铺满整张网格。边长属于类别布局，跟具体配方无关，写死才对。</p>
 *
 * <p><b>边长 ≠ 工作台尺寸</b>：合成拓展的类别与台面同尺寸（3/5/7/9），无尽重生的类别比台面
 * 小一圈（类别 3/5/7/9 对应台面 5/7/9/11——每档台留一圈边正好放得下本档最大图案）。
 * 所以这里记的始终是<b>JEI 画出来的格子数</b>，台面尺寸由 {@link Station} 那边的档位换算负责。</p>
 */
public final class RecipeGridCategories {

    public static final String EXTENDED_CRAFTING = "extendedcrafting";
    public static final String AVARITIA = "avaritia";

    /** 原版合成台。9 格，不需要标志工作方块。 */
    public static final Category VANILLA_CRAFTING =
            Category.square("minecraft", "crafting", 3, Station.NONE);

    /** 本模组认得的全部类别，顺序只影响注册顺序，不影响行为。 */
    public static final List<Category> BUILTIN = List.of(
            VANILLA_CRAFTING,
            // 合成拓展：四档台，类别与台面同尺寸。
            Category.square(EXTENDED_CRAFTING, "basic_crafting", 3, Station.EXTENDED_CRAFTING),
            Category.square(EXTENDED_CRAFTING, "advanced_crafting", 5, Station.EXTENDED_CRAFTING),
            Category.square(EXTENDED_CRAFTING, "elite_crafting", 7, Station.EXTENDED_CRAFTING),
            Category.square(EXTENDED_CRAFTING, "ultimate_crafting", 9, Station.EXTENDED_CRAFTING),
            // 无尽重生：类别比台面小一圈，边长只决定画几格。
            Category.square(AVARITIA, "sculk_craft", 3, Station.AVARITIA_TABLE),
            Category.square(AVARITIA, "nether_craft", 5, Station.AVARITIA_TABLE),
            Category.square(AVARITIA, "end_craft", 7, Station.AVARITIA_TABLE),
            Category.square(AVARITIA, "extreme_craft", 9, Station.AVARITIA_TABLE),
            // 无尽锻造不是网格：五格十字，同样是「哪张台」由标志物决定。
            Category.cross(AVARITIA, "extreme_smithing", Station.AVARITIA_SMITHING));

    /** 标志工作方块怎么来。 */
    public enum Station {
        /** 原版合成：不动标志物槽，玩家原来放什么就还是什么。 */
        NONE,
        /** 按配方 tier 放对应档位的合成拓展台。 */
        EXTENDED_CRAFTING,
        /** 按配方 tier 放对应档位的无尽重生台。 */
        AVARITIA_TABLE,
        /** 无尽锻造台，只有一张。 */
        AVARITIA_SMITHING
    }

    /**
     * 一个配方类别的布局。
     *
     * @param id      配方查看器类别 UID
     * @param side    类别画出来的网格边长（十字布局记 3，仅用于「放不放得下」的判断）
     * @param cross   是否十字五格
     * @param station 转移时标志物槽放什么
     */
    public record Category(ResourceLocation id, int side, boolean cross, Station station) {

        public static Category square(String namespace, String path, int side, Station station) {
            return new Category(ResourceLocation.fromNamespaceAndPath(namespace, path), side, false, station);
        }

        public static Category cross(String namespace, String path, Station station) {
            return new Category(ResourceLocation.fromNamespaceAndPath(namespace, path), 3, true, station);
        }

        /** 这一类在 9×9 编码台上占的格子数：方形是边长的平方，十字固定 5。 */
        public int slotCount() {
            return cross ? 5 : side * side;
        }
    }

    /**
     * 只按边长推档位——配方自己没写 tier（{@code getTier() == 0}，表示不限档）时用它兜底。
     *
     * <p>两家的换算恰好一致：合成拓展 3/5/7/9 → 1/2/3/4；无尽重生的类别比台面小一圈，
     * 3/5/7/9 也正好对应 1/2/3/4 档台。所以能共用一条公式。</p>
     */
    public static int tierFromSide(int side) {
        return Math.max(1, (side - 1) / 2);
    }

    /** 按 UID 找类别。找不到说明这一类的配方本模组不支持。 */
    public static Optional<Category> byId(ResourceLocation id) {
        return BUILTIN.stream().filter(category -> category.id().equals(id)).findFirst();
    }

    private RecipeGridCategories() {
    }
}
