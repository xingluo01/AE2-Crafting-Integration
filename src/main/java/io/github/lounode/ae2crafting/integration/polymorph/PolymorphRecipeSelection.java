package io.github.lounode.ae2crafting.integration.polymorph;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.fml.ModList;

import io.github.lounode.ae2crafting.common.logic.RecipeSelection;

/**
 * 多态合成（Polymorph）的选择器：把判定器收集到的候选交给它，由玩家/方块实体已选的配方决定。
 *
 * <p><b>全反射</b>：Polymorph 只在 {@code localRuntime}，编译期不可见。API 入口
 * {@code PolymorphApi.getInstance().getRecipeManager()} 通过类名加载，两个重载按擦除后的
 * 参数类型查找：编码终端用 {@code getPlayerRecipe(menu, type, input, level, player, list)}，
 * 装配室用 {@code getBlockEntityRecipe(type, input, level, blockEntity)}。</p>
 *
 * <p>没装模组时工厂方法直接返回 {@code null}，判定器链回到「第一个命中获胜」，
 * 不会为收集候选付出额外遍历。</p>
 */
public final class PolymorphRecipeSelection implements RecipeSelection {

    private static final String MOD_ID = "polymorph";
    private static final String API_CLASS = "com.illusivesoulworks.polymorph.api.PolymorphApi";

    @Nullable
    private final AbstractContainerMenu menu;
    @Nullable
    private final Player player;
    @Nullable
    private final BlockEntity blockEntity;

    private PolymorphRecipeSelection(@Nullable AbstractContainerMenu menu, @Nullable Player player,
            @Nullable BlockEntity blockEntity) {
        this.menu = menu;
        this.player = player;
        this.blockEntity = blockEntity;
    }

    /** 编码终端：按玩家在 Polymorph 里的选择决定。未装模组返回 {@code null}。 */
    @Nullable
    public static RecipeSelection forMenu(AbstractContainerMenu menu, Player player) {
        return ModList.get().isLoaded(MOD_ID) ? new PolymorphRecipeSelection(menu, player, null) : null;
    }

    /** 装配室：按方块实体自己的 Polymorph 配方数据决定。未装模组返回 {@code null}。 */
    @Nullable
    public static RecipeSelection forBlockEntity(BlockEntity blockEntity) {
        return ModList.get().isLoaded(MOD_ID) ? new PolymorphRecipeSelection(null, null, blockEntity) : null;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <I extends RecipeInput, T extends Recipe<I>> RecipeHolder<T> select(
            RecipeType<T> type, I input, List<RecipeHolder<T>> candidates) {
        try {
            var apiClass = Class.forName(API_CLASS);
            var api = apiClass.getMethod("getInstance").invoke(null);
            if (api == null) {
                return null;
            }
            var manager = apiClass.getMethod("getRecipeManager").invoke(api);
            if (manager == null) {
                return null;
            }
            Object result;
            if (menu != null && player != null) {
                var method = manager.getClass().getMethod("getPlayerRecipe",
                        AbstractContainerMenu.class, RecipeType.class, RecipeInput.class,
                        Level.class, Player.class, List.class);
                result = method.invoke(manager, menu, type, input, player.level(), player, candidates);
            } else if (blockEntity != null && blockEntity.getLevel() != null) {
                var method = manager.getClass().getMethod("getBlockEntityRecipe",
                        RecipeType.class, RecipeInput.class, Level.class, BlockEntity.class);
                result = method.invoke(manager, type, input, blockEntity.getLevel(), blockEntity);
            } else {
                return null;
            }
            return result instanceof Optional<?> optional
                    ? (RecipeHolder<T>) optional.orElse(null)
                    : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // 反射失败一律退回「第一个候选」，不让兼容层把编码/合成打断。
            return null;
        }
    }
}
