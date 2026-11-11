package io.github.lounode.ae2crafting.common.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import appeng.api.stacks.GenericStack;

/**
 * 增广合成样板的数据载体。
 *
 * <p>与 AE2 的合成样板不同，这里<b>不存配方 id</b>：原版 {@code CraftingRecipe} 上限 3×3，
 * 9×9 网格没有对应的配方类型可用。改为直接存「二维网格 + 产物」，语义接近处理样板，
 * 但保留每一格的坐标，这样装配室才能按位置摆放物品、也才能校验形状。</p>
 *
 * <p>{@code grid} 是行优先的定长列表，长度必须等于 {@code width * height}；
 * 空格用 {@code null} 占位，因此用容错型可空列表编解码（缺内容时会降级成占位而不是整份样板失效）。</p>
 */
public record AugmentedPatternData(int width, int height, List<@Nullable GenericStack> grid,
        List<GenericStack> outputs) {

    /** 与 {@code AssemblerSlots.GRID_SIZE} 一致；写死在这里避免 common 层依赖菜单包。 */
    public static final int MAX_DIMENSION = 9;

    public static final Codec<AugmentedPatternData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.intRange(1, MAX_DIMENSION).fieldOf("width").forGetter(AugmentedPatternData::width),
            Codec.intRange(1, MAX_DIMENSION).fieldOf("height").forGetter(AugmentedPatternData::height),
            GenericStack.FAULT_TOLERANT_NULLABLE_LIST_CODEC.fieldOf("grid")
                    .forGetter(AugmentedPatternData::grid),
            GenericStack.FAULT_TOLERANT_LIST_CODEC.fieldOf("outputs").forGetter(AugmentedPatternData::outputs))
            .apply(builder, AugmentedPatternData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AugmentedPatternData> STREAM_CODEC = StreamCodec.of(
            AugmentedPatternData::write, AugmentedPatternData::read);

    public AugmentedPatternData {
        if (width < 1 || width > MAX_DIMENSION || height < 1 || height > MAX_DIMENSION) {
            throw new IllegalArgumentException(
                    "Augmented pattern dimension out of range: " + width + "x" + height);
        }
        int expected = width * height;
        if (grid.size() != expected) {
            throw new IllegalArgumentException(
                    "Augmented pattern grid must have " + expected + " slots, got " + grid.size());
        }
        if (outputs.isEmpty() || outputs.getFirst() == null) {
            throw new IllegalArgumentException("Augmented pattern needs a primary output");
        }
        grid = Collections.unmodifiableList(new ArrayList<>(grid));
        outputs = List.copyOf(outputs);
    }

    /** 网格中非空格的数量，用于界面提示与样板匹配的快速判断。 */
    public int filledSlots() {
        return (int) grid.stream().filter(Objects::nonNull).count();
    }

    /** 取指定格子的内容，越界返回 {@code null}。 */
    @Nullable
    public GenericStack at(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return grid.get(y * width + x);
    }

    private static void write(RegistryFriendlyByteBuf buffer, AugmentedPatternData data) {
        buffer.writeVarInt(data.width);
        buffer.writeVarInt(data.height);
        for (var stack : data.grid) {
            GenericStack.writeBuffer(stack, buffer);
        }
        buffer.writeVarInt(data.outputs.size());
        for (var stack : data.outputs) {
            GenericStack.writeBuffer(stack, buffer);
        }
    }

    private static AugmentedPatternData read(RegistryFriendlyByteBuf buffer) {
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        var grid = new ArrayList<GenericStack>(width * height);
        for (int i = 0; i < width * height; i++) {
            grid.add(GenericStack.readBuffer(buffer));
        }
        int outputCount = buffer.readVarInt();
        var outputs = new ArrayList<GenericStack>(outputCount);
        for (int i = 0; i < outputCount; i++) {
            outputs.add(GenericStack.readBuffer(buffer));
        }
        return new AugmentedPatternData(width, height, grid, outputs);
    }
}
