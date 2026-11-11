package io.github.lounode.ae2crafting.client.integration.jei;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEItemKey;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.me.common.GridInventoryEntry;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.runtime.IIngredientVisibility;

import io.github.lounode.ae2crafting.common.logic.GridPlacement;
import io.github.lounode.ae2crafting.common.logic.RecipeGridCategories;
import io.github.lounode.ae2crafting.common.logic.RecipeGridCategories.Category;
import io.github.lounode.ae2crafting.common.menu.AugmentedPatternEncodingTermMenu;

/**
 * 把配方查看器里的配方搬进 9×9 编码台的那个「+」按钮。
 *
 * <p><b>为什么必须自己写，不能复用 AE2 的</b>：JEI 的
 * {@code RecipeTransferManager} 把处理器存成
 * {@code ImmutableTable<Class<? extends AbstractContainerMenu>, RecipeType<?>, handler>}，
 * 查表用的是 {@code container.getClass()} <b>精确相等</b>。AE2 的
 * {@code EncodePatternTransferHandler} 注册时绑的是 {@code PatternEncodingTermMenu.class}，
 * 而本终端继承的是 {@code MEStorageMenu}——两者没有继承关系，查不到就<b>不画按钮</b>。
 * 换句话说，按钮缺失不是权限或界面问题，而是「容器类没登记」。</p>
 *
 * <p><b>为什么按类别各注册一个实例，而不是一个 universal</b>：universal 处理器挂在一个
 * 「万能」配方类型上，对所有类别都会显示按钮——包括熔炉、酿造台这些本终端根本没有的模式。
 * 本终端只有 9×9 合成一种能力，只该在这些类别上出现。按类别注册时 JEI 只在对应页签询问，
 * 天然不越界。</p>
 *
 * <p><b>为什么自己发包，不用 JEI 的默认转移</b>：默认实现是对客户端 {@code Slot#set}，
 * 而本终端的 81 格是 AE2 的 {@code FakeSlot}，客户端写入只改本地副本，服务端毫不知情。
 * AE2 自己的处理办法是向服务端发 {@link InventoryActionPacket}（{@code SET_FILTER}），
 * 由 {@code AEBaseMenu#setFilter} 落到 {@code ConfigInventory} 上——这里照抄同一条通道。</p>
 */
public final class AugmentedEncodeTransferHandler
        implements IRecipeTransferHandler<AugmentedPatternEncodingTermMenu, Object> {

    /** 十字五格（无尽锻造）的输入数：模板 + 基底 + 三个添加物。 */
    private static final int CROSS_SLOTS = 5;
    private static final String TOO_LARGE = "gui.ae2_crafting_integration.jei.recipe_too_large";
    private static final String INCOMPLETE = "gui.ae2_crafting_integration.jei.incomplete_recipe";

    private final RecipeType<Object> recipeType;
    private final Category category;
    private final IRecipeTransferHandlerHelper helper;
    private final IIngredientVisibility visibility;

    public AugmentedEncodeTransferHandler(RecipeType<Object> recipeType, Category category,
            IRecipeTransferHandlerHelper helper, IIngredientVisibility visibility) {
        this.recipeType = recipeType;
        this.category = category;
        this.helper = helper;
        this.visibility = visibility;
    }

    @Override
    public Class<? extends AugmentedPatternEncodingTermMenu> getContainerClass() {
        return AugmentedPatternEncodingTermMenu.class;
    }

    @Override
    public Optional<MenuType<AugmentedPatternEncodingTermMenu>> getMenuType() {
        return Optional.of(AugmentedPatternEncodingTermMenu.TYPE);
    }

    @Override
    public RecipeType<Object> getRecipeType() {
        return recipeType;
    }

    /**
     * JEI 19.56 起的新入口：老的六参重载已标 {@code forRemoval}，JEI 会优先调这一版。
     * 这里只是把上下文拆开，真正的活还在下面那个方法里——两版共用同一段逻辑，
     * 免得将来只改一边、留一条「新版能用旧版不能用」的岔路。
     */
    @Override
    public @Nullable IRecipeTransferError transferRecipe(
            IRecipeTransferContext<Object, AugmentedPatternEncodingTermMenu> context, boolean doTransfer) {
        return transferRecipe(context.getContainer(), context.getRecipe(), context.getRecipeSlots(),
                context.getPlayer(), context.isMaxTransfer(), doTransfer);
    }

    @SuppressWarnings("removal")
    @Override
    public @Nullable IRecipeTransferError transferRecipe(
            AugmentedPatternEncodingTermMenu menu,
            Object recipeBase,
            IRecipeSlotsView slotsView,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {

        int gridWidth = AugmentedPatternEncodingTermMenu.GRID_WIDTH;
        int gridHeight = AugmentedPatternEncodingTermMenu.GRID_HEIGHT;

        int[] placement;
        try {
            placement = category.cross()
                    ? GridPlacement.crossCentered(gridWidth, gridHeight)
                    : GridPlacement.squareCentered(category.side(), gridWidth, gridHeight);
        } catch (IllegalArgumentException e) {
            // 目前白名单里最大 9×9，走不到这里；将来接 11×11 的类别时这就是那句提示。
            return helper.createUserErrorWithTooltip(Component.translatable(TOO_LARGE));
        }

        List<IRecipeSlotView> inputs = slotsView.getSlotViews(RecipeIngredientRole.INPUT);
        if (inputs.size() > category.slotCount()) {
            // 类别布局变了（槽数比这套布局多）。宁可报内部错误，也不要按错位的顺序塞材料。
            return helper.createInternalError();
        }
        if (category.cross() && inputs.size() < CROSS_SLOTS) {
            return helper.createUserErrorWithTooltip(Component.translatable(INCOMPLETE));
        }

        if (!doTransfer) {
            // 只是画按钮：布局能放下就返回 null（正常态），放不下上面已经报错了。
            return null;
        }

        Set<AEItemKey> stock = networkStock(menu);
        int[] gridSlots = menu.getGridSlotIndices();
        for (int cell = 0; cell < placement.length; cell++) {
            int source = placement[cell];
            ItemStack stack = source >= 0 && source < inputs.size()
                    ? pick(inputs.get(source), stock)
                    : ItemStack.EMPTY;
            // 空格也要发：转移是「覆盖」，不发就留着上一次的残留。
            sendSetFilter(gridSlots[cell], stack);
        }

        ItemStack station = JeiTransferStations.station(
                category.station(),
                JeiTransferStations.recipeTier(recipeBase),
                RecipeGridCategories.tierFromSide(category.side()));
        if (station != null) {
            sendSetFilter(menu.getWorkstationSlotIndex(), station);
        }

        return null;
    }

    /**
     * 挑这一格放哪个变体。
     *
     * <p>顺序：先把 ME 网络里真有库存的候选挑出来（编码出来的样板才会指向玩家拿得到的东西），
     * 其次挑在 JEI 里可见的（配置里被隐藏的物品不选），最后退回第一个候选。
     * 与 AE2 的 {@code EncodingHelper} 一个思路，只是不看数量、只看有没有。</p>
     */
    private ItemStack pick(IRecipeSlotView slot, Set<AEItemKey> stock) {
        List<ItemStack> candidates = slot.getIngredients(VanillaTypes.ITEM_STACK).toList();
        if (candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (ItemStack candidate : candidates) {
            AEItemKey key = AEItemKey.of(candidate);
            if (key != null && stock.contains(key)) {
                return candidate.copy();
            }
        }
        for (ItemStack candidate : candidates) {
            if (visibility.isIngredientVisible(VanillaTypes.ITEM_STACK, candidate)) {
                return candidate.copy();
            }
        }
        return candidates.getFirst().copy();
    }

    /** 网络里存着的物品键。仓库还没同步下来时返回空集，退化成「挑第一个可见的」。 */
    private static Set<AEItemKey> networkStock(AugmentedPatternEncodingTermMenu menu) {
        var repo = menu.getClientRepo();
        if (repo == null) {
            return Set.of();
        }
        return repo.getAllEntries().stream()
                .filter(entry -> entry.getStoredAmount() > 0)
                .map(GridInventoryEntry::getWhat)
                .filter(AEItemKey.class::isInstance)
                .map(AEItemKey.class::cast)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static void sendSetFilter(int slotIndex, ItemStack stack) {
        PacketDistributor.sendToServer(
                new InventoryActionPacket(InventoryAction.SET_FILTER, slotIndex, stack));
    }
}
