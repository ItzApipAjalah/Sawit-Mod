package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractBoatRenderer extends EntityRenderer<AbstractBoat, BoatRenderState> {
	public AbstractBoatRenderer(EntityRendererProvider.Context arg) {
		super(arg);
		this.shadowRadius = 0.8F;
	}

	public void render(BoatRenderState arg, PoseStack arg2, MultiBufferSource arg3, int i) {
		arg2.pushPose();
		arg2.translate(0.0F, 0.375F, 0.0F);
		arg2.mulPose(Axis.YP.rotationDegrees(180.0F - arg.yRot));
		float f = arg.hurtTime;
		if (f > 0.0F) {
			arg2.mulPose(Axis.XP.rotationDegrees(Mth.sin(f) * f * arg.damageTime / 10.0F * arg.hurtDir));
		}

		if (!Mth.equal(arg.bubbleAngle, 0.0F)) {
			arg2.mulPose(new Quaternionf().setAngleAxis(arg.bubbleAngle * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));
		}

		arg2.scale(-1.0F, -1.0F, 1.0F);
		arg2.mulPose(Axis.YP.rotationDegrees(90.0F));
		EntityModel<BoatRenderState> entityModel = this.model();
		entityModel.setupAnim(arg);
		VertexConsumer vertexConsumer = arg3.getBuffer(this.renderType());
		entityModel.renderToBuffer(arg2, vertexConsumer, i, OverlayTexture.NO_OVERLAY);
		this.renderTypeAdditions(arg, arg2, arg3, i);
		arg2.popPose();
		super.render(arg, arg2, arg3, i);
	}

	protected void renderTypeAdditions(BoatRenderState arg, PoseStack arg2, MultiBufferSource arg3, int i) {
	}

	protected abstract EntityModel<BoatRenderState> model();

	protected abstract RenderType renderType();

	public BoatRenderState createRenderState() {
		return new BoatRenderState();
	}

	public void extractRenderState(AbstractBoat arg, BoatRenderState arg2, float f) {
		super.extractRenderState(arg, arg2, f);
		arg2.yRot = arg.getYRot(f);
		arg2.hurtTime = arg.getHurtTime() - f;
		arg2.hurtDir = arg.getHurtDir();
		arg2.damageTime = Math.max(arg.getDamage() - f, 0.0F);
		arg2.bubbleAngle = arg.getBubbleAngle(f);
		arg2.isUnderWater = arg.isUnderWater();
		arg2.rowingTimeLeft = arg.getRowingTime(0, f);
		arg2.rowingTimeRight = arg.getRowingTime(1, f);
	}
}
