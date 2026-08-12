package net.minecraft.world.item.equipment;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public record EquipmentModel(Map<EquipmentModel.LayerType, List<EquipmentModel.Layer>> layers) {
	private static final Codec<List<EquipmentModel.Layer>> LAYER_LIST_CODEC = ExtraCodecs.nonEmptyList(EquipmentModel.Layer.CODEC.listOf());
	public static final Codec<EquipmentModel> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				ExtraCodecs.nonEmptyMap(Codec.unboundedMap(EquipmentModel.LayerType.CODEC, LAYER_LIST_CODEC)).fieldOf("layers").forGetter(EquipmentModel::layers)
			)
			.apply(instance, EquipmentModel::new)
	);

	public static EquipmentModel.Builder builder() {
		return new EquipmentModel.Builder();
	}

	public List<EquipmentModel.Layer> getLayers(EquipmentModel.LayerType arg) {
		return (List<EquipmentModel.Layer>)this.layers.getOrDefault(arg, List.of());
	}

	public static class Builder {
		private final Map<EquipmentModel.LayerType, List<EquipmentModel.Layer>> layersByType = new EnumMap(EquipmentModel.LayerType.class);

		Builder() {
		}

		public EquipmentModel.Builder addHumanoidLayers(ResourceLocation arg) {
			return this.addHumanoidLayers(arg, false);
		}

		public EquipmentModel.Builder addHumanoidLayers(ResourceLocation arg, boolean bl) {
			this.addLayers(EquipmentModel.LayerType.HUMANOID_LEGGINGS, EquipmentModel.Layer.leatherDyeable(arg, bl));
			this.addMainHumanoidLayer(arg, bl);
			return this;
		}

		public EquipmentModel.Builder addMainHumanoidLayer(ResourceLocation arg, boolean bl) {
			return this.addLayers(EquipmentModel.LayerType.HUMANOID, EquipmentModel.Layer.leatherDyeable(arg, bl));
		}

		public EquipmentModel.Builder addLayers(EquipmentModel.LayerType arg, EquipmentModel.Layer... args) {
			Collections.addAll((Collection)this.layersByType.computeIfAbsent(arg, argx -> new ArrayList()), args);
			return this;
		}

		public EquipmentModel build() {
			return new EquipmentModel(
				(Map<EquipmentModel.LayerType, List<EquipmentModel.Layer>>)this.layersByType
					.entrySet()
					.stream()
					.collect(ImmutableMap.toImmutableMap(Entry::getKey, entry -> List.copyOf((Collection)entry.getValue())))
			);
		}
	}

	public record Dyeable(Optional<Integer> colorWhenUndyed) {
		public static final Codec<EquipmentModel.Dyeable> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color_when_undyed").forGetter(EquipmentModel.Dyeable::colorWhenUndyed))
				.apply(instance, EquipmentModel.Dyeable::new)
		);
	}

	public record Layer(ResourceLocation textureId, Optional<EquipmentModel.Dyeable> dyeable, boolean usePlayerTexture) {
		public static final Codec<EquipmentModel.Layer> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					ResourceLocation.CODEC.fieldOf("texture").forGetter(EquipmentModel.Layer::textureId),
					EquipmentModel.Dyeable.CODEC.optionalFieldOf("dyeable").forGetter(EquipmentModel.Layer::dyeable),
					Codec.BOOL.optionalFieldOf("use_player_texture", false).forGetter(EquipmentModel.Layer::usePlayerTexture)
				)
				.apply(instance, EquipmentModel.Layer::new)
		);

		public Layer(ResourceLocation arg) {
			this(arg, Optional.empty(), false);
		}

		public static EquipmentModel.Layer leatherDyeable(ResourceLocation arg, boolean bl) {
			return new EquipmentModel.Layer(arg, bl ? Optional.of(new EquipmentModel.Dyeable(Optional.of(-6265536))) : Optional.empty(), false);
		}

		public static EquipmentModel.Layer onlyIfDyed(ResourceLocation arg, boolean bl) {
			return new EquipmentModel.Layer(arg, bl ? Optional.of(new EquipmentModel.Dyeable(Optional.empty())) : Optional.empty(), false);
		}

		public ResourceLocation getTextureLocation(EquipmentModel.LayerType arg) {
			return this.textureId.withPath((UnaryOperator<String>)(string -> "textures/entity/equipment/" + arg.getSerializedName() + "/" + string + ".png"));
		}
	}

	public static enum LayerType implements StringRepresentable {
		HUMANOID("humanoid"),
		HUMANOID_LEGGINGS("humanoid_leggings"),
		WINGS("wings"),
		WOLF_BODY("wolf_body"),
		HORSE_BODY("horse_body"),
		LLAMA_BODY("llama_body");

		public static final Codec<EquipmentModel.LayerType> CODEC = StringRepresentable.fromEnum(EquipmentModel.LayerType::values);
		private final String id;

		private LayerType(String string2) {
			this.id = string2;
		}

		@Override
		public String getSerializedName() {
			return this.id;
		}
	}
}
