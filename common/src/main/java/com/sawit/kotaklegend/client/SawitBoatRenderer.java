package com.sawit.kotaklegend.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.vehicle.boat.Boat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;

import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.state.BoatRenderState;

public class SawitBoatRenderer extends AbstractBoatRenderer {
    private final BoatModel boatModel;
    private final Identifier texture;
    private final net.minecraft.client.model.Model waterPatchModel;

    public SawitBoatRenderer(EntityRendererProvider.Context context, boolean hasChest) {
        super(context);
        this.boatModel = hasChest ? new BoatModel(context.bakeLayer(ModelLayers.OAK_CHEST_BOAT)) : new BoatModel(context.bakeLayer(ModelLayers.OAK_BOAT));
        this.texture = Identifier.fromNamespaceAndPath("sawitmod", hasChest ? "textures/entity/chest_boat/sawit.png" : "textures/entity/boat/sawit.png");
        this.waterPatchModel = new net.minecraft.client.model.Model.Simple(context.bakeLayer(ModelLayers.BOAT_WATER_PATCH), argx -> net.minecraft.client.renderer.rendertype.RenderTypes.waterMask());
    }

    @Override
    protected net.minecraft.client.model.EntityModel<BoatRenderState> model() {
        return this.boatModel;
    }

    @Override
    protected RenderType renderType() {
        return this.boatModel.renderType(this.texture);
    }

    @Override
    protected void submitTypeAdditions(BoatRenderState arg, PoseStack arg2, net.minecraft.client.renderer.SubmitNodeCollector arg3, int i) {
        if (!arg.isUnderWater) {
            arg3.submitModel(this.waterPatchModel, net.minecraft.util.Unit.INSTANCE, arg2, this.waterPatchModel.renderType(this.texture), i, OverlayTexture.NO_OVERLAY, -1, null);
        }
    }
}
