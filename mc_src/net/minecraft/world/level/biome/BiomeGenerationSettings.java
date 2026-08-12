package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;

public class BiomeGenerationSettings {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final BiomeGenerationSettings EMPTY = new BiomeGenerationSettings(HolderSet.direct(), List.of());
	public static final MapCodec<BiomeGenerationSettings> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
				ConfiguredWorldCarver.LIST_CODEC.promotePartial(Util.prefix("Carver: ", LOGGER::error)).fieldOf("carvers").forGetter(arg -> arg.carvers),
				PlacedFeature.LIST_OF_LISTS_CODEC.promotePartial(Util.prefix("Features: ", LOGGER::error)).fieldOf("features").forGetter(arg -> arg.features)
			)
			.apply(instance, BiomeGenerationSettings::new)
	);
	private final HolderSet<ConfiguredWorldCarver<?>> carvers;
	private final List<HolderSet<PlacedFeature>> features;
	private final Supplier<List<ConfiguredFeature<?, ?>>> flowerFeatures;
	private final Supplier<Set<PlacedFeature>> featureSet;

	public BiomeGenerationSettings(HolderSet<ConfiguredWorldCarver<?>> arg, List<HolderSet<PlacedFeature>> list) {
		this.carvers = arg;
		this.features = list;
		this.flowerFeatures = Suppliers.memoize(
			() -> (List<ConfiguredFeature<?, ?>>)list.stream()
				.flatMap(HolderSet::stream)
				.map(Holder::value)
				.flatMap(PlacedFeature::getFeatures)
				.filter(argx -> argx.feature() == Feature.FLOWER)
				.collect(ImmutableList.toImmutableList())
		);
		this.featureSet = Suppliers.memoize(() -> (Set<PlacedFeature>)list.stream().flatMap(HolderSet::stream).map(Holder::value).collect(Collectors.toSet()));
	}

	public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers() {
		return this.carvers;
	}

	public List<ConfiguredFeature<?, ?>> getFlowerFeatures() {
		return (List<ConfiguredFeature<?, ?>>)this.flowerFeatures.get();
	}

	public List<HolderSet<PlacedFeature>> features() {
		return this.features;
	}

	public boolean hasFeature(PlacedFeature arg) {
		return ((Set)this.featureSet.get()).contains(arg);
	}

	public static class Builder extends BiomeGenerationSettings.PlainBuilder {
		private final HolderGetter<PlacedFeature> placedFeatures;
		private final HolderGetter<ConfiguredWorldCarver<?>> worldCarvers;

		public Builder(HolderGetter<PlacedFeature> arg, HolderGetter<ConfiguredWorldCarver<?>> arg2) {
			this.placedFeatures = arg;
			this.worldCarvers = arg2;
		}

		public BiomeGenerationSettings.Builder addFeature(GenerationStep.Decoration arg, ResourceKey<PlacedFeature> arg2) {
			this.addFeature(arg.ordinal(), this.placedFeatures.getOrThrow(arg2));
			return this;
		}

		public BiomeGenerationSettings.Builder addCarver(ResourceKey<ConfiguredWorldCarver<?>> arg) {
			this.addCarver(this.worldCarvers.getOrThrow(arg));
			return this;
		}
	}

	public static class PlainBuilder {
		protected final List<Holder<ConfiguredWorldCarver<?>>> carvers = new ArrayList();
		protected final List<List<Holder<PlacedFeature>>> features = new ArrayList();

		public BiomeGenerationSettings.PlainBuilder addFeature(GenerationStep.Decoration arg, Holder<PlacedFeature> arg2) {
			return this.addFeature(arg.ordinal(), arg2);
		}

		public BiomeGenerationSettings.PlainBuilder addFeature(int i, Holder<PlacedFeature> arg) {
			this.addFeatureStepsUpTo(i);
			((List)this.features.get(i)).add(arg);
			return this;
		}

		public BiomeGenerationSettings.PlainBuilder addCarver(Holder<ConfiguredWorldCarver<?>> arg) {
			this.carvers.add(arg);
			return this;
		}

		protected void addFeatureStepsUpTo(int i) {
			while (this.features.size() <= i) {
				this.features.add(Lists.newArrayList());
			}
		}

		public BiomeGenerationSettings build() {
			return new BiomeGenerationSettings(
				HolderSet.direct(this.carvers), (List<HolderSet<PlacedFeature>>)this.features.stream().map(HolderSet::direct).collect(ImmutableList.toImmutableList())
			);
		}
	}
}
