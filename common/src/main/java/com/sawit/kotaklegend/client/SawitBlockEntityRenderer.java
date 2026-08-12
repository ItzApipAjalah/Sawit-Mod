package com.sawit.kotaklegend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sawit.kotaklegend.block.SawitBlock;
import com.sawit.kotaklegend.block.SawitBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

public class SawitBlockEntityRenderer implements BlockEntityRenderer<SawitBlockEntity> {
    public SawitBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SawitBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // Read live state from level — blockEntity.getBlockState() can return stale AGE=0 right after world gen
        net.minecraft.world.level.Level level = blockEntity.getLevel();
        if (level == null) return;
        BlockState state = blockEntity.getBlockState();
        if (state.getBlock() instanceof SawitBlock && state.getValue(SawitBlock.AGE) >= 4) {

            BlockState dummyState = state.getValue(SawitBlock.AGE) == 5 ?
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_LEAVES_FRUIT_DUMMY.get().defaultBlockState() :
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_LEAVES_DUMMY.get().defaultBlockState();

            // Calculate light at the canopy level. If the chunk section above isn't loaded/lit yet,
            // getLightColor will return 0 (pitch black, invisible). In that case, fallback to the base light.
            net.minecraft.core.BlockPos leafPos = blockEntity.getBlockPos().above(11);
            int leafLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, dummyState, leafPos);
            if (leafLight <= 0) {
                leafLight = packedLight; // Fallback to base light to prevent invisible black models
            }

            // Render cardinal directions (0 degrees)
            poseStack.pushPose();
            poseStack.translate(1.0, 8.0, 1.0);
            poseStack.scale(6.0f, 6.0f, 6.0f);
            poseStack.translate(-0.5, -0.5, -0.5);
            renderModel(dummyState, poseStack, buffer, leafLight, packedOverlay);
            poseStack.popPose();

            // Render diagonal directions (45 degrees)
            poseStack.pushPose();
            poseStack.translate(1.0, 8.0, 1.0);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45.0f));
            poseStack.scale(6.0f, 6.0f, 6.0f);
            poseStack.translate(-0.5, -0.5, -0.5);
            renderModel(dummyState, poseStack, buffer, leafLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private void renderModel(BlockState dummyState, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(dummyState);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.cutout()),
                dummyState,
                model,
                1.0f, 1.0f, 1.0f,
                packedLight,
                packedOverlay
        );
    }

    @Override
    public boolean shouldRenderOffScreen(SawitBlockEntity blockEntity) {
        // Bypass frustum culling — without this, only trees whose base block
        // is directly in view are rendered (canopy extends way beyond default 1x1x1 AABB).
        // Default vanilla view distance (usually 64 blocks) will handle the distance culling.
        return true;
    }
}
