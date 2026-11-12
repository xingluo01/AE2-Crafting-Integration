package io.github.lounode.ae2crafting.common.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.StreamCodec;

import appeng.api.stacks.AEItemKey;
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
 *
 * <p><b>{@code workstation} 是「这张样板要用哪张工作台」</b>，取自编码终端的标志工作方块槽。
 * 装配室据此决定要不要接这张样板：标志物栏里没有对应工作台时，样板连推都推不进来
 * （见 {@code MolecularAssemblerBlockEntity#pushPattern}）。原版 3×3 配方没有工作台，
 * 这个字段就是 {@code null}；老样板解码出来也是 {@code null}，按「不需要工作台」处理。</p>
 *
 * <p><b>{@code substitute} 是编码终端「替换已启用 / 已禁用」开关的状态</b>：写进样板后，
 * 把样板放回输出槽能还原开关，装配室也据此决定输入校验是否放宽。老样板没有这一项，
 * 解码后默认 {@code false}（替换已禁用），行为与加字段之前一致。</p>
 *
 * <p><b>{@code container} 是终焉厨锅的产物容纳物</b>（碗、桶之类）：每次合成消耗一份，
 * 所以它作为样板的最后一个输入参与备料，但<b>不写进 {@code grid}</b>——装配室不把它铺进
 * 9×9 槽位，只从计数器里扣掉。老样板没有这一项，解码后为 {@code null}。</p>
 */
public record AugmentedPatternData(int width, int height, List<@Nullable GenericStack> grid,
        List<GenericStack> outputs, @Nullable AEItemKey workstation, boolean substitute,
        @Nullable GenericStack container, @Nullable ResourceLocation recipeId) {

    /** 与 {@code AssemblerSlots.GRID_SIZE} 一致；写死在这里避免 common 层依赖菜单包。 */
    public static final int MAX_DIMENSION = 9;

    public static final StreamCodec<RegistryFriendlyByteBuf, AugmentedPatternData> STREAM_CODEC = StreamCodec.of(
            AugmentedPatternData::write, AugmentedPatternData::read);

    /**
     * 落盘用的编解码器。
     *
     * <p><b>不能写成静态字段</b>：{@code AEItemKey.CODEC} 的静态初始化会碰
     * {@code BuiltInRegistries}，而它一进去就 {@code checkBootstrapCalled}。写成字段的话，
     * 连 {@code new AugmentedPatternData(...)} 这种纯数据校验都会连带崩掉（类初始化失败，
     * 后续访问一律 {@code NoClassDefFoundError}）。嵌套类的初始化是懒的，只有真的读写 NBT
     * 才付出这份代价，那时必然已经 bootstrap 过。</p>
     */
    public static Codec<AugmentedPatternData> codec() {
        return Codecs.DATA;
    }

    private static final class Codecs {

        static final Codec<AugmentedPatternData> DATA = RecordCodecBuilder.create(builder -> builder.group(
                Codec.intRange(1, MAX_DIMENSION).fieldOf("width").forGetter(AugmentedPatternData::width),
                Codec.intRange(1, MAX_DIMENSION).fieldOf("height").forGetter(AugmentedPatternData::height),
                GenericStack.FAULT_TOLERANT_NULLABLE_LIST_CODEC.fieldOf("grid")
                        .forGetter(AugmentedPatternData::grid),
                GenericStack.FAULT_TOLERANT_LIST_CODEC.fieldOf("outputs")
                        .forGetter(AugmentedPatternData::outputs),
                // 可选字段：老样板没有这一项，解码后是空 Optional → null。
                AEItemKey.CODEC.optionalFieldOf("workstation")
                        .forGetter(data -> Optional.ofNullable(data.workstation())),
                // 同样是可选字段：老样板按「替换已禁用」处理。
                Codec.BOOL.optionalFieldOf("substitute")
                        .forGetter(data -> Optional.of(data.substitute())),
                // 可选字段：老样板没有产物容纳物。
                GenericStack.CODEC.optionalFieldOf("container")
                        .forGetter(data -> Optional.ofNullable(data.container())),
                // 可选字段：编码时选中的候选配方 id，装配室据此按同一配方执行。
                ResourceLocation.CODEC.optionalFieldOf("recipe")
                        .forGetter(data -> Optional.ofNullable(data.recipeId())))
                .apply(builder, (width, height, grid, outputs, workstation, substitute, container, recipe) ->
                        new AugmentedPatternData(width, height, grid, outputs,
                                workstation.orElse(null), substitute.orElse(false),
                                container.orElse(null), recipe.orElse(null))));

        private Codecs() {
        }
    }

    /** 不带标志工作方块、替换状态与容纳物的旧写法，原版 3×3 配方与既有测试用。 */
    public AugmentedPatternData(int width, int height, List<@Nullable GenericStack> grid,
            List<GenericStack> outputs) {
        this(width, height, grid, outputs, null, false, null, null);
    }

    /** 不带替换状态与容纳物的写法，默认「替换已禁用」。 */
    public AugmentedPatternData(int width, int height, List<@Nullable GenericStack> grid,
            List<GenericStack> outputs, @Nullable AEItemKey workstation) {
        this(width, height, grid, outputs, workstation, false, null, null);
    }

    /** 带替换状态但不带容纳物的写法。 */
    public AugmentedPatternData(int width, int height, List<@Nullable GenericStack> grid,
            List<GenericStack> outputs, @Nullable AEItemKey workstation, boolean substitute) {
        this(width, height, grid, outputs, workstation, substitute, null, null);
    }

    /** 带容纳物但不带候选配方 id 的写法。 */
    public AugmentedPatternData(int width, int height, List<@Nullable GenericStack> grid,
            List<GenericStack> outputs, @Nullable AEItemKey workstation, boolean substitute,
            @Nullable GenericStack container) {
        this(width, height, grid, outputs, workstation, substitute, container, null);
    }

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

    /** 这张样板是否要求某张工作台。{@code false} 表示原版 3×3 之类不需要工作台的配方。 */
    public boolean requiresWorkstation() {
        return workstation != null;
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
        buffer.writeBoolean(data.workstation != null);
        if (data.workstation != null) {
            data.workstation.writeToPacket(buffer);
        }
        buffer.writeBoolean(data.substitute);
        buffer.writeBoolean(data.container != null);
        if (data.container != null) {
            GenericStack.writeBuffer(data.container, buffer);
        }
        buffer.writeBoolean(data.recipeId != null);
        if (data.recipeId != null) {
            buffer.writeResourceLocation(data.recipeId);
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
        AEItemKey workstation = buffer.readBoolean() ? AEItemKey.fromPacket(buffer) : null;
        boolean substitute = buffer.readBoolean();
        GenericStack container = buffer.readBoolean() ? GenericStack.readBuffer(buffer) : null;
        ResourceLocation recipeId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        return new AugmentedPatternData(width, height, grid, outputs, workstation, substitute, container, recipeId);
    }
}
