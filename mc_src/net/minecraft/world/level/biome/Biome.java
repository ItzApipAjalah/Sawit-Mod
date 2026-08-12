package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

public final class Biome {
	public static final Codec<Biome> DIRECT_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				Biome.ClimateSettings.CODEC.forGetter(arg -> arg.modifiableBiomeInfo().getOriginalBiomeInfo().climateSettings()),
				BiomeSpecialEffects.CODEC.fieldOf("effects").forGetter(arg -> arg.modifiableBiomeInfo().getOriginalBiomeInfo().effects()),
				BiomeGenerationSettings.CODEC.forGetter(arg -> arg.generationSettings),
				MobSpawnSettings.CODEC.forGetter(arg -> arg.mobSettings)
			)
			.apply(instance, Biome::new)
	);
	public static final Codec<Biome> NETWORK_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				Biome.ClimateSettings.CODEC.forGetter(arg -> arg.climateSettings), BiomeSpecialEffects.CODEC.fieldOf("effects").forGetter(arg -> arg.specialEffects)
			)
			.apply(instance, (arg, arg2) -> new Biome(arg, arg2, BiomeGenerationSettings.EMPTY, MobSpawnSettings.EMPTY))
	);
	public static final Codec<Holder<Biome>> CODEC = RegistryFileCodec.create(Registries.BIOME, DIRECT_CODEC);
	public static final Codec<HolderSet<Biome>> LIST_CODEC = RegistryCodecs.homogeneousList(Registries.BIOME, DIRECT_CODEC);
	private static final PerlinSimplexNoise TEMPERATURE_NOISE = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(1234L)), ImmutableList.of(0));
	static final PerlinSimplexNoise FROZEN_TEMPERATURE_NOISE = new PerlinSimplexNoise(
		new WorldgenRandom(new LegacyRandomSource(3456L)), ImmutableList.of(-2, -1, 0)
	);
	@Deprecated(
		forRemoval = true
	)
	public static final PerlinSimplexNoise BIOME_INFO_NOISE = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(2345L)), ImmutableList.of(0));
	private static final int TEMPERATURE_CACHE_SIZE = 1024;
	private final Biome.ClimateSettings climateSettings;
	private final BiomeGenerationSettings generationSettings;
	private final MobSpawnSettings mobSettings;
	private final BiomeSpecialEffects specialEffects;
	private final ThreadLocal<Long2FloatLinkedOpenHashMap> temperatureCache = ThreadLocal.withInitial(() -> Util.make(() -> {
		Long2FloatLinkedOpenHashMap long2floatlinkedopenhashmap = new Long2FloatLinkedOpenHashMap(1024, 0.25F) {
			@Override
			protected void rehash(int i) {
			}
		};
		long2floatlinkedopenhashmap.defaultReturnValue(Float.NaN);
		return long2floatlinkedopenhashmap;
	}));
	private final ModifiableBiomeInfo modifiableBiomeInfo;

	Biome(Biome.ClimateSettings arg, BiomeSpecialEffects arg2, BiomeGenerationSettings arg3, MobSpawnSettings arg4) {
		this.climateSettings = arg;
		this.generationSettings = arg3;
		this.mobSettings = arg4;
		this.specialEffects = arg2;
		this.modifiableBiomeInfo = new ModifiableBiomeInfo(new ModifiableBiomeInfo.BiomeInfo(arg, arg2, arg3, arg4));
	}

	public int getSkyColor() {
		return this.specialEffects.getSkyColor();
	}

	public MobSpawnSettings getMobSettings() {
		return this.modifiableBiomeInfo().get().mobSpawnSettings();
	}

	public boolean hasPrecipitation() {
		return this.climateSettings.hasPrecipitation();
	}

	public Biome.Precipitation getPrecipitationAt(BlockPos arg, int i) {
		if (!this.hasPrecipitation()) {
			return Biome.Precipitation.NONE;
		} else {
			return this.coldEnoughToSnow(arg, i) ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN;
		}
	}

	private float getHeightAdjustedTemperature(BlockPos arg, int j) {
		float f = this.climateSettings.temperatureModifier.modifyTemperature(arg, this.getBaseTemperature());
		int i = j + 17;
		if (arg.getY() > i) {
			float f1 = (float)(TEMPERATURE_NOISE.getValue(arg.getX() / 8.0F, arg.getZ() / 8.0F, false) * 8.0);
			return f - (f1 + arg.getY() - i) * 0.05F / 40.0F;
		} else {
			return f;
		}
	}

	@Deprecated
	private float getTemperature(BlockPos arg, int j) {
		long i = arg.asLong();
		Long2FloatLinkedOpenHashMap long2floatlinkedopenhashmap = (Long2FloatLinkedOpenHashMap)this.temperatureCache.get();
		float f = long2floatlinkedopenhashmap.get(i);
		if (!Float.isNaN(f)) {
			return f;
		} else {
			float f1 = this.getHeightAdjustedTemperature(arg, j);
			if (long2floatlinkedopenhashmap.size() == 1024) {
				long2floatlinkedopenhashmap.removeFirstFloat();
			}

			long2floatlinkedopenhashmap.put(i, f1);
			return f1;
		}
	}

	public boolean shouldFreeze(LevelReader arg, BlockPos arg2) {
		return this.shouldFreeze(arg, arg2, true);
	}

	public boolean shouldFreeze(LevelReader arg, BlockPos arg2, boolean bl) {
		if (this.warmEnoughToRain(arg2, arg.getSeaLevel())) {
			return false;
		} else {
			if (arg.isInsideBuildHeight(arg2.getY()) && arg.getBrightness(LightLayer.BLOCK, arg2) < 10) {
				BlockState blockstate = arg.getBlockState(arg2);
				FluidState fluidstate = arg.getFluidState(arg2);
				if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
					if (!bl) {
						return true;
					}

					boolean flag = arg.isWaterAt(arg2.west()) && arg.isWaterAt(arg2.east()) && arg.isWaterAt(arg2.north()) && arg.isWaterAt(arg2.south());
					if (!flag) {
						return true;
					}
				}
			}

			return false;
		}
	}

	public boolean coldEnoughToSnow(BlockPos arg, int i) {
		return !this.warmEnoughToRain(arg, i);
	}

	public boolean warmEnoughToRain(BlockPos arg, int i) {
		return this.getTemperature(arg, i) >= 0.15F;
	}

	public boolean shouldMeltFrozenOceanIcebergSlightly(BlockPos arg, int i) {
		return this.getTemperature(arg, i) > 0.1F;
	}

	public boolean shouldSnow(LevelReader arg, BlockPos arg2) {
		if (this.warmEnoughToRain(arg2, arg.getSeaLevel())) {
			return false;
		} else {
			if (arg.isInsideBuildHeight(arg2.getY()) && arg.getBrightness(LightLayer.BLOCK, arg2) < 10) {
				BlockState blockstate = arg.getBlockState(arg2);
				if ((blockstate.isAir() || blockstate.is(Blocks.SNOW)) && Blocks.SNOW.defaultBlockState().canSurvive(arg, arg2)) {
					return true;
				}
			}

			return false;
		}
	}

	public BiomeGenerationSettings getGenerationSettings() {
		return this.modifiableBiomeInfo().get().generationSettings();
	}

	public int getFogColor() {
		return this.specialEffects.getFogColor();
	}

	public int getGrassColor(double d, double e) {
		int i = (Integer)this.specialEffects.getGrassColorOverride().orElseGet(this::getGrassColorFromTexture);
		return this.specialEffects.getGrassColorModifier().modifyColor(d, e, i);
	}

	private int getGrassColorFromTexture() {
		double d0 = Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
		double d1 = Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
		return GrassColor.get(d0, d1);
	}

	public int getFoliageColor() {
		return (Integer)this.specialEffects.getFoliageColorOverride().orElseGet(this::getFoliageColorFromTexture);
	}

	private int getFoliageColorFromTexture() {
		double d0 = Mth.clamp(this.climateSettings.temperature, 0.0F, 1.0F);
		double d1 = Mth.clamp(this.climateSettings.downfall, 0.0F, 1.0F);
		return FoliageColor.get(d0, d1);
	}

	public float getBaseTemperature() {
		return this.climateSettings.temperature;
	}

	public BiomeSpecialEffects getSpecialEffects() {
		return this.specialEffects;
	}

	public int getWaterColor() {
		return this.specialEffects.getWaterColor();
	}

	public int getWaterFogColor() {
		return this.specialEffects.getWaterFogColor();
	}

	public Optional<AmbientParticleSettings> getAmbientParticle() {
		return this.specialEffects.getAmbientParticleSettings();
	}

	public Optional<Holder<SoundEvent>> getAmbientLoop() {
		return this.specialEffects.getAmbientLoopSoundEvent();
	}

	public Optional<AmbientMoodSettings> getAmbientMood() {
		return this.specialEffects.getAmbientMoodSettings();
	}

	public Optional<AmbientAdditionsSettings> getAmbientAdditions() {
		return this.specialEffects.getAmbientAdditionsSettings();
	}

	public Optional<Music> getBackgroundMusic() {
		return this.specialEffects.getBackgroundMusic();
	}

	public ModifiableBiomeInfo modifiableBiomeInfo() {
		return this.modifiableBiomeInfo;
	}

	public Biome.ClimateSettings getModifiedClimateSettings() {
		return this.modifiableBiomeInfo().get().climateSettings();
	}

	public BiomeSpecialEffects getModifiedSpecialEffects() {
		return this.modifiableBiomeInfo().get().effects();
	}

	public static class BiomeBuilder {
		private boolean hasPrecipitation = true;
		@Nullable
		private Float temperature;
		private Biome.TemperatureModifier temperatureModifier = Biome.TemperatureModifier.NONE;
		@Nullable
		private Float downfall;
		@Nullable
		private BiomeSpecialEffects specialEffects;
		@Nullable
		private MobSpawnSettings mobSpawnSettings;
		@Nullable
		private BiomeGenerationSettings generationSettings;

		public Biome.BiomeBuilder hasPrecipitation(boolean bl) {
			this.hasPrecipitation = bl;
			return this;
		}

		public Biome.BiomeBuilder temperature(float f) {
			this.temperature = f;
			return this;
		}

		public Biome.BiomeBuilder downfall(float f) {
			this.downfall = f;
			return this;
		}

		public Biome.BiomeBuilder specialEffects(BiomeSpecialEffects arg) {
			this.specialEffects = arg;
			return this;
		}

		public Biome.BiomeBuilder mobSpawnSettings(MobSpawnSettings arg) {
			this.mobSpawnSettings = arg;
			return this;
		}

		public Biome.BiomeBuilder generationSettings(BiomeGenerationSettings arg) {
			this.generationSettings = arg;
			return this;
		}

		public Biome.BiomeBuilder temperatureAdjustment(Biome.TemperatureModifier arg) {
			this.temperatureModifier = arg;
			return this;
		}

		public Biome build() {
			if (this.temperature != null && this.downfall != null && this.specialEffects != null && this.mobSpawnSettings != null && this.generationSettings != null) {
				return new Biome(
					new Biome.ClimateSettings(this.hasPrecipitation, this.temperature, this.temperatureModifier, this.downfall),
					this.specialEffects,
					this.generationSettings,
					this.mobSpawnSettings
				);
			} else {
				throw new IllegalStateException("You are missing parameters to build a proper biome\n" + this);
			}
		}

		public String toString() {
			return "BiomeBuilder{\nhasPrecipitation="
				+ this.hasPrecipitation
				+ ",\ntemperature="
				+ this.temperature
				+ ",\ntemperatureModifier="
				+ this.temperatureModifier
				+ ",\ndownfall="
				+ this.downfall
				+ ",\nspecialEffects="
				+ this.specialEffects
				+ ",\nmobSpawnSettings="
				+ this.mobSpawnSettings
				+ ",\ngenerationSettings="
				+ this.generationSettings
				+ ",\n}";
		}
	}

	public record ClimateSettings(boolean hasPrecipitation, float temperature, Biome.TemperatureModifier temperatureModifier, float downfall) {
		public static final MapCodec<Biome.ClimateSettings> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codec.BOOL.fieldOf("has_precipitation").forGetter(arg -> arg.hasPrecipitation),
					Codec.FLOAT.fieldOf("temperature").forGetter(arg -> arg.temperature),
					Biome.TemperatureModifier.CODEC.optionalFieldOf("temperature_modifier", Biome.TemperatureModifier.NONE).forGetter(arg -> arg.temperatureModifier),
					Codec.FLOAT.fieldOf("downfall").forGetter(arg -> arg.downfall)
				)
				.apply(instance, Biome.ClimateSettings::new)
		);
	}

	public static enum Precipitation implements StringRepresentable {
		NONE("none"),
		RAIN("rain"),
		SNOW("snow");

		public static final Codec<Biome.Precipitation> CODEC = StringRepresentable.fromEnum(Biome.Precipitation::values);
		private final String name;

		private Precipitation(String string2) {
			this.name = string2;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public static enum TemperatureModifier implements StringRepresentable {
		NONE("none") {
			@Override
			public float modifyTemperature(BlockPos arg, float f) {
				return f;
			}
		},
		FROZEN("frozen") {
			@Override
			public float modifyTemperature(BlockPos arg, float f) {
				double d0 = Biome.FROZEN_TEMPERATURE_NOISE.getValue(arg.getX() * 0.05, arg.getZ() * 0.05, false) * 7.0;
				double d1 = Biome.BIOME_INFO_NOISE.getValue(arg.getX() * 0.2, arg.getZ() * 0.2, false);
				double d2 = d0 + d1;
				if (d2 < 0.3) {
					double d3 = Biome.BIOME_INFO_NOISE.getValue(arg.getX() * 0.09, arg.getZ() * 0.09, false);
					if (d3 < 0.8) {
						return 0.2F;
					}
				}

				return f;
			}
		};

		private final String name;
		public static final Codec<Biome.TemperatureModifier> CODEC = StringRepresentable.fromEnum(Biome.TemperatureModifier::values);

		public abstract float modifyTemperature(BlockPos arg, float f);

		private TemperatureModifier(String string2) {
			this.name = string2;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}
}
