package net.minecraft.world.effect;

import com.google.common.collect.ComparisonChain;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

public class MobEffectInstance implements Comparable<MobEffectInstance> {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final int INFINITE_DURATION = -1;
	public static final int MIN_AMPLIFIER = 0;
	public static final int MAX_AMPLIFIER = 255;
	public static final Codec<MobEffectInstance> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				MobEffect.CODEC.fieldOf("id").forGetter(MobEffectInstance::getEffect), MobEffectInstance.Details.MAP_CODEC.forGetter(MobEffectInstance::asDetails)
			)
			.apply(instance, MobEffectInstance::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectInstance> STREAM_CODEC = StreamCodec.composite(
		MobEffect.STREAM_CODEC, MobEffectInstance::getEffect, MobEffectInstance.Details.STREAM_CODEC, MobEffectInstance::asDetails, MobEffectInstance::new
	);
	private final Holder<MobEffect> effect;
	private int duration;
	private int amplifier;
	private boolean ambient;
	private boolean visible;
	private boolean showIcon;
	@Nullable
	private MobEffectInstance hiddenEffect;
	private final MobEffectInstance.BlendState blendState = new MobEffectInstance.BlendState();

	public MobEffectInstance(Holder<MobEffect> arg) {
		this(arg, 0, 0);
	}

	public MobEffectInstance(Holder<MobEffect> arg, int i) {
		this(arg, i, 0);
	}

	public MobEffectInstance(Holder<MobEffect> arg, int i, int j) {
		this(arg, i, j, false, true);
	}

	public MobEffectInstance(Holder<MobEffect> arg, int i, int j, boolean bl, boolean bl2) {
		this(arg, i, j, bl, bl2, bl2);
	}

	public MobEffectInstance(Holder<MobEffect> arg, int i, int j, boolean bl, boolean bl2, boolean bl3) {
		this(arg, i, j, bl, bl2, bl3, null);
	}

	public MobEffectInstance(Holder<MobEffect> arg, int i, int j, boolean bl, boolean bl2, boolean bl3, @Nullable MobEffectInstance arg2) {
		this.effect = arg;
		this.duration = i;
		this.amplifier = Mth.clamp(j, 0, 255);
		this.ambient = bl;
		this.visible = bl2;
		this.showIcon = bl3;
		this.hiddenEffect = arg2;
	}

	public MobEffectInstance(MobEffectInstance arg) {
		this.effect = arg.effect;
		this.setDetailsFrom(arg);
	}

	private MobEffectInstance(Holder<MobEffect> arg, MobEffectInstance.Details arg2) {
		this(
			arg,
			arg2.duration(),
			arg2.amplifier(),
			arg2.ambient(),
			arg2.showParticles(),
			arg2.showIcon(),
			(MobEffectInstance)arg2.hiddenEffect().map(arg2x -> new MobEffectInstance(arg, arg2x)).orElse(null)
		);
	}

	private MobEffectInstance.Details asDetails() {
		return new MobEffectInstance.Details(
			this.getAmplifier(),
			this.getDuration(),
			this.isAmbient(),
			this.isVisible(),
			this.showIcon(),
			Optional.ofNullable(this.hiddenEffect).map(MobEffectInstance::asDetails)
		);
	}

	public float getBlendFactor(LivingEntity arg, float f) {
		return this.blendState.getFactor(arg, f);
	}

	public ParticleOptions getParticleOptions() {
		return this.effect.value().createParticleOptions(this);
	}

	void setDetailsFrom(MobEffectInstance arg) {
		this.duration = arg.duration;
		this.amplifier = arg.amplifier;
		this.ambient = arg.ambient;
		this.visible = arg.visible;
		this.showIcon = arg.showIcon;
	}

	public boolean update(MobEffectInstance arg) {
		if (!this.effect.equals(arg.effect)) {
			LOGGER.warn("This method should only be called for matching effects!");
		}

		boolean flag = false;
		if (arg.amplifier > this.amplifier) {
			if (arg.isShorterDurationThan(this)) {
				MobEffectInstance mobeffectinstance = this.hiddenEffect;
				this.hiddenEffect = new MobEffectInstance(this);
				this.hiddenEffect.hiddenEffect = mobeffectinstance;
			}

			this.amplifier = arg.amplifier;
			this.duration = arg.duration;
			flag = true;
		} else if (this.isShorterDurationThan(arg)) {
			if (arg.amplifier == this.amplifier) {
				this.duration = arg.duration;
				flag = true;
			} else if (this.hiddenEffect == null) {
				this.hiddenEffect = new MobEffectInstance(arg);
			} else {
				this.hiddenEffect.update(arg);
			}
		}

		if (!arg.ambient && this.ambient || flag) {
			this.ambient = arg.ambient;
			flag = true;
		}

		if (arg.visible != this.visible) {
			this.visible = arg.visible;
			flag = true;
		}

		if (arg.showIcon != this.showIcon) {
			this.showIcon = arg.showIcon;
			flag = true;
		}

		return flag;
	}

	private boolean isShorterDurationThan(MobEffectInstance arg) {
		return !this.isInfiniteDuration() && (this.duration < arg.duration || arg.isInfiniteDuration());
	}

	public boolean isInfiniteDuration() {
		return this.duration == -1;
	}

	public boolean endsWithin(int i) {
		return !this.isInfiniteDuration() && this.duration <= i;
	}

	public int mapDuration(Int2IntFunction int2IntFunction) {
		return !this.isInfiniteDuration() && this.duration != 0 ? int2IntFunction.applyAsInt(this.duration) : this.duration;
	}

	public Holder<MobEffect> getEffect() {
		return this.effect;
	}

	public int getDuration() {
		return this.duration;
	}

	public int getAmplifier() {
		return this.amplifier;
	}

	public boolean isAmbient() {
		return this.ambient;
	}

	public boolean isVisible() {
		return this.visible;
	}

	public boolean showIcon() {
		return this.showIcon;
	}

	public boolean tick(LivingEntity arg, Runnable runnable) {
		if (this.hasRemainingDuration()) {
			int i = this.isInfiniteDuration() ? arg.tickCount : this.duration;
			if (arg.level() instanceof ServerLevel serverlevel
				&& this.effect.value().shouldApplyEffectTickThisTick(i, this.amplifier)
				&& !this.effect.value().applyEffectTick(serverlevel, arg, this.amplifier)) {
				arg.removeEffect(this.effect);
			}

			this.tickDownDuration();
			if (this.duration == 0 && this.hiddenEffect != null) {
				this.setDetailsFrom(this.hiddenEffect);
				this.hiddenEffect = this.hiddenEffect.hiddenEffect;
				runnable.run();
			}
		}

		this.blendState.tick(this);
		return this.hasRemainingDuration();
	}

	private boolean hasRemainingDuration() {
		return this.isInfiniteDuration() || this.duration > 0;
	}

	private int tickDownDuration() {
		if (this.hiddenEffect != null) {
			this.hiddenEffect.tickDownDuration();
		}

		return this.duration = this.mapDuration(i -> i - 1);
	}

	public void onEffectStarted(LivingEntity arg) {
		this.effect.value().onEffectStarted(arg, this.amplifier);
	}

	public void onMobRemoved(ServerLevel arg, LivingEntity arg2, Entity.RemovalReason arg3) {
		this.effect.value().onMobRemoved(arg, arg2, this.amplifier, arg3);
	}

	public void onMobHurt(ServerLevel arg, LivingEntity arg2, DamageSource arg3, float f) {
		this.effect.value().onMobHurt(arg, arg2, this.amplifier, arg3, f);
	}

	public String getDescriptionId() {
		return this.effect.value().getDescriptionId();
	}

	public String toString() {
		String s;
		if (this.amplifier > 0) {
			s = this.getDescriptionId() + " x " + (this.amplifier + 1) + ", Duration: " + this.describeDuration();
		} else {
			s = this.getDescriptionId() + ", Duration: " + this.describeDuration();
		}

		if (!this.visible) {
			s = s + ", Particles: false";
		}

		if (!this.showIcon) {
			s = s + ", Show Icon: false";
		}

		return s;
	}

	private String describeDuration() {
		return this.isInfiniteDuration() ? "infinite" : Integer.toString(this.duration);
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else {
			return object instanceof MobEffectInstance mobeffectinstance
				? this.duration == mobeffectinstance.duration
					&& this.amplifier == mobeffectinstance.amplifier
					&& this.ambient == mobeffectinstance.ambient
					&& this.visible == mobeffectinstance.visible
					&& this.showIcon == mobeffectinstance.showIcon
					&& this.effect.equals(mobeffectinstance.effect)
				: false;
		}
	}

	public int hashCode() {
		int i = this.effect.hashCode();
		i = 31 * i + this.duration;
		i = 31 * i + this.amplifier;
		i = 31 * i + (this.ambient ? 1 : 0);
		i = 31 * i + (this.visible ? 1 : 0);
		return 31 * i + (this.showIcon ? 1 : 0);
	}

	public Tag save() {
		return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}

	@Nullable
	public static MobEffectInstance load(CompoundTag arg) {
		return (MobEffectInstance)CODEC.parse(NbtOps.INSTANCE, arg).resultOrPartial(LOGGER::error).orElse(null);
	}

	public int compareTo(MobEffectInstance arg) {
		int i = 32147;
		return this.getDuration() > 32147 && arg.getDuration() > 32147 || this.isAmbient() && arg.isAmbient()
			? ComparisonChain.start()
				.compare(this.isAmbient(), arg.isAmbient())
				.compare(this.getEffect().value().getSortOrder(this), arg.getEffect().value().getSortOrder(arg))
				.result()
			: ComparisonChain.start()
				.compareFalseFirst(this.isAmbient(), arg.isAmbient())
				.compareFalseFirst(this.isInfiniteDuration(), arg.isInfiniteDuration())
				.compare(this.getDuration(), arg.getDuration())
				.compare(this.getEffect().value().getSortOrder(this), arg.getEffect().value().getSortOrder(arg))
				.result();
	}

	public void onEffectAdded(LivingEntity arg) {
		this.effect.value().onEffectAdded(arg, this.amplifier);
	}

	public boolean is(Holder<MobEffect> arg) {
		return this.effect.equals(arg);
	}

	public void copyBlendState(MobEffectInstance arg) {
		this.blendState.copyFrom(arg.blendState);
	}

	public void skipBlending() {
		this.blendState.setImmediate(this);
	}

	static class BlendState {
		private float factor;
		private float factorPreviousFrame;

		public void setImmediate(MobEffectInstance arg) {
			this.factor = computeTarget(arg);
			this.factorPreviousFrame = this.factor;
		}

		public void copyFrom(MobEffectInstance.BlendState arg) {
			this.factor = arg.factor;
			this.factorPreviousFrame = arg.factorPreviousFrame;
		}

		public void tick(MobEffectInstance arg) {
			this.factorPreviousFrame = this.factor;
			int i = getBlendDuration(arg);
			if (i == 0) {
				this.factor = 1.0F;
			} else {
				float f = computeTarget(arg);
				if (this.factor != f) {
					float f1 = 1.0F / i;
					this.factor = this.factor + Mth.clamp(f - this.factor, -f1, f1);
				}
			}
		}

		private static float computeTarget(MobEffectInstance arg) {
			boolean flag = !arg.endsWithin(getBlendDuration(arg));
			return flag ? 1.0F : 0.0F;
		}

		private static int getBlendDuration(MobEffectInstance arg) {
			return arg.getEffect().value().getBlendDurationTicks();
		}

		public float getFactor(LivingEntity arg, float f) {
			if (arg.isRemoved()) {
				this.factorPreviousFrame = this.factor;
			}

			return Mth.lerp(f, this.factorPreviousFrame, this.factor);
		}
	}

	record Details(int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon, Optional<MobEffectInstance.Details> hiddenEffect) {
		public static final MapCodec<MobEffectInstance.Details> MAP_CODEC = MapCodec.recursive(
			"MobEffectInstance.Details",
			codec -> RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("amplifier", 0).forGetter(MobEffectInstance.Details::amplifier),
						Codec.INT.optionalFieldOf("duration", 0).forGetter(MobEffectInstance.Details::duration),
						Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffectInstance.Details::ambient),
						Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectInstance.Details::showParticles),
						Codec.BOOL.optionalFieldOf("show_icon").forGetter(arg -> Optional.of(arg.showIcon())),
						codec.optionalFieldOf("hidden_effect").forGetter(MobEffectInstance.Details::hiddenEffect)
					)
					.apply(instance, MobEffectInstance.Details::create)
			)
		);
		public static final StreamCodec<ByteBuf, MobEffectInstance.Details> STREAM_CODEC = StreamCodec.recursive(
			arg -> StreamCodec.composite(
				ByteBufCodecs.VAR_INT,
				MobEffectInstance.Details::amplifier,
				ByteBufCodecs.VAR_INT,
				MobEffectInstance.Details::duration,
				ByteBufCodecs.BOOL,
				MobEffectInstance.Details::ambient,
				ByteBufCodecs.BOOL,
				MobEffectInstance.Details::showParticles,
				ByteBufCodecs.BOOL,
				MobEffectInstance.Details::showIcon,
				arg.apply(ByteBufCodecs::optional),
				MobEffectInstance.Details::hiddenEffect,
				MobEffectInstance.Details::new
			)
		);

		private static MobEffectInstance.Details create(
			int i, int j, boolean bl, boolean bl2, Optional<Boolean> optional, Optional<MobEffectInstance.Details> optional2
		) {
			return new MobEffectInstance.Details(i, j, bl, bl2, (Boolean)optional.orElse(bl2), optional2);
		}
	}
}
