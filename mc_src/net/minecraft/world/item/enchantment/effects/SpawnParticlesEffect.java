package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public record SpawnParticlesEffect(
	ParticleOptions particle,
	SpawnParticlesEffect.PositionSource horizontalPosition,
	SpawnParticlesEffect.PositionSource verticalPosition,
	SpawnParticlesEffect.VelocitySource horizontalVelocity,
	SpawnParticlesEffect.VelocitySource verticalVelocity,
	FloatProvider speed
) implements EnchantmentEntityEffect {
	public static final MapCodec<SpawnParticlesEffect> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
				ParticleTypes.CODEC.fieldOf("particle").forGetter(SpawnParticlesEffect::particle),
				SpawnParticlesEffect.PositionSource.CODEC.fieldOf("horizontal_position").forGetter(SpawnParticlesEffect::horizontalPosition),
				SpawnParticlesEffect.PositionSource.CODEC.fieldOf("vertical_position").forGetter(SpawnParticlesEffect::verticalPosition),
				SpawnParticlesEffect.VelocitySource.CODEC.fieldOf("horizontal_velocity").forGetter(SpawnParticlesEffect::horizontalVelocity),
				SpawnParticlesEffect.VelocitySource.CODEC.fieldOf("vertical_velocity").forGetter(SpawnParticlesEffect::verticalVelocity),
				FloatProvider.CODEC.optionalFieldOf("speed", ConstantFloat.ZERO).forGetter(SpawnParticlesEffect::speed)
			)
			.apply(instance, SpawnParticlesEffect::new)
	);

	public static SpawnParticlesEffect.PositionSource offsetFromEntityPosition(float f) {
		return new SpawnParticlesEffect.PositionSource(SpawnParticlesEffect.PositionSourceType.ENTITY_POSITION, f, 1.0F);
	}

	public static SpawnParticlesEffect.PositionSource inBoundingBox() {
		return new SpawnParticlesEffect.PositionSource(SpawnParticlesEffect.PositionSourceType.BOUNDING_BOX, 0.0F, 1.0F);
	}

	public static SpawnParticlesEffect.VelocitySource movementScaled(float f) {
		return new SpawnParticlesEffect.VelocitySource(f, ConstantFloat.ZERO);
	}

	public static SpawnParticlesEffect.VelocitySource fixedVelocity(FloatProvider arg) {
		return new SpawnParticlesEffect.VelocitySource(0.0F, arg);
	}

	@Override
	public void apply(ServerLevel arg, int i, EnchantedItemInUse arg2, Entity arg3, Vec3 arg4) {
		RandomSource randomSource = arg3.getRandom();
		Vec3 vec3 = arg3.getKnownMovement();
		float f = arg3.getBbWidth();
		float g = arg3.getBbHeight();
		arg.sendParticles(
			this.particle,
			this.horizontalPosition.getCoordinate(arg4.x(), arg4.x(), f, randomSource),
			this.verticalPosition.getCoordinate(arg4.y(), arg4.y() + g / 2.0F, g, randomSource),
			this.horizontalPosition.getCoordinate(arg4.z(), arg4.z(), f, randomSource),
			0,
			this.horizontalVelocity.getVelocity(vec3.x(), randomSource),
			this.verticalVelocity.getVelocity(vec3.y(), randomSource),
			this.horizontalVelocity.getVelocity(vec3.z(), randomSource),
			this.speed.sample(randomSource)
		);
	}

	@Override
	public MapCodec<SpawnParticlesEffect> codec() {
		return CODEC;
	}

	public record PositionSource(SpawnParticlesEffect.PositionSourceType type, float offset, float scale) {
		public static final MapCodec<SpawnParticlesEffect.PositionSource> CODEC = RecordCodecBuilder.<SpawnParticlesEffect.PositionSource>mapCodec(
				instance -> instance.group(
						SpawnParticlesEffect.PositionSourceType.CODEC.fieldOf("type").forGetter(SpawnParticlesEffect.PositionSource::type),
						Codec.FLOAT.optionalFieldOf("offset", 0.0F).forGetter(SpawnParticlesEffect.PositionSource::offset),
						ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("scale", 1.0F).forGetter(SpawnParticlesEffect.PositionSource::scale)
					)
					.apply(instance, SpawnParticlesEffect.PositionSource::new)
			)
			.validate(
				arg -> arg.type() == SpawnParticlesEffect.PositionSourceType.ENTITY_POSITION && arg.scale() != 1.0F
					? DataResult.error(() -> "Cannot scale an entity position coordinate source")
					: DataResult.success(arg)
			);

		public double getCoordinate(double d, double e, float f, RandomSource arg) {
			return this.type.getCoordinate(d, e, f * this.scale, arg) + this.offset;
		}
	}

	public static enum PositionSourceType implements StringRepresentable {
		ENTITY_POSITION("entity_position", (d, e, f, arg) -> d),
		BOUNDING_BOX("in_bounding_box", (d, e, f, arg) -> e + (arg.nextDouble() - 0.5) * f);

		public static final Codec<SpawnParticlesEffect.PositionSourceType> CODEC = StringRepresentable.fromEnum(SpawnParticlesEffect.PositionSourceType::values);
		private final String id;
		private final SpawnParticlesEffect.PositionSourceType.CoordinateSource source;

		private PositionSourceType(String string2, SpawnParticlesEffect.PositionSourceType.CoordinateSource arg) {
			this.id = string2;
			this.source = arg;
		}

		public double getCoordinate(double d, double e, float f, RandomSource arg) {
			return this.source.getCoordinate(d, e, f, arg);
		}

		@Override
		public String getSerializedName() {
			return this.id;
		}

		@FunctionalInterface
		interface CoordinateSource {
			double getCoordinate(double d, double e, float f, RandomSource arg);
		}
	}

	public record VelocitySource(float movementScale, FloatProvider base) {
		public static final MapCodec<SpawnParticlesEffect.VelocitySource> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codec.FLOAT.optionalFieldOf("movement_scale", 0.0F).forGetter(SpawnParticlesEffect.VelocitySource::movementScale),
					FloatProvider.CODEC.optionalFieldOf("base", ConstantFloat.ZERO).forGetter(SpawnParticlesEffect.VelocitySource::base)
				)
				.apply(instance, SpawnParticlesEffect.VelocitySource::new)
		);

		public double getVelocity(double d, RandomSource arg) {
			return d * this.movementScale + this.base.sample(arg);
		}
	}
}
