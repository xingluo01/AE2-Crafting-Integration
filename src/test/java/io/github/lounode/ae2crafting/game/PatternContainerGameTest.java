package io.github.lounode.ae2crafting.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.pattern.AugmentedPattern;
import io.github.lounode.ae2crafting.registry.ModDataComponents;
import io.github.lounode.ae2crafting.registry.ModItems;

/**
 * 产物容纳物：编进样板、每次消耗一份，但不占 9×9 网格、也不铺进装配室槽位。
 *
 * <p>重点验三件事：容器是不是真的没混进 {@code grid}；存盘再读回来还在不在；解码后是不是
 * 作为 {@code getInputs()} 的最后一项（AE2 按这个顺序给 {@code pushPattern} 的 table 装料）。</p>
 */
@GameTestHolder(AE2CraftingIntegration.MOD_ID)
@PrefixGameTestTemplate(false)
public class PatternContainerGameTest {

    @GameTest(template = "empty")
    public void containerIsStoredSeparatelyAndDecodesAsLastInput(GameTestHelper helper) {
        var level = helper.getLevel();
        var registries = level.registryAccess();
        var bowl = GenericStack.fromItemStack(new ItemStack(Items.BOWL));
        var output = GenericStack.fromItemStack(new ItemStack(Items.IRON_BLOCK));

        var grid = new ArrayList<GenericStack>(81);
        for (int i = 0; i < 81; i++) {
            grid.add(null);
        }
        grid.set(0, GenericStack.fromItemStack(new ItemStack(Items.IRON_INGOT)));
        grid.set(1, GenericStack.fromItemStack(new ItemStack(Items.GOLD_INGOT)));

        var stack = new ItemStack(ModItems.AUGMENTED_CRAFTING_PATTERN.get());
        AugmentedPattern.encode(stack, 9, 9, grid, List.of(output), null, false, bowl);

        // 存盘往返：容器走的是组件 codec 的可选字段，老样板缺这一项时必须是 null。
        var restored = ItemStack.parseOptional(registries, (CompoundTag) stack.save(registries));
        var data = restored.get(ModDataComponents.AUGMENTED_PATTERN.get());
        helper.assertTrue(data != null, "样板组件没读回来");
        helper.assertTrue(data.container() != null
                        && data.container().what().equals(bowl.what()),
                "产物容纳物没写进样板或读回来变了");
        helper.assertTrue(data.grid().stream().filter(Objects::nonNull).count() == 2,
                "容纳物不该占 9x9 网格的一格");

        var details = new AugmentedPattern(AEItemKey.of(restored), level);
        helper.assertTrue(details.getContainer() != null, "解码后拿不到容纳物");
        helper.assertTrue(details.getInputs().length == 3,
                "应为 2 个网格输入 + 1 个容纳物，实际 " + details.getInputs().length);
        helper.assertTrue(details.getInputs()[2].isValid(bowl.what(), level),
                "最后一个输入不是容纳物");
        helper.succeed();
    }
}
