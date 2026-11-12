package io.github.lounode.ae2crafting.common.block.entity;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;

import io.github.lounode.ae2crafting.common.logic.AugmentedRecipeMatcher;
import io.github.lounode.ae2crafting.common.logic.CraftKind;
import io.github.lounode.ae2crafting.common.logic.CraftRules;
import io.github.lounode.ae2crafting.common.menu.AssemblerSlots;
import io.github.lounode.ae2crafting.common.pattern.AugmentedPattern;

/**
 * 装配室的「配方规则 + 铺料」这一摊事：找标志物、解析机器类别、算通量耗能、
 * 把供应器给的输入铺进 9×9、以及清空/快照网格。
 *
 * <p>从方块实体里拆出来是为了让主类只留生命周期、tick 调度、推产物与动画；这些方法只读写
 * 91 格库存与网络节点，不需要碰方块实体自己的存档与同步状态。规则缓存（{@code rulesFor}）
 * 也放在这里：它以样板的对象身份为键，读档重建 plan 后自然失效。</p>
 */
final class AssemblerCraftOps {

    private final MolecularAssemblerBlockEntity owner;

    /** 规则缓存对应的样板；换对象（含读档重建）时重算一次，避免每 tick 重跑判定器链。 */
    @Nullable
    private AugmentedPattern rulesFor;
    private CraftKind craftKind = CraftKind.NORMAL;
    private int requiredInputs;

    AssemblerCraftOps(MolecularAssemblerBlockEntity owner) {
        this.owner = owner;
    }

    /**
     * 标志物栏里有没有这张样板要的工作台。
     *
     * <p>样板不要工作台（原版 3×3、合成拓展按自身 tier 匹配的那类）时直接放行；
     * 要的话必须<b>同一个物品键</b>（含组件）出现在标志物栏里。</p>
     */
    boolean hasRequiredWorkstation(AugmentedPattern pattern) {
        var required = pattern.getWorkstation();
        if (required == null) {
            return true;
        }
        return findWorkstation(required) != null;
    }

    @Nullable
    private ItemStack findWorkstation(AEItemKey required) {
        for (int i = 0; i < AssemblerSlots.CATALYST_SLOTS; i++) {
            var stack = owner.getInventory().getStackInSlot(AssemblerSlots.catalyst(i));
            if (!stack.isEmpty() && required.equals(AEItemKey.of(stack))) {
                return stack;
            }
        }
        return null;
    }

    /**
     * 交给判定器的「标志工作方块」。
     *
     * <p>样板不要工作台时返回空栈而不是随便拿一格：无尽的两条判定器只看标志物，
     * 递一个无关的台子过去会让它们抢答成另一个配方。</p>
     */
    ItemStack stationFor(AugmentedPattern pattern) {
        var required = pattern.getWorkstation();
        if (required == null) {
            return ItemStack.EMPTY;
        }
        var found = findWorkstation(required);
        return found != null ? found : ItemStack.EMPTY;
    }

    /**
     * 标志物栏里某一类催化剂的总个数（按堆叠数量累加）。
     *
     * <p>直接遍历 9 个槽位，不建中间列表：装配室每 tick 都要算耗时倍率，多配方/多机器时
     * 每 tick 一次的列表分配没有必要。</p>
     */
    private int countCatalyst(CraftRules.Catalyst wanted) {
        int total = 0;
        for (int i = 0; i < AssemblerSlots.CATALYST_SLOTS; i++) {
            var stack = owner.getInventory().getStackInSlot(AssemblerSlots.catalyst(i));
            if (!stack.isEmpty() && CraftRules.catalystOf(stack) == wanted) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * 从样板里的网格重跑一次判定器链，取出机器类别与所需输入数。
     *
     * <p>这些信息故意不写进样板：按约定编码端只带工作台，加成与槽位都在执行时按标志物栏
     * 动态决定。</p>
     */
    void ensureRules(AugmentedPattern pattern) {
        if (rulesFor == pattern) {
            return;
        }
        rulesFor = pattern;
        craftKind = CraftKind.NORMAL;
        requiredInputs = 0;
        var level = owner.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        var data = pattern.getData();
        var items = AugmentedRecipeMatcher.toItemStacks(data.grid());
        var match = AugmentedRecipeMatcher.match(level, items, data.width(), data.height(),
                stationFor(pattern));
        if (match != null) {
            craftKind = match.kind();
            requiredInputs = match.requiredInputs();
        }
    }

    /** 当前配方需要多慢：末影/通量按发电机数量折减，其余恒为 1。 */
    int durationMultiplier() {
        if (!craftKind.isAlternatorDriven()) {
            return 1;
        }
        var wanted = craftKind == CraftKind.ENDER_CRAFTER
                ? CraftRules.Catalyst.ENDER_ALTERNATOR : CraftRules.Catalyst.FLUX_ALTERNATOR;
        return CraftRules.durationMultiplier(craftKind, countCatalyst(wanted));
    }

    /** 组合合成器：基础 1 槽 + 每个基座 1 槽，够不够放下这条配方。 */
    boolean canAcceptCombination() {
        return craftKind != CraftKind.COMBINATION
                || CraftRules.availableInputSlots(countCatalyst(CraftRules.Catalyst.PEDESTAL)) >= requiredInputs;
    }

    /**
     * 通量合成器的额外能耗：每个生效的通量发电机 8000 AE，每次合成扣一次。
     *
     * <p>先 SIMULATE 再 MODULATE：电量不够时整份拒绝，不会扣到一半再失败。</p>
     */
    boolean payFluxPower() {
        long cost = CraftRules.fluxPowerCost(craftKind, countCatalyst(CraftRules.Catalyst.FLUX_ALTERNATOR));
        if (cost <= 0) {
            return true;
        }
        var grid = owner.getMainNode().getGrid();
        if (grid == null) {
            return false;
        }
        var energy = grid.getEnergyService();
        double requested = cost;
        double available = energy.extractAEPower(requested, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        if (available + 1.0E-4 < requested) {
            return false;
        }
        energy.extractAEPower(requested, Actionable.MODULATE, PowerMultiplier.CONFIG);
        return true;
    }

    /**
     * 把供应器给的输入按样板网格铺进 81 格。
     *
     * <p>{@code table} 的顺序与样板 {@code getInputs()} 一致，也就是<b>非空格的行优先顺序</b>，
     * 不是 9×9 的下标；产物容纳物是最后一项，只从计数器里扣掉，不铺任何槽位。铺完后每个
     * 计数器都必须归零：剩下东西说明有输入没落地，这时候宁可把已经铺好的也收回去，
     * 也不要带着半份原料开机。</p>
     */
    boolean fillGrid(AugmentedPattern pattern, KeyCounter[] table) {
        var data = pattern.getData();
        int inputIndex = 0;
        for (int y = 0; y < data.height(); y++) {
            for (int x = 0; x < data.width(); x++) {
                if (data.at(x, y) == null) {
                    continue;
                }
                if (inputIndex >= table.length) {
                    clearGrid();
                    return false;
                }
                var counter = table[inputIndex++];
                var entry = counter.getFirstEntry();
                if (entry == null || !(entry.getKey() instanceof AEItemKey key)) {
                    clearGrid();
                    return false;
                }
                long amount = entry.getLongValue();
                if (amount <= 0 || amount > key.getMaxStackSize()) {
                    clearGrid();
                    return false;
                }
                owner.getInventory().setItemDirect(AssemblerSlots.grid(y, x), key.toStack((int) amount));
                counter.remove(key, amount);
            }
        }

        // 产物容纳物：AE2 已按 getInputs() 把它备进 table 的最后一份，这里只消耗、不铺槽。
        // 不扣掉的话末尾「计数器必须归零」的校验会判定失败，把已经铺好的原料全部回滚。
        if (pattern.getContainer() != null) {
            if (inputIndex >= table.length) {
                clearGrid();
                return false;
            }
            var counter = table[inputIndex++];
            var entry = counter.getFirstEntry();
            if (entry == null || entry.getLongValue() <= 0) {
                clearGrid();
                return false;
            }
            counter.remove(entry.getKey(), entry.getLongValue());
        }

        for (var counter : table) {
            counter.removeZeros();
            if (!counter.isEmpty()) {
                clearGrid();
                return false;
            }
        }
        return true;
    }

    void clearGrid() {
        for (int i = 0; i < AssemblerSlots.GRID_SLOTS; i++) {
            owner.getInventory().setItemDirect(i, ItemStack.EMPTY);
        }
    }

    boolean isGridEmpty() {
        for (int i = 0; i < AssemblerSlots.GRID_SLOTS; i++) {
            if (!owner.getInventory().getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    List<ItemStack> snapshotGridItems() {
        var items = new ArrayList<ItemStack>(AssemblerSlots.GRID_SLOTS);
        for (int i = 0; i < AssemblerSlots.GRID_SLOTS; i++) {
            items.add(owner.getInventory().getStackInSlot(i));
        }
        return items;
    }
}
