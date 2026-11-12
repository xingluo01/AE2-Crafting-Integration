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
     * @param output    产物。已经过 {@code assemble}，数量是本次合成的真实产出
     * @param recipeId  命中的配方 id；邻居的配方类型拿不到 id 时允许为 {@code null}
     * @param remaining 合成后每格剩下的东西（铁桶→空桶、无尽的不消耗催化剂），
     *                  <b>长度与顺序与传进 {@code match} 的 {@code items} 一致</b>；
     *                  没有余料信息时是空列表
     */
    record Match(ItemStack output, @Nullable ResourceLocation recipeId, List<ItemStack> remaining) {

        public Match {
            var sanitized = new java.util.ArrayList<ItemStack>(remaining.size());
            for (var stack : remaining) {
                sanitized.add(stack == null ? ItemStack.EMPTY : stack);
            }
            remaining = List.copyOf(sanitized);
        }

        /** 不关心余料的旧写法。 */
        public Match(ItemStack output, @Nullable ResourceLocation recipeId) {
            this(output, recipeId, List.of());
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
}
