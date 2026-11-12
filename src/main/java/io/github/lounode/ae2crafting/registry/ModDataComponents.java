package io.github.lounode.ae2crafting.registry;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.pattern.AugmentedPatternData;

/**
 * 数据组件注册。
 *
 * <p>增广合成样板把整个 9×9 网格塞进组件，因此 {@code persistent} 与 {@code networkSynchronized}
 * 两条都要给：前者保证存档不丢，后者保证样板在客户端能画出提示与预览。
 * 只给 persistent 的组件在客户端会读到 {@code null}，样板看起来是空的。</p>
 */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AE2CraftingIntegration.MOD_ID);

    public static final Supplier<DataComponentType<AugmentedPatternData>> AUGMENTED_PATTERN =
            DATA_COMPONENTS.register("augmented_pattern",
                    () -> DataComponentType.<AugmentedPatternData>builder()
                            .persistent(AugmentedPatternData.codec())
                            .networkSynchronized(AugmentedPatternData.STREAM_CODEC)
                            .build());

    private ModDataComponents() {
    }
}
