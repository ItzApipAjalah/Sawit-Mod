package com.sawit.kotaklegend.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.vehicle.Boat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;

public class SawitBoatRenderer extends EntityRenderer<Boat> {
    private final BoatModel boatModel;
    private final ResourceLocation texture;

    public SawitBoatRenderer(EntityRendererProvider.Context context, boolean hasChest) {
        super(context);
        this.shadowRadius = 0.8F;
        this.boatModel = hasChest ? new ChestBoatModel(context.bakeLayer(ModelLayers.createChestBoatModelName(Boat.Type.OAK))) : new BoatModel(context.bakeLayer(ModelLayers.createBoatModelName(Boat.Type.OAK)));
        this.texture = new ResourceLocation("sawitmod", hasChest ? "textures/entity/chest_boat/sawit.png" : "textures/entity/boat/sawit.png");
    }

    @Override
    public ResourceLocation getTextureLocation(Boat entity) {
        return texture;
    }

    @Override
    public void render(Boat entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.375D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        float h = (float)entity.getHurtTime() - partialTicks;
        float j = entity.getDamage() - partialTicks;
        if (j < 0.0F) j = 0.0F;
        if (h > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(h) * h * j / 10.0F * (float)entity.getHurtDir()));
        }
        float k = entity.getBubbleAngle(partialTicks);
        if (!Mth.equal(k, 0.0F)) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getBubbleAngle(partialTicks)));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        this.boatModel.setupAnim(entity, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F);
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = buffer.getBuffer(this.boatModel.renderType(this.getTextureLocation(entity)));
        this.boatModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        if (!entity.isUnderWater()) {
            com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer2 = buffer.getBuffer(RenderType.waterMask());
            this.boatModel.waterPatch().render(poseStack, vertexConsumer2, packedLight, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
