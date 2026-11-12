package io.github.lounode.ae2crafting.integration.avaritiadelight;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import io.github.lounode.ae2crafting.common.logic.CraftKind;
import io.github.lounode.ae2crafting.common.logic.GridMatcher;

/**
 * 无尽乐事终焉厨锅：81 格原料 + 产物容器。
 *
 * <p><b>为什么全用反射</b>：无尽乐事只在 {@code localRuntime}，编译期不可见，按集成红线本模组
 * 不引用它的类型。配方类型从 {@code BuiltInRegistries.RECIPE_TYPE} 按 id 查，容器相关的两个
 * getter 用反射读；{@code matches} / {@code assemble} 走 {@link Recipe} 接口本身，不需要邻居类。</p>
 *
 * <p>锅的配方有 81 个输入槽，正好是编码台大小。按约定<b>容器顺位接在最后一个原材料之后</b>：
 * 行优先收集非空格，最后一件是产物容纳物，其余按原顺序当原料。它必须匹配配方的
 * {@code getOutputContainer()}（锅的 {@code isContainerValid} 同口径），否则配方不成立——这就是
 * 「把产物容器纳入配方计算」。匹配用的 {@code RecipeWrapper} 只放原料：配方的 {@code matches}
 * 只扫前 81 槽，容器在锅的 UI 里本来就是独立的 {@code INDEX_CONTAINER = 82} 槽。</p>
 *
 * <p>编码时这一格会被摘出来写进样板的独立字段，作为最后一个输入参与备料与消耗，
 * 但不会出现在装配室的 9×9 槽位里。</p>
 */
public final class ExtremeCookingPotMatcher implements GridMatcher {

    private static final ResourceLocation RECIPE_TYPE =
            ResourceLocation.fromNamespaceAndPath("avaritia_delight", "extreme_cooking");
    private static final int INPUT_SLOTS = 81;
    private static final String CONTAINER_GETTER = "getOutputContainer";

    @Nullable
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Match match(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station) {
        if (!AvaritiaDelightMachines.isExtremeCookingPot(station)) {
            return null;
        }
        var compact = new ArrayList<ItemStack>(width * height);
        for (var item : items) {
            if (item != null && !item.isEmpty()) {
                compact.add(item);
            }
        }
        if (compact.size() < 2) {
            return null;
        }
        var container = compact.get(compact.size() - 1);
        var ingredients = compact.subList(0, compact.size() - 1);

        var type = BuiltInRegistries.RECIPE_TYPE.get(RECIPE_TYPE);
        if (type == null) {
            return null;
        }
        var handler = new ItemStackHandler(INPUT_SLOTS);
        for (int i = 0; i < Math.min(INPUT_SLOTS, ingredients.size()); i++) {
            handler.setStackInSlot(i, ingredients.get(i).copy());
        }
        var wrapper = new RecipeWrapper(handler);

        // raw 调用让返回值退化成元素为 Object 的集合，逐个转型；这里不能写邻居的配方类型。
        for (Object entry : level.getRecipeManager().getAllRecipesFor((RecipeType) type)) {
            RecipeHolder holder = (RecipeHolder) entry;
            Recipe recipe = (Recipe) holder.value();
            if (!containerMatches(recipe, container) || !recipe.matches(wrapper, level)) {
                continue;
            }
            return Match.special(recipe.assemble(wrapper, level.registryAccess()), holder.id(),
                    List.of(), CraftKind.COOKING_POT, compact.size());
        }
        return null;
    }

    private static boolean containerMatches(Recipe recipe, ItemStack container) {
        var expected = containerOf(recipe, CONTAINER_GETTER);
        // 配方没声明容纳物时不限制；声明了就必须是同一件物品。
        // 不读 getContainerOverride：锅的方块实体从不调用它，它只是 JSON 显式参数的原样副本，
        // 拿它当备选会放行本该被拒的容器。
        return expected.isEmpty() || expected.getItem() == container.getItem();
    }

    /** 反射读邻居配方的容器 getter；方法不存在或抛错都按「不要求容器」处理，不让判定器崩。 */
    private static ItemStack containerOf(Recipe recipe, String name) {
        try {
            var method = recipe.getClass().getMethod(name);
            return method.invoke(recipe) instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    @Nullable
    @Override
    public Match match(Level level, List<ItemStack> items, int width, int height) {
        return null;
    }
}
