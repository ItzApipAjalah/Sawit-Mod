package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;

public class MobSpawnSettings {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final float DEFAULT_CREATURE_SPAWN_PROBABILITY = 0.1F;
	public static final WeightedRandomList<MobSpawnSettings.SpawnerData> EMPTY_MOB_LIST = WeightedRandomList.create();
	public static final MobSpawnSettings EMPTY = new MobSpawnSettings.Builder().build();
	public static final MapCodec<MobSpawnSettings> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
				Codec.floatRange(0.0F, 0.9999999F).optionalFieldOf("creature_spawn_probability", 0.1F).forGetter(arg -> arg.creatureGenerationProbability),
				Codec.simpleMap(
						MobCategory.CODEC,
						WeightedRandomList.codec(MobSpawnSettings.SpawnerData.CODEC).promotePartial(Util.prefix("Spawn data: ", LOGGER::error)),
						StringRepresentable.keys(MobCategory.values())
					)
					.fieldOf("spawners")
					.forGetter(arg -> arg.spawners),
				Codec.simpleMap(BuiltInRegistries.ENTITY_TYPE.byNameCodec(), MobSpawnSettings.MobSpawnCost.CODEC, BuiltInRegistries.ENTITY_TYPE)
					.fieldOf("spawn_costs")
					.forGetter(arg -> arg.mobSpawnCosts)
			)
			.apply(instance, MobSpawnSettings::new)
	);
	private final float creatureGenerationProbability;
	private final Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> spawners;
	private final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts;
	private final Set<MobCategory> typesView;
	private final Set<EntityType<?>> costView;

	MobSpawnSettings(float f, Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> map, Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> map2) {
		this.creatureGenerationProbability = f;
		this.spawners = ImmutableMap.copyOf(map);
		this.mobSpawnCosts = ImmutableMap.copyOf(map2);
		this.typesView = Collections.unmodifiableSet(this.spawners.keySet());
		this.costView = Collections.unmodifiableSet(this.mobSpawnCosts.keySet());
	}

	public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobs(MobCategory arg) {
		return (WeightedRandomList<MobSpawnSettings.SpawnerData>)this.spawners.getOrDefault(arg, EMPTY_MOB_LIST);
	}

	public Set<MobCategory> getSpawnerTypes() {
		return this.typesView;
	}

	@Nullable
	public MobSpawnSettings.MobSpawnCost getMobSpawnCost(EntityType<?> arg) {
		return (MobSpawnSettings.MobSpawnCost)this.mobSpawnCosts.get(arg);
	}

	public Set<EntityType<?>> getEntityTypes() {
		return this.costView;
	}

	public float getCreatureProbability() {
		return this.creatureGenerationProbability;
	}

	public static class Builder {
		protected final Map<MobCategory, List<MobSpawnSettings.SpawnerData>> spawners = (Map<MobCategory, List<MobSpawnSettings.SpawnerData>>)Stream.of(
				MobCategory.values()
			)
			.collect(ImmutableMap.toImmutableMap(arg -> arg, arg -> Lists.newArrayList()));
		protected final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> mobSpawnCosts = Maps.<EntityType<?>, MobSpawnSettings.MobSpawnCost>newLinkedHashMap();
		protected float creatureGenerationProbability = 0.1F;

		public MobSpawnSettings.Builder addSpawn(MobCategory arg, MobSpawnSettings.SpawnerData arg2) {
			((List)this.spawners.get(arg)).add(arg2);
			return this;
		}

		public MobSpawnSettings.Builder addMobCharge(EntityType<?> arg, double d, double e) {
			this.mobSpawnCosts.put(arg, new MobSpawnSettings.MobSpawnCost(e, d));
			return this;
		}

		public MobSpawnSettings.Builder creatureGenerationProbability(float f) {
			this.creatureGenerationProbability = f;
			return this;
		}

		public MobSpawnSettings build() {
			return new MobSpawnSettings(
				this.creatureGenerationProbability,
				(Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>>)this.spawners
					.entrySet()
					.stream()
					.collect(ImmutableMap.toImmutableMap(Entry::getKey, entry -> WeightedRandomList.create((List)entry.getValue()))),
				ImmutableMap.copyOf(this.mobSpawnCosts)
			);
		}
	}

	public record MobSpawnCost(double energyBudget, double charge) {
		public static final Codec<MobSpawnSettings.MobSpawnCost> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Codec.DOUBLE.fieldOf("energy_budget").forGetter(arg -> arg.energyBudget), Codec.DOUBLE.fieldOf("charge").forGetter(arg -> arg.charge)
				)
				.apply(instance, MobSpawnSettings.MobSpawnCost::new)
		);
	}

	public static class SpawnerData extends WeightedEntry.IntrusiveBase {
		public static final Codec<MobSpawnSettings.SpawnerData> CODEC = RecordCodecBuilder.<MobSpawnSettings.SpawnerData>create(
				instance -> instance.group(
						BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(arg -> arg.type),
						Weight.CODEC.fieldOf("weight").forGetter(WeightedEntry.IntrusiveBase::getWeight),
						ExtraCodecs.POSITIVE_INT.fieldOf("minCount").forGetter(arg -> arg.minCount),
						ExtraCodecs.POSITIVE_INT.fieldOf("maxCount").forGetter(arg -> arg.maxCount)
					)
					.apply(instance, MobSpawnSettings.SpawnerData::new)
			)
			.validate(arg -> arg.minCount > arg.maxCount ? DataResult.error(() -> "minCount needs to be smaller or equal to maxCount") : DataResult.success(arg));
		public final EntityType<?> type;
		public final int minCount;
		public final int maxCount;

		public SpawnerData(EntityType<?> arg, int i, int j, int k) {
			this(arg, Weight.of(i), j, k);
		}

		public SpawnerData(EntityType<?> arg, Weight arg2, int i, int j) {
			super(arg2);
			this.type = arg.getCategory() == MobCategory.MISC ? EntityType.PIG : arg;
			this.minCount = i;
			this.maxCount = j;
		}

		public String toString() {
			return EntityType.getKey(this.type) + "*(" + this.minCount + "-" + this.maxCount + "):" + this.getWeight();
		}
	}
}
