package io.github.lounode.ae2crafting.common.pattern;

import java.util.ArrayList;
import java.util.List;
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
                builtInputs.add(new Input(stack));
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
     * <p>编码入口尚未接到编码终端；等界面上有 9×9 编码面板时，直接调这里即可。
     * {@code grid} 必须是行优先、长度 {@code width * height} 的列表，空格用 {@code null}。</p>
     */
    public static void encode(ItemStack stack, int width, int height,
            List<@Nullable GenericStack> grid, List<GenericStack> outputs) {
        Objects.requireNonNull(stack, "stack");
        stack.set(ModDataComponents.AUGMENTED_PATTERN.get(),
                new AugmentedPatternData(width, height, grid, outputs));
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
    private record Input(GenericStack template, GenericStack[] possible) implements IInput {

        Input(GenericStack template) {
            this(template, new GenericStack[] { new GenericStack(template.what(), 1) });
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return possible;
        }

        @Override
        public long getMultiplier() {
            return template.amount();
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return input.matches(possible[0]);
        }

        @Nullable
        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
