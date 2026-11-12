package io.github.lounode.ae2crafting.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import guideme.Guide;
import guideme.GuidesCommon;
import guideme.PageAnchor;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.client.gui.AugmentedPatternEncodingTermScreen;
import io.github.lounode.ae2crafting.client.gui.MolecularAssemblerScreen;
import io.github.lounode.ae2crafting.client.render.MolecularAssemblerRenderer;
import io.github.lounode.ae2crafting.registry.ModBlockEntities;
import io.github.lounode.ae2crafting.registry.ModMenuTypes;

/**
 * 仅客户端入口：绑定菜单界面，并注册本模组自己的 GuideME 指南书。
 *
 * <p>指南书的资源根是 {@code assets/ae2_crafting_integration/ae2guide}，页面 id 为
 * {@code ae2_crafting_integration:<文件名>}。{@code folder("ae2guide")} 必须与资源目录一致，
 * 改名会让指南打开后一片空白，且日志里只有一条「0 pages」。</p>
 *
 * <p>指南书在<b>构造函数里</b>建，而不是 {@code FMLClientSetupEvent}：GuideME 的静态指南表
 * 只会在资源重载时被读取一次，注册晚了页面就是空的。AE2 自己也是在
 * {@code AppEngClient} 的构造函数里调 {@code createGuide()}。</p>
 */
@Mod(value = AE2CraftingIntegration.MOD_ID, dist = Dist.CLIENT)
public class AE2CraftingClient {

    public static final ResourceLocation GUIDE_ID =
            ResourceLocation.fromNamespaceAndPath(AE2CraftingIntegration.MOD_ID, "guide");
    private static final ResourceLocation GUIDE_START_PAGE =
            ResourceLocation.fromNamespaceAndPath(AE2CraftingIntegration.MOD_ID, "index");
    private static final ResourceLocation GUIDE_MACHINE_PAGE =
            ResourceLocation.fromNamespaceAndPath(AE2CraftingIntegration.MOD_ID, "molecular_assembler");
    private static final ResourceLocation GUIDE_TERMINAL_PAGE =
            ResourceLocation.fromNamespaceAndPath(AE2CraftingIntegration.MOD_ID, "augmented_pattern");

    private final Guide guide;

    public AE2CraftingClient(IEventBus modBus) {
        modBus.addListener(this::registerScreens);
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::registerAdditionalModels);

        this.guide = Guide.builder(GUIDE_ID)
                .folder("ae2guide")
                .defaultNamespace(AE2CraftingIntegration.MOD_ID)
                .defaultLanguage("zh_cn")
                .startPage(GUIDE_START_PAGE)
                .build();
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.MOLECULAR_ASSEMBLER.get(), MolecularAssemblerScreen::new);
        event.register(ModMenuTypes.AUGMENTED_PATTERN_TERMINAL.get(), AugmentedPatternEncodingTermScreen::new);
    }

    /**
     * 装配室的呼吸灯与产物浮空动画走方块实体渲染器。
     *
     * <p>AE2 只给自家 {@code MolecularAssemblerBlockEntity} 挂了渲染器，本模组是另一个方块实体类型，
     * 必须自己登记；漏了不报错，只是灯永远不亮、产物永远不浮起来。</p>
     */
    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MOLECULAR_ASSEMBLER.get(),
                MolecularAssemblerRenderer::new);
    }

    /**
     * 呼吸灯的模型不在 blockstate 里，是独立的一份 {@code block/molecular_assembler_lights}，
     * 由渲染器按 {@link MolecularAssemblerRenderer#LIGHTS_MODEL} 直接取。AE2 也用它自己的
     * {@code InitAdditionalModels} 走同一个「额外模型」通道；不登记的话
     * {@code getModel} 拿到的是 missing model（紫黑方块）。
     */
    private void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MolecularAssemblerRenderer.LIGHTS_MODEL);
    }

    /** 指南书本体，留给后续做「手持指南物品」或调试命令时取用。 */
    public Guide getGuide() {
        return guide;
    }

    /** 左侧指南按钮的落点：直接翻到装配室那一页。 */
    public static void openGuide(LocalPlayer player) {
        GuidesCommon.openGuide(player, GUIDE_ID, PageAnchor.page(GUIDE_MACHINE_PAGE));
    }

    /** 编码终端的指南按钮落点：翻到增广合成样板那一页。 */
    public static void openTerminalGuide(LocalPlayer player) {
        GuidesCommon.openGuide(player, GUIDE_ID, PageAnchor.page(GUIDE_TERMINAL_PAGE));
    }
}
