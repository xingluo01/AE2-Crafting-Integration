package io.github.lounode.ae2crafting.common.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import io.github.lounode.ae2crafting.common.logic.RecipeGridCategories.Category;

/**
 * 类别白名单的形状约束。
 *
 * <p>这里锁的是「注册端能安全地按这张表铺格」：边长不超过 9×9、十字类别只有无尽锻造一个、
 * UID 能反查。具体 UID 拼写由 {@code StyleDocumentTest} 之外的集成测试兜不住，
 * 只能靠和反编译的模组源码逐字对照，所以把它写成常量表而不是散落在注册代码里。</p>
 */
class RecipeGridCategoriesTest {

    @Test
    void tierFromSideMatchesBothModsTableLadders() {
        // 合成拓展与无尽重生的 JEI 类别边长都是 3/5/7/9，对应 1..4 档台。
        assertEquals(1, RecipeGridCategories.tierFromSide(3));
        assertEquals(2, RecipeGridCategories.tierFromSide(5));
        assertEquals(3, RecipeGridCategories.tierFromSide(7));
        assertEquals(4, RecipeGridCategories.tierFromSide(9));
    }

    @Test
    void everyBuiltinCategoryFitsTheNineByNineGrid() {
        for (Category category : RecipeGridCategories.BUILTIN) {
            assertTrue(category.side() <= 9,
                    category.id() + " 的边长是 " + category.side() + "，超出 9x9 编码台");
        }
    }

    @Test
    void smithingIsTheOnlyCrossCategory() {
        var crosses = RecipeGridCategories.BUILTIN.stream().filter(Category::cross).toList();

        assertEquals(1, crosses.size(), "十字布局只应有无尽锻造一类");
        assertEquals(5, crosses.getFirst().slotCount());
        assertEquals(RecipeGridCategories.AVARITIA, crosses.getFirst().id().getNamespace());
    }

    @Test
    void squareCategoriesCountSlotsAsSideSquared() {
        for (Category category : RecipeGridCategories.BUILTIN) {
            if (!category.cross()) {
                assertEquals(category.side() * category.side(), category.slotCount());
            }
        }
    }

    @Test
    void byIdFindsTheVanillaCraftingCategory() {
        assertEquals(Optional.of(RecipeGridCategories.VANILLA_CRAFTING),
                RecipeGridCategories.byId(ResourceLocation.withDefaultNamespace("crafting")));
    }

    @Test
    void byIdIgnoresCategoriesWeCannotEncode() {
        assertTrue(RecipeGridCategories.byId(
                ResourceLocation.fromNamespaceAndPath("ae2", "inscriber")).isEmpty());
    }
}
