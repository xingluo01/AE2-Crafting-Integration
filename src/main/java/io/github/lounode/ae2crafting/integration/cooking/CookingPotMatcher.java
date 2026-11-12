package io.github.lounode.ae2crafting.integration.cooking;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
import io.github.lounode.ae2crafting.common.logic.CraftRules;
import io.github.lounode.ae2crafting.common.logic.GridMatcher;
import io.github.lounode.ae2crafting.common.logic.RecipeSelection;

/**
 * 烹饪锅类配方（农夫乐事本体、无尽乐事终焉厨锅）：81 格原料 + 产物容纳物。
 *
 * <p>两家的配方形状几乎一样：{@code INPUT_SLOTS = 81}、{@code Recipe<RecipeWrapper>}、
 * 带一个 {@code getOutputContainer()}。差别只有配方类型 id 与锅的物品 id，所以这里参数化，
 * 两个集成各 new 一个实例挂到判定器链上。</p>
 *
 * <p><b>为什么全用反射</b>：两家都只在 {@code localRuntime}，编译期不可见，按集成红线本模组
 * 不引用它们的类型。配方类型从 {@code BuiltInRegistries.RECIPE_TYPE} 按 id 查，容器 getter
 * 用反射读（按配方类缓存 {@link Method}，配方几十上百条时省掉重复查找）；
 * {@code matches} / {@code assemble} 走 {@link Recipe} 接口本身。</p>
 *
 * <p>按约定<b>容器顺位接在最后一个原材料之后</b>：行优先收集非空格，最后一件是产物容器，
 * 其余按原顺序当原料。容器必须匹配配方的 {@code getOutputContainer()}，否则配方不成立。
 * 匹配用的 {@code RecipeWrapper} 只放原料：配方用 {@code RecipeMatcher.findMatches} 比对
 * 全部非空格，多放一件容器会让它必定失败。</p>
 */
public class CookingPotMatcher implements GridMatcher {

    private static final int INPUT_SLOTS = 81;
    /** 一次最多收集这么多候选，避免冲突配方特别多时把列表撑爆。 */
    private static final int MAX_CANDIDATES = 16;
    private static final String CONTAINER_GETTER = "getOutputContainer";
    /** 按配方类缓存反射结果：配方几十上百条时，每次匹配都 getMethod 会成为明显开销。 */
    private static final Map<Class<?>, Optional<Method>> CONTAINER_GETTERS = new ConcurrentHashMap<>();

    private final ResourceLocation recipeTypeId;
    private final ResourceLocation stationItemId;

    /**
     * @param recipeTypeId  配方类型 id，例如 {@code farmersdelight:cooking}
     * @param stationItemId 锅的物品 id，例如 {@code farmersdelight:cooking_pot}
     */
    public CookingPotMatcher(String recipeTypeId, String stationItemId) {
        this.recipeTypeId = ResourceLocation.parse(recipeTypeId);
        this.stationItemId = ResourceLocation.parse(stationItemId);
    }

    @Nullable
    @Override
    public Match match(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station) {
        var matches = matchAll(level, items, width, height, station, null);
        return matches.isEmpty() ? null : matches.getFirst();
    }

    /**
     * 同一锅配方类型下的全部命中。多模组往同一个 {@code cooking} 类型里塞相同原料的配方时，
     * 编码终端据此提供候选切换；选择器在这里不参与（锅的候选由终端滚轮决定）。
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<Match> matchAll(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station, @Nullable RecipeSelection selection) {
        if (!stationItemId.equals(CraftRules.itemKey(station))) {
            return List.of();
        }
        var compact = new ArrayList<ItemStack>(width * height);
        for (var item : items) {
            if (item != null && !item.isEmpty()) {
                compact.add(item);
            }
        }
        if (compact.size() < 2) {
            return List.of();
        }
        var container = compact.get(compact.size() - 1);
        var ingredients = compact.subList(0, compact.size() - 1);

        var type = BuiltInRegistries.RECIPE_TYPE.get(recipeTypeId);
        if (type == null) {
            return List.of();
        }
        var handler = new ItemStackHandler(INPUT_SLOTS);
        for (int i = 0; i < Math.min(INPUT_SLOTS, ingredients.size()); i++) {
            handler.setStackInSlot(i, ingredients.get(i).copy());
        }
        var wrapper = new RecipeWrapper(handler);

        // raw 调用让返回值退化成元素为 Object 的集合，逐个转型；这里不能写邻居的配方类型。
        var holders = new ArrayList<RecipeHolder>();
        for (Object entry : level.getRecipeManager().getAllRecipesFor((RecipeType) type)) {
            RecipeHolder holder = (RecipeHolder) entry;
            Recipe recipe = (Recipe) holder.value();
            if (!containerMatches(recipe, container) || !recipe.matches(wrapper, level)) {
                continue;
            }
            holders.add(holder);
            if (holders.size() >= MAX_CANDIDATES) {
                break;
            }
        }
        if (holders.isEmpty()) {
            return List.of();
        }
        var matches = new ArrayList<Match>(holders.size());
        for (var holder : holders) {
            var recipe = (Recipe) holder.value();
            matches.add(Match.special(recipe.assemble(wrapper, level.registryAccess()), holder.id(),
                    List.of(), CraftKind.COOKING_POT, compact.size()));
        }
        return matches;
    }

    private static boolean containerMatches(Recipe recipe, ItemStack container) {
        var expected = containerOf(recipe);
        // 配方没声明容纳物时不限制；声明了就必须是同一件物品。
        // 不读 getContainerOverride：锅的方块实体从不调用它，拿它当备选会放行本该被拒的容器。
        return expected.isEmpty() || expected.getItem() == container.getItem();
    }

    /** 反射读邻居配方的容器 getter；方法不存在或抛错都按「不要求容器」处理，不让判定器崩。 */
    private static ItemStack containerOf(Recipe recipe) {
        var method = CONTAINER_GETTERS.computeIfAbsent(recipe.getClass(), type -> {
            try {
                return Optional.of(type.getMethod(CONTAINER_GETTER));
            } catch (NoSuchMethodException e) {
                return Optional.empty();
            }
        }).orElse(null);
        if (method == null) {
            return ItemStack.EMPTY;
        }
        try {
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
