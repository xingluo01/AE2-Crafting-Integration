package io.github.lounode.ae2crafting.game;

import java.util.Objects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.menu.locator.MenuLocators;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.item.WirelessAugmentedPatternTerminalHost;
import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;
import io.github.lounode.ae2crafting.registry.ModDataComponents;
import io.github.lounode.ae2crafting.registry.ModItems;

/**
 * 厨锅样板：产物容纳物必须编进样板的独立字段，不能留在 9×9 网格里。
 *
 * <p>取一条真实的农夫乐事厨锅配方，按行优先把原料铺进网格、容器接在最后，再走一遍
 * {@code menu.encode()}。断言两件事：样板带上了容器；网格里的非空格数等于原料数。</p>
 */
@GameTestHolder(AE2CraftingIntegration.MOD_ID)
@PrefixGameTestTemplate(false)
public class CookingPatternEncodeGameTest {

    @GameTest(template = "empty")
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void cookingPotContainerIsEncodedSeparately(GameTestHelper helper) {
        var level = helper.getLevel();
        var pot = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.parse("farmersdelight:cooking_pot")).orElse(null);
        helper.assertTrue(pot != null, "农夫乐事厨锅不存在");
        var type = BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse("farmersdelight:cooking"));
        helper.assertTrue(type != null, "厨锅配方类型不存在");

        Recipe<?> recipe = null;
        var container = ItemStack.EMPTY;
        for (Object entry : level.getRecipeManager().getAllRecipesFor((RecipeType) type)) {
            Recipe<?> candidate = (Recipe<?>) ((RecipeHolder) entry).value();
            var expected = outputContainer(candidate);
            if (!expected.isEmpty() && !candidate.getIngredients().isEmpty()) {
                recipe = candidate;
                container = expected.copy();
                break;
            }
        }
        helper.assertTrue(recipe != null, "没有找到带容器的厨锅配方");
        helper.assertTrue(!container.isEmpty(), "配方容器为空");

        var item = ModItems.WIRELESS_TERMINAL.get();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var locator = MenuLocators.forStack(new ItemStack(item));
        var host = new WirelessAugmentedPatternTerminalHost(item, player, locator, (p, menu) -> {
        });
        var menu = new AugmentedPatternEncodingTermMenu(0, player.getInventory(), host);
        var logic = menu.getLogic();

        logic.getBlankPatternInv().setItemDirect(0, AEItems.BLANK_PATTERN.stack());
        var grid = logic.getEncodedInputInv();
        int slot = 0;
        for (var ingredient : recipe.getIngredients()) {
            var options = ingredient.getItems();
            helper.assertTrue(options.length > 0, "配方原料没有可用物品");
            grid.setStack(slot++, GenericStack.fromItemStack(options[0].copy()));
        }
        grid.setStack(slot, GenericStack.fromItemStack(container));

        menu.getSlot(menu.getWorkstationSlotIndex()).set(new ItemStack(pot));

        menu.encode();

        var pattern = logic.getEncodedPatternInv().getStackInSlot(0);
        helper.assertFalse(pattern.isEmpty(), "编码失败：输出槽没有样板");
        var data = pattern.get(ModDataComponents.AUGMENTED_PATTERN.get());
        helper.assertTrue(data != null, "输出槽里不是增广样板");
        helper.assertTrue(data.container() != null, "产物容纳物没有写进样板");
        long filled = data.grid().stream().filter(Objects::nonNull).count();
        helper.assertTrue(filled == recipe.getIngredients().size(),
                "网格里不该留容器：非空格 " + filled + "，原料 " + recipe.getIngredients().size());
        helper.succeed();
    }

    private static ItemStack outputContainer(Recipe recipe) {
        try {
            return recipe.getClass().getMethod("getOutputContainer").invoke(recipe) instanceof ItemStack stack
                    ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }
}
