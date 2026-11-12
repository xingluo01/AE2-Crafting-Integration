package io.github.lounode.ae2crafting.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;

import appeng.client.render.crafting.AssemblerAnimationStatus;
import appeng.client.render.effects.ParticleTypes;

import io.github.lounode.ae2crafting.AE2CraftingIntegration;
import io.github.lounode.ae2crafting.common.block.entity.MolecularAssemblerBlockEntity;

/**
 * 装配室的方块实体渲染：呼吸灯 + 产物浮空动画。
 *
 * <p>两段都照 AE2 分子装配室的 {@code MolecularAssemblerRenderer} 写，只是对象换成本模组的方块实体：</p>
 * <ul>
 *   <li><b>呼吸灯</b>：{@code block/molecular_assembler_lights} 是一张 12 帧的动画贴图，用
 *       {@code tripwire} 渲染类型画在方块中心（模型 JSON 的 {@code neoforge_data} 给六个面都写了
 *       block/sky light 15，所以不受环境光影响）；{@link MolecularAssemblerBlockEntity#isPowered()}
 *       为真时才画。状态来自方块实体的流同步（{@code onMainNodeStateChanged} 里算、{@code markForUpdate} 推）。</li>
 *   <li><b>产物</b>：每完成一次合成，服务端把产物键与耗时写进更新包，客户端据此新建一个
 *       {@link AssemblerAnimationStatus}，在方块中心浮空画 2 秒左右并撒合成粒子。</li>
 * </ul>
 *
 * <p>贴图动画不由本类驱动——JSON 里的 {@code .mcmeta} 已经声明了帧序列，画这张模型就是在「呼吸」。</p>
 */
public class MolecularAssemblerRenderer implements BlockEntityRenderer<MolecularAssemblerBlockEntity> {

    public static final ModelResourceLocation LIGHTS_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(AE2CraftingIntegration.MOD_ID, "block/molecular_assembler_lights"));

    private final RandomSource particleRandom = RandomSource.create();

    public MolecularAssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MolecularAssemblerBlockEntity assembler, float partialTicks, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        // 先让方块实体看一眼更新包里有没有新的合成时间戳：判定要用到客户端专属字段，
        // 只能在这里触发，不能写进服务端也会走的 readFromStream（见 updateAnimationStatus 的注释）。
        assembler.updateAnimationStatus();

        var status = assembler.getAnimationStatus();
        if (status != null) {
            if (!Minecraft.getInstance().isPaused()) {
                if (status.isExpired()) {
                    assembler.setAnimationStatus(null);
                } else {
                    status.setAccumulatedTicks(status.getAccumulatedTicks() + partialTicks);
                    status.setTicksUntilParticles(status.getTicksUntilParticles() - partialTicks);
                }
            }
            renderStatus(assembler, pose, buffers, light, status);
        }

        if (assembler.isPowered()) {
            renderPowerLight(pose, buffers, light, overlay);
        }
    }

    private void renderPowerLight(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel lightsModel = minecraft.getModelManager().getModel(LIGHTS_MODEL);
        VertexConsumer buffer = buffers.getBuffer(RenderType.tripwire());
        minecraft.getBlockRenderer()
                .getModelRenderer()
                .renderModel(pose.last(), buffer, null, lightsModel,
                        1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, null);
    }

    private void renderStatus(MolecularAssemblerBlockEntity assembler, PoseStack pose,
            MultiBufferSource buffers, int light, AssemblerAnimationStatus status) {
        double centerX = assembler.getBlockPos().getX() + 0.5;
        double centerY = assembler.getBlockPos().getY() + 0.5;
        double centerZ = assembler.getBlockPos().getZ() + 0.5;

        Minecraft minecraft = Minecraft.getInstance();
        if (status.getTicksUntilParticles() <= 0.0F) {
            status.setTicksUntilParticles(4.0F);
            for (int i = 0; i < (int) Math.ceil(status.getSpeed() / 5.0); i++) {
                minecraft.particleEngine.createParticle(ParticleTypes.CRAFTING,
                        centerX, centerY, centerZ, 0.0, 0.0, 0.0);
            }
        }

        ItemStack output = status.getIs();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        if (output.getItem() instanceof BlockItem) {
            pose.translate(0.0F, -0.2F, 0.0F);
        } else {
            pose.translate(0.0F, -0.3F, 0.0F);
        }
        itemRenderer.renderStatic(output, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, assembler.getLevel(), 0);
        pose.popPose();
    }
}
