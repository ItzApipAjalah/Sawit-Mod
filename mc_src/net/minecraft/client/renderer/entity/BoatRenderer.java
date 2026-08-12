package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.UnaryOperator;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BoatRenderer extends AbstractBoatRenderer {
	private final Model waterPatchModel;
	private final ResourceLocation texture;
	private final EntityModel<BoatRenderState> model;

	public BoatRenderer(EntityRendererProvider.Context arg, ModelLayerLocation arg2) {
		super(arg);
		this.texture = arg2.model().withPath((UnaryOperator<String>)(string -> "textures/entity/" + string + ".png"));
		this.waterPatchModel = new Model.Simple(arg.bakeLayer(ModelLayers.BOAT_WATER_PATCH), argx -> RenderType.waterMask());
		this.model = new BoatModel(arg.bakeLayer(arg2));
	}

	@Override
	protected EntityModel<BoatRenderState> model() {
		return this.model;
	}

	@Override
	protected RenderType renderType() {
		return this.model.renderType(this.texture);
	}

	@Override
	protected void renderTypeAdditions(BoatRenderState arg, PoseStack arg2, MultiBufferSource arg3, int i) {
		if (!arg.isUnderWater) {
			this.waterPatchModel.renderToBuffer(arg2, arg3.getBuffer(this.waterPatchModel.renderType(this.texture)), i, OverlayTexture.NO_OVERLAY);
		}
	}
}
