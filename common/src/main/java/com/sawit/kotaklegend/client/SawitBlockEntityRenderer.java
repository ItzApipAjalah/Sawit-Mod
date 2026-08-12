package com.sawit.kotaklegend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sawit.kotaklegend.block.SawitBlock;
import com.sawit.kotaklegend.block.SawitBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SawitBlockEntityRenderer implements BlockEntityRenderer<SawitBlockEntity, SawitBlockEntityRenderer.SawitRenderState> {
    
    public static class SawitRenderState extends BlockEntityRenderState {
        public boolean renderLeaves;
        public BlockState dummyState;
        public int leafLight;
    }

    public SawitBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public SawitRenderState createRenderState() {
        return new SawitRenderState();
    }

    @Override
    public void extractRenderState(SawitBlockEntity blockEntity, SawitRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
        
        net.minecraft.world.level.Level level = blockEntity.getLevel();
        if (level == null) {
            state.renderLeaves = false;
            return;
        }
        BlockState blockState = blockEntity.getBlockState();
        if (blockState.getBlock() instanceof SawitBlock && blockState.getValue(SawitBlock.AGE) >= 4) {
            state.renderLeaves = true;
            state.dummyState = blockState.getValue(SawitBlock.AGE) == 5 ?
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_LEAVES_FRUIT_DUMMY.get().defaultBlockState() :
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_LEAVES_DUMMY.get().defaultBlockState();
            
            net.minecraft.core.BlockPos leafPos = blockEntity.getBlockPos().above(11);
            int leafLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, leafPos);
            if (leafLight <= 0) {
                leafLight = state.lightCoords;
            }
            state.leafLight = leafLight;
        } else {
            state.renderLeaves = false;
        }
    }

    @Override
    public void submit(SawitRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (!state.renderLeaves) return;

        net.minecraft.client.renderer.block.model.BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state.dummyState);
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS));

        poseStack.pushPose();
        poseStack.translate(1.0, 8.0, 1.0);
        poseStack.scale(6.0f, 6.0f, 6.0f);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), vertexConsumer, model, 1.0F, 1.0F, 1.0F, state.leafLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(1.0, 8.0, 1.0);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45.0f));
        poseStack.scale(6.0f, 6.0f, 6.0f);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), vertexConsumer, model, 1.0F, 1.0F, 1.0F, state.leafLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
