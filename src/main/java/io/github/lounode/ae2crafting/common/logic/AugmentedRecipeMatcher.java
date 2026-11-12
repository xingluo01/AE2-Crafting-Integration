package io.github.lounode.ae2crafting.common.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;

/**
 * 9×9 网格 → 产物。编码终端的产物预览与（以后的）装配室都走这里，保证「预览看到什么」与
 * 「机器做什么」是同一套判定。
 *
 * <p>判定器按注册顺序依次询问，**第一个非 {@code null} 的结果获胜**。内置的原版合成台判定器排在最前，
 * 邻居的 9×9 工作台判定器由 {@code integration/*} 在模组初始化时用 {@link #registerIntegration} 追加。</p>
 *
 * <p><b>为什么原版判定器有 3×3 上限</b>：{@code ShapedRecipePattern#matches} 要求输入宽高与配方
 * 宽高**完全相等**，不是「放得下就匹配」。所以 9×9 全图永远匹配不到 3×3 配方，必须先按非空格
 * 的包围盒裁成紧贴的小图（与原版工作台 {@code CraftingInput.ofPositioned} 同口径）。包围盒超过
 * 3×3 时原版判定器直接放弃，交给后面的邻居判定器。</p>
 */
public final class AugmentedRecipeMatcher {

    /** 原版合成台的尺寸上限。超过它原版配方不可能命中（见类注释）。 */
    public static final int VANILLA_MAX_SIZE = 3;

    private static final List<GridMatcher> MATCHERS = new CopyOnWriteArrayList<>();

    static {
        MATCHERS.add(new VanillaCraftingMatcher());
    }

    private AugmentedRecipeMatcher() {
    }

    /**
     * 追加一个判定器，**只在模组初始化时调用一次**。
     *
     * <p>排在原版判定器之后，所以邻居拿到的只是「原版做不出来」的那部分网格——这正是要的：
     * 一个 3×3 的木板配方不该被工作台适配层按别的规则重新解释。</p>
     */
    public static void registerIntegration(GridMatcher matcher) {
        MATCHERS.add(matcher);
    }

    /** 当前的判定器链，顺序即优先级。测试与排障用。 */
    public static List<GridMatcher> matchers() {
        return List.copyOf(MATCHERS);
    }

    /**
     * 按注册顺序匹配整张网格。
     *
     * @param items 行优先、长度 {@code width * height}；空格是 {@link ItemStack#EMPTY}
     * @return 第一个非 {@code null} 的命中；全都认不出时 {@code null}
     */
    @Nullable
    public static GridMatcher.Match match(Level level, List<ItemStack> items, int width, int height) {
        return match(level, items, width, height, (ItemStack) null);
    }

    /**
     * 带标志工作方块的版本。{@code station} 为 {@code null} 表示玩家没放标志物。
     *
     * <p>顺序不变：原版合成台仍然排第一。所以在一张无尽工作台上摆 3×3 木板图案，得到的仍是
     * 原版木板而不是某个 9×9 配方——工作台首先是个工作台。</p>
     */
    @Nullable
    public static GridMatcher.Match match(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station) {
        return matchWithChain(level, items, width, height, station, MATCHERS);
    }

    /** 指定判定器链的版本。包内可见：单测用它验证顺序与短路，不必污染全局注册表。 */
    @Nullable
    static GridMatcher.Match matchWithChain(Level level, List<ItemStack> items, int width, int height,
            List<GridMatcher> matchers) {
        return matchWithChain(level, items, width, height, null, matchers);
    }

    @Nullable
    static GridMatcher.Match matchWithChain(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station, List<GridMatcher> matchers) {
        for (var matcher : matchers) {
            var match = matcher.match(level, items, width, height, station);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /**
     * 把编码台的 {@code ConfigInventory} 快照转成配方匹配要的物品表。
     *
     * <p>非物品的 {@code AEKey}（流体等）在合成网格里没有意义，按空格处理；数量为 0 的也按空格处理——
     * {@code GenericStack} 允许 0，{@link ItemStack#EMPTY} 不允许「数量 0 的某物品」。</p>
     */
    public static List<ItemStack> toItemStacks(List<@Nullable GenericStack> grid) {
        var items = new ArrayList<ItemStack>(grid.size());
        for (var stack : grid) {
            if (stack == null || stack.amount() <= 0 || !(stack.what() instanceof AEItemKey itemKey)) {
                items.add(ItemStack.EMPTY);
            } else {
                items.add(itemKey.toStack((int) Math.min(Integer.MAX_VALUE, stack.amount())));
            }
        }
        return items;
    }

    /** 原版合成台：裁包围盒 → 查 {@code RecipeType.CRAFTING}。 */
    private static final class VanillaCraftingMatcher implements GridMatcher {

        /**
         * 标志物是特殊工作台时让路：末影/通量合成器、组合合成器、终焉厨锅都是 3×3 或 9×9 的
         * 网格配方，若原版判定器抢先命中，同一个图案会被解释成普通配方。
         */
        @Nullable
        @Override
        public Match match(Level level, List<ItemStack> items, int width, int height,
                @Nullable ItemStack station) {
            if (CraftRules.isSpecialWorkstation(station)) {
                return null;
            }
            return match(level, items, width, height);
        }

        @Nullable
        @Override
        public Match match(Level level, List<ItemStack> items, int width, int height) {
            var bounds = GridBounds.ofItems(items, width, height);
            if (bounds.isEmpty() || bounds.width() > VANILLA_MAX_SIZE || bounds.height() > VANILLA_MAX_SIZE) {
                return null;
            }

            var input = CraftingInput.of(bounds.width(), bounds.height(), bounds.crop(items, width));
            var holder = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
            if (holder == null) {
                return null;
            }
            var recipe = holder.value();
            // 余料按裁剪后的输入返回，这里摊回整张网格：装配室完成一次合成后要逐格回填。
            var remaining = bounds.scatter(recipe.getRemainingItems(input), width, height, ItemStack.EMPTY);
            return new Match(recipe.assemble(input, level.registryAccess()), holder.id(), remaining);
        }
    }
}
