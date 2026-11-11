package io.github.lounode.ae2crafting.client.integration.jei;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.logic.RecipeGridCategories;
import io.github.lounode.ae2crafting.common.logic.RecipeGridCategories.Category;

/**
 * JEI 侧的唯一入口：只干一件事——给 9×9 编码终端挂上配方转移按钮。
 *
 * <p>AE2 的 JEI 集成是独立模组（{@code ae2-jei-integration}），它按自己的终端类型注册处理器，
 * 对本模组自定义的菜单一无所知，所以这里必须自报家门。插件 UID 用本模组命名空间，
 * 不与 {@code ae2:core} 撞车。</p>
 *
 * <p><b>按 UID 反查类别，而不是直接引用邻居的类别类</b>：{@code IJeiHelpers#getRecipeType}
 * 查的是「这一次真的注册了哪些类别」。邻居把某类配方在配置里关掉、或版本改名时，
 * 查不到就安静跳过，不会因为一个 {@code static final} 常量引用而抛 {@code NoClassDefFoundError}，
 * 也不会出现「按钮挂在未注册类别上」的悬空注册。</p>
 */
@JeiPlugin
public final class AugmentedPatternTerminalJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(AE2CraftingIntegration.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        IJeiHelpers helpers = registration.getJeiHelpers();
        for (Category category : RecipeGridCategories.BUILTIN) {
            helpers.getRecipeType(category.id())
                    .ifPresent(type -> register(registration, helpers, category, type));
        }
    }

    /** 泛型擦除后的登记：类别对象来自 JEI 自己，配方类型对得上，这里只是把通配符收窄。 */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void register(IRecipeTransferRegistration registration, IJeiHelpers helpers,
            Category category, RecipeType<?> type) {
        var handler = new AugmentedEncodeTransferHandler(
                (RecipeType) type, category, registration.getTransferHelper(),
                helpers.getIngredientVisibility());
        registration.addRecipeTransferHandler(handler, (RecipeType) type);
    }
}
