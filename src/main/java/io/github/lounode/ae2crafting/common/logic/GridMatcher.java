package io.github.lounode.ae2crafting.common.logic;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * 一种「9×9 网格里的这批物品能做出什么」的判定方式。
 *
 * <p>做成接口而不是一堆 if：原版合成台只是其中一种尺寸上限（3×3）。合成拓展工作台、无尽工作台各有
 * 自己的配方类型与输入容器，它们由 {@code integration/<邻居>} 实现本接口并在模组初始化时注册，
 * 这样总是加载的类里不会出现任何邻居的类型名（见 {@code ARCHITECTURE.md} 红线「集成隔离」）。</p>
 *
 * <p>实现方约定：<b>不要修改 {@code items}</b>，拿到的列表是调用方快照；不认得这种网格就返回
 * {@code null} 让下一个判定器接手，不要抛异常。</p>
 */
public interface GridMatcher {

    /**
     * 一次命中的结果。
     *
     * @param output         产物。已经过 {@code assemble}，数量是本次合成的真实产出
     * @param recipeId       命中的配方 id；邻居的配方类型拿不到 id 时允许为 {@code null}
     * @param remaining      合成后每格剩下的东西（铁桶→空桶、无尽的不消耗催化剂），
     *                       <b>长度与顺序与传进 {@code match} 的 {@code items} 一致</b>；
     *                       没有余料信息时是空列表
     * @param kind           命中的机器类别；装配室据此决定速度折减与输入槽位校验。
     *                       普通网格合成用 {@link CraftKind#NORMAL}
     * @param requiredInputs 这条配方需要的输入物品数；只有组合合成器会用到（校验基座数量），
     *                       其余判定器填 0
     */
    record Match(ItemStack output, @Nullable ResourceLocation recipeId, List<ItemStack> remaining,
            CraftKind kind, int requiredInputs) {

        public Match {
            var sanitized = new java.util.ArrayList<ItemStack>(remaining.size());
            for (var stack : remaining) {
                sanitized.add(stack == null ? ItemStack.EMPTY : stack);
            }
            remaining = List.copyOf(sanitized);
        }

        /** 普通网格合成的写法，兼容既有判定器。 */
        public Match(ItemStack output, @Nullable ResourceLocation recipeId, List<ItemStack> remaining) {
            this(output, recipeId, remaining, CraftKind.NORMAL, 0);
        }

        /** 不关心余料的旧写法。 */
        public Match(ItemStack output, @Nullable ResourceLocation recipeId) {
            this(output, recipeId, List.of());
        }

        /** 特殊工作台的写法：带类别与所需输入数。 */
        public static Match special(ItemStack output, @Nullable ResourceLocation recipeId,
                List<ItemStack> remaining, CraftKind kind, int requiredInputs) {
            return new Match(output, recipeId, remaining, kind, requiredInputs);
        }
    }

    /**
     * @param level  服务端世界，取 {@code RecipeManager} 用
     * @param items  行优先的整张网格，长度 {@code width * height}，空格是 {@link ItemStack#EMPTY}
     * @param width  网格宽
     * @param height 网格高
     * @return 命中结果；不认得返回 {@code null}
     */
    @Nullable
    Match match(Level level, List<ItemStack> items, int width, int height);

    /**
     * 带「标志工作方块」的版本：{@code station} 是编码终端标志物槽里的那一个物品。
     *
     * <p>无尽重生的配方带 tier（工作台档位），而 tier 只能由<b>玩家放了哪张台</b>决定，网格本身
     * 看不出来。所以判定器需要这个上下文。默认实现直接忽略它并转给四参版本——原版合成台与
     * 合成拓展都不依赖工作台，保持零改动。</p>
     *
     * <p>约定：{@code station} 为 {@code null} 或空栈表示「玩家没放标志物」。需要标志物的判定器
     * 此时必须返回 {@code null}，而不是猜一个档位。</p>
     */
    @Nullable
    default Match match(Level level, List<ItemStack> items, int width, int height, @Nullable ItemStack station) {
        return match(level, items, width, height);
    }

    /**
     * 带「多候选选择器」的版本。
     *
     * <p>默认实现忽略选择器并转给五参版本——只有真正会收集多条候选的判定器（目前是原版合成台）
     * 才需要覆写它；其余判定器本来就按机器类型短路，同一类型下命中多条的情况很少。</p>
     */
    @Nullable
    default Match match(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station, @Nullable RecipeSelection selection) {
        return match(level, items, width, height, station);
    }

    /**
     * 同一判定器内命中的全部候选，按优先级排列。
     *
     * <p>默认只有一条（就是 {@link #match} 的结果）。会命中多条的判定器（原版合成、厨锅）
     * 覆写它，供编码终端做「候选切换」；选择器返回的候选会被提到第一位，其余保持原顺序。</p>
     */
    default List<Match> matchAll(Level level, List<ItemStack> items, int width, int height,
            @Nullable ItemStack station, @Nullable RecipeSelection selection) {
        var match = match(level, items, width, height, station, selection);
        return match == null ? List.of() : List.of(match);
    }
}
