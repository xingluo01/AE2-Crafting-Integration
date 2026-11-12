package io.github.lounode.ae2crafting.common.pattern;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

import io.github.lounode.ae2crafting.registry.ModDataComponents;

/**
 * 增广合成样板：保留格子位置的 9×9 合成样板。
 *
 * <p>AE2 的合成样板（{@code AECraftingPattern}）只认 3×3：它把配方 id 存进组件，解码时回查
 * {@code CraftingRecipe}。9×9 没有对应的原版配方类型，所以这里改成<b>直接存网格</b>：
 * 每一格的坐标与物品都写进 {@link AugmentedPatternData}，解码时逐格还原成 {@link IInput}。</p>
 *
 * <p>输入按「每格一个 {@code IInput}、倍率为该格数量」展开，空格不产生输入；
 * 这与合成样板在装配室里的语义一致：产物按网格摆放决定，而不是按无序配比。</p>
 *
 * <p>{@code equals/hashCode} 必须基于样板物品本身（含组件），否则世界重载后合成任务无法续接。</p>
 */
public class AugmentedPattern implements IPatternDetails {

    private final AEItemKey definition;
    private final AugmentedPatternData data;
    private final IInput[] inputs;
    private final List<GenericStack> outputs;

    public AugmentedPattern(AEItemKey definition, Level level) {
        this.definition = definition;

        var encoded = definition.get(ModDataComponents.AUGMENTED_PATTERN.get());
        if (encoded == null) {
            throw new IllegalArgumentException("Item does not encode an augmented pattern: " + definition);
        }
        this.data = encoded;

        var builtInputs = new ArrayList<IInput>(data.filledSlots());
        for (var stack : data.grid()) {
            if (stack != null) {
                builtInputs.add(new Input(stack, data.substitute()));
            }
        }
        if (builtInputs.isEmpty()) {
            throw new IllegalArgumentException("Augmented pattern has no inputs: " + definition);
        }
        this.inputs = builtInputs.toArray(IInput[]::new);
        this.outputs = List.copyOf(data.outputs());
    }

    /**
     * 把网格写进样板物品。
     *
     * <p>{@code workstation} 是编码终端标志工作方块槽里的那件物品，写进样板后装配室会拿它
     * 与本机标志物栏比对：对不上就不接这张样板（见 {@code MolecularAssemblerBlockEntity}）。
     * 原版 3×3 配方传 {@code null}。</p>
     *
     * <p>{@code grid} 必须是行优先、长度 {@code width * height} 的列表，空格用 {@code null}。</p>
     */
    public static void encode(ItemStack stack, int width, int height,
            List<@Nullable GenericStack> grid, List<GenericStack> outputs, @Nullable AEItemKey workstation) {
        encode(stack, width, height, grid, outputs, workstation, false);
    }

    /**
     * 带替换开关状态的写法。{@code substitute} 取自编码终端的切换按钮，随样板落盘；
     * 放进输出槽反填时会把按钮恢复到编码时的状态。
     */
    public static void encode(ItemStack stack, int width, int height,
            List<@Nullable GenericStack> grid, List<GenericStack> outputs, @Nullable AEItemKey workstation,
            boolean substitute) {
        Objects.requireNonNull(stack, "stack");
        stack.set(ModDataComponents.AUGMENTED_PATTERN.get(),
                new AugmentedPatternData(width, height, grid, outputs, workstation, substitute));
    }

    /** 这张样板要求的工作台；{@code null} 表示不需要（原版 3×3 之类）。 */
    @Nullable
    public AEItemKey getWorkstation() {
        return data.workstation();
    }

    /** 样板记录的网格尺寸与内容；界面画 9×9 预览时要用。 */
    public AugmentedPatternData getData() {
        return data;
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public List<GenericStack> getOutputs() {
        return outputs;
    }

    /**
     * tooltip 用：网格里的 1-based (列, 行) 坐标，与编码台/装配室里玩家看到的格序一致。
     */
    public record GridPos(int column, int row) {
    }

    /** 一种合并后的输入：总量 + 它出现过的所有格子。 */
    public record InputGroup(GenericStack stack, List<GridPos> positions) {
    }

    /** 行优先下标换算成 1-based 坐标；纯算术，方便单测。 */
    public static GridPos positionOf(int index, int width) {
        return new GridPos(index % width + 1, index / width + 1);
    }

    /**
     * 把网格里相同的物品合并成一组，数量按格累加，并记下每组的坐标。
     *
     * <p>AE2 默认的 tooltip 是「每个 {@code IInput} 一行」，9×9 样板铺开后同一个物品会重复
     * 六七行（见实机截图）。这里按 {@link AEKey} 全等分组（含组件），用 {@link LinkedHashMap}
     * 保持物品在网格里首次出现的顺序，坐标按行优先追加。</p>
     */
    public List<InputGroup> groupedInputs() {
        Map<AEKey, MutableGroup> groups = new LinkedHashMap<>();
        var grid = data.grid();
        for (int i = 0; i < grid.size(); i++) {
            var stack = grid.get(i);
            if (stack == null) {
                continue;
            }
            var group = groups.computeIfAbsent(stack.what(), key -> new MutableGroup());
            group.amount += stack.amount();
            group.positions.add(positionOf(i, data.width()));
        }

        var result = new ArrayList<InputGroup>(groups.size());
        for (var entry : groups.entrySet()) {
            var group = entry.getValue();
            result.add(new InputGroup(new GenericStack(entry.getKey(), group.amount),
                    List.copyOf(group.positions)));
        }
        return result;
    }

    private static final class MutableGroup {
        private long amount;
        private final List<GridPos> positions = new ArrayList<>();
    }

    /**
     * 装配室专用样板不往普通容器里推输入：产物由网格摆放决定，外部容器无法保证形状。
     * 与 AE2 合成样板的 {@code supportsPushInputsToExternalInventory() = false} 同义。
     */
    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass()
                && ((AugmentedPattern) obj).definition.equals(definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    /** 单格输入：可能输入恒为该物品 1 个，倍率取格子里的数量。 */
    private record Input(GenericStack template, GenericStack[] possible, boolean substitute) implements IInput {

        Input(GenericStack template, boolean substitute) {
            this(template, new GenericStack[] { new GenericStack(template.what(), 1) }, substitute);
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return possible;
        }

        @Override
        public long getMultiplier() {
            return template.amount();
        }

        /**
         * 替换已启用时，同一个物品、只差组件（NBT / 耐久）的输入也算数。
         *
         * <p>不扩大 {@link #getPossibleInputs()}：AE2 的 CPU 仍按样板记录的具体键备料，
         * 这里只放宽校验，避免替换开关把备料范围变成不可控的标签搜索。</p>
         */
        @Override
        public boolean isValid(AEKey input, Level level) {
            if (input.matches(possible[0])) {
                return true;
            }
            return substitute
                    && input instanceof AEItemKey candidate
                    && template.what() instanceof AEItemKey expected
                    && candidate.getItem() == expected.getItem();
        }

        @Nullable
        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
