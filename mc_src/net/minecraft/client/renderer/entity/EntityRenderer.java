package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.renderstate.RenderStateExtensions;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> {
	protected static final float NAMETAG_SCALE = 0.025F;
	public static final int LEASH_RENDER_STEPS = 24;
	protected final EntityRenderDispatcher entityRenderDispatcher;
	private final Font font;
	protected float shadowRadius;
	protected float shadowStrength = 1.0F;
	private final S reusedState = this.createRenderState();

	protected EntityRenderer(EntityRendererProvider.Context arg) {
		this.entityRenderDispatcher = arg.getEntityRenderDispatcher();
		this.font = arg.getFont();
	}

	public final int getPackedLightCoords(T arg, float f) {
		BlockPos blockpos = BlockPos.containing(arg.getLightProbePosition(f));
		return LightTexture.pack(this.getBlockLightLevel((T)arg, blockpos), this.getSkyLightLevel((T)arg, blockpos));
	}

	protected int getSkyLightLevel(T arg, BlockPos arg2) {
		return arg.level().getBrightness(LightLayer.SKY, arg2);
	}

	protected int getBlockLightLevel(T arg, BlockPos arg2) {
		return arg.isOnFire() ? 15 : arg.level().getBrightness(LightLayer.BLOCK, arg2);
	}

	public boolean shouldRender(T arg, Frustum arg2, double d, double e, double f) {
		if (!arg.shouldRender(d, e, f)) {
			return false;
		} else if (!this.affectedByCulling((T)arg)) {
			return true;
		} else {
			AABB aabb = this.getBoundingBoxForCulling((T)arg).inflate(0.5);
			if (aabb.hasNaN() || aabb.getSize() == 0.0) {
				aabb = new AABB(arg.getX() - 2.0, arg.getY() - 2.0, arg.getZ() - 2.0, arg.getX() + 2.0, arg.getY() + 2.0, arg.getZ() + 2.0);
			}

			if (arg2.isVisible(aabb)) {
				return true;
			} else {
				if (arg instanceof Leashable leashable) {
					Entity entity = leashable.getLeashHolder();
					if (entity != null) {
						return arg2.isVisible(this.entityRenderDispatcher.getRenderer(entity).getBoundingBoxForCulling(entity));
					}
				}

				return false;
			}
		}
	}

	protected AABB getBoundingBoxForCulling(T arg) {
		return arg.getBoundingBox();
	}

	protected boolean affectedByCulling(T arg) {
		return true;
	}

	public Vec3 getRenderOffset(S arg) {
		return arg.passengerOffset != null ? arg.passengerOffset : Vec3.ZERO;
	}

	public void render(S arg, PoseStack arg2, MultiBufferSource arg3, int i) {
		EntityRenderState.LeashState entityrenderstate$leashstate = arg.leashState;
		if (entityrenderstate$leashstate != null) {
			renderLeash(arg2, arg3, entityrenderstate$leashstate);
		}

		if (arg.nameTag != null) {
			RenderNameTagEvent.DoRender event = new RenderNameTagEvent.DoRender(arg, arg.nameTag, this, arg2, arg3, i, arg.partialTick);
			if (!NeoForge.EVENT_BUS.post(event).isCanceled()) {
				this.renderNameTag((S)arg, arg.nameTag, arg2, arg3, i);
			}
		}
	}

	private static void renderLeash(PoseStack arg, MultiBufferSource arg2, EntityRenderState.LeashState arg3) {
		float f = 0.025F;
		float f1 = (float)(arg3.end.x - arg3.start.x);
		float f2 = (float)(arg3.end.y - arg3.start.y);
		float f3 = (float)(arg3.end.z - arg3.start.z);
		float f4 = Mth.invSqrt(f1 * f1 + f3 * f3) * 0.025F / 2.0F;
		float f5 = f3 * f4;
		float f6 = f1 * f4;
		arg.pushPose();
		arg.translate(arg3.offset);
		VertexConsumer vertexconsumer = arg2.getBuffer(RenderType.leash());
		Matrix4f matrix4f = arg.last().pose();

		for (int i = 0; i <= 24; i++) {
			addVertexPair(
				vertexconsumer, matrix4f, f1, f2, f3, arg3.startBlockLight, arg3.endBlockLight, arg3.startSkyLight, arg3.endSkyLight, 0.025F, 0.025F, f5, f6, i, false
			);
		}

		for (int j = 24; j >= 0; j--) {
			addVertexPair(
				vertexconsumer, matrix4f, f1, f2, f3, arg3.startBlockLight, arg3.endBlockLight, arg3.startSkyLight, arg3.endSkyLight, 0.025F, 0.0F, f5, f6, j, true
			);
		}

		arg.popPose();
	}

	private static void addVertexPair(
		VertexConsumer arg, Matrix4f matrix4f, float g, float h, float l, int m, int n, int o, int p, float q, float r, float s, float t, int u, boolean bl
	) {
		float f = u / 24.0F;
		int i = (int)Mth.lerp(f, (float)m, (float)n);
		int j = (int)Mth.lerp(f, (float)o, (float)p);
		int k = LightTexture.pack(i, j);
		float f1 = u % 2 == (bl ? 1 : 0) ? 0.7F : 1.0F;
		float f2 = 0.5F * f1;
		float f3 = 0.4F * f1;
		float f4 = 0.3F * f1;
		float f5 = g * f;
		float f6 = h > 0.0F ? h * f * f : h - h * (1.0F - f) * (1.0F - f);
		float f7 = l * f;
		arg.addVertex(matrix4f, f5 - s, f6 + r, f7 + t).setColor(f2, f3, f4, 1.0F).setLight(k);
		arg.addVertex(matrix4f, f5 + s, f6 + q - r, f7 - t).setColor(f2, f3, f4, 1.0F).setLight(k);
	}

	protected boolean shouldShowName(T arg, double d) {
		return arg.shouldShowName() || arg.hasCustomName() && arg == this.entityRenderDispatcher.crosshairPickEntity;
	}

	public Font getFont() {
		return this.font;
	}

	protected void renderNameTag(S arg, Component arg2, PoseStack arg3, MultiBufferSource arg4, int k) {
		Vec3 vec3 = arg.nameTagAttachment;
		if (vec3 != null) {
			boolean flag = !arg.isDiscrete;
			int i = "deadmau5".equals(arg2.getString()) ? -10 : 0;
			arg3.pushPose();
			arg3.translate(vec3.x, vec3.y + 0.5, vec3.z);
			arg3.mulPose(this.entityRenderDispatcher.cameraOrientation());
			arg3.scale(0.025F, -0.025F, 0.025F);
			Matrix4f matrix4f = arg3.last().pose();
			Font font = this.getFont();
			float f = -font.width(arg2) / 2.0F;
			int j = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
			font.drawInBatch(arg2, f, (float)i, -2130706433, false, matrix4f, arg4, flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, j, k);
			if (flag) {
				font.drawInBatch(arg2, f, (float)i, -1, false, matrix4f, arg4, Font.DisplayMode.NORMAL, 0, LightTexture.lightCoordsWithEmission(k, 2));
			}

			arg3.popPose();
		}
	}

	@Nullable
	protected Component getNameTag(T arg) {
		return arg.getDisplayName();
	}

	protected float getShadowRadius(S arg) {
		return this.shadowRadius;
	}

	public abstract S createRenderState();

	public final S createRenderState(T arg, float f) {
		S s = this.reusedState;
		this.extractRenderState((T)arg, s, f);
		RenderStateExtensions.onUpdateEntityRenderState(this, (T)arg, s);
		return s;
	}

	public void extractRenderState(T arg, S arg2, float g) {
		arg2.x = Mth.lerp((double)g, arg.xOld, arg.getX());
		arg2.y = Mth.lerp((double)g, arg.yOld, arg.getY());
		arg2.z = Mth.lerp((double)g, arg.zOld, arg.getZ());
		arg2.isInvisible = arg.isInvisible();
		arg2.ageInTicks = arg.tickCount + g;
		arg2.boundingBoxWidth = arg.getBbWidth();
		arg2.boundingBoxHeight = arg.getBbHeight();
		arg2.eyeHeight = arg.getEyeHeight();
		if (arg.isPassenger()
			&& arg.getVehicle() instanceof AbstractMinecart abstractminecart
			&& abstractminecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior
			&& newminecartbehavior.cartHasPosRotLerp()) {
			double d2 = Mth.lerp((double)g, abstractminecart.xOld, abstractminecart.getX());
			double d0 = Mth.lerp((double)g, abstractminecart.yOld, abstractminecart.getY());
			double d1 = Mth.lerp((double)g, abstractminecart.zOld, abstractminecart.getZ());
			arg2.passengerOffset = newminecartbehavior.getCartLerpPosition(g).subtract(new Vec3(d2, d0, d1));
		} else {
			arg2.passengerOffset = null;
		}

		arg2.distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr(arg);
		RenderNameTagEvent.CanRender event = new RenderNameTagEvent.CanRender(arg, arg2, this.getNameTag((T)arg), this, g);
		NeoForge.EVENT_BUS.post(event);
		boolean flag = event.canRender().isTrue()
			|| event.canRender().isDefault() && arg2.distanceToCameraSq < 4096.0 && this.shouldShowName((T)arg, arg2.distanceToCameraSq);
		if (flag) {
			arg2.nameTag = event.getContent();
			arg2.nameTagAttachment = arg.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, arg.getYRot(g));
		} else {
			arg2.nameTag = null;
		}

		arg2.isDiscrete = arg.isDiscrete();
		Entity entity = arg instanceof Leashable leashable ? leashable.getLeashHolder() : null;
		if (entity != null) {
			float f = arg.getPreciseBodyRotation(g) * (float) (Math.PI / 180.0);
			Vec3 vec3 = arg.getLeashOffset(g).yRot(-f);
			BlockPos blockpos1 = BlockPos.containing(arg.getEyePosition(g));
			BlockPos blockpos = BlockPos.containing(entity.getEyePosition(g));
			if (arg2.leashState == null) {
				arg2.leashState = new EntityRenderState.LeashState();
			}

			EntityRenderState.LeashState entityrenderstate$leashstate = arg2.leashState;
			entityrenderstate$leashstate.offset = vec3;
			entityrenderstate$leashstate.start = arg.getPosition(g).add(vec3);
			entityrenderstate$leashstate.end = entity.getRopeHoldPosition(g);
			entityrenderstate$leashstate.startBlockLight = this.getBlockLightLevel((T)arg, blockpos1);
			entityrenderstate$leashstate.endBlockLight = this.entityRenderDispatcher.getRenderer(entity).getBlockLightLevel(entity, blockpos);
			entityrenderstate$leashstate.startSkyLight = arg.level().getBrightness(LightLayer.SKY, blockpos1);
			entityrenderstate$leashstate.endSkyLight = arg.level().getBrightness(LightLayer.SKY, blockpos);
		} else {
			arg2.leashState = null;
		}

		arg2.displayFireAnimation = arg.displayFireAnimation();
		arg2.partialTick = g;
	}
}
