package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record ItemAttributeModifiers(List<ItemAttributeModifiers.Entry> modifiers, boolean showInTooltip) {
	public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of(), true);
	private static final Codec<ItemAttributeModifiers> FULL_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				ItemAttributeModifiers.Entry.CODEC.listOf().fieldOf("modifiers").forGetter(ItemAttributeModifiers::modifiers),
				Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(ItemAttributeModifiers::showInTooltip)
			)
			.apply(instance, ItemAttributeModifiers::new)
	);
	public static final Codec<ItemAttributeModifiers> CODEC = Codec.withAlternative(
		FULL_CODEC, ItemAttributeModifiers.Entry.CODEC.listOf(), list -> new ItemAttributeModifiers(list, true)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers> STREAM_CODEC = StreamCodec.composite(
		ItemAttributeModifiers.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
		ItemAttributeModifiers::modifiers,
		ByteBufCodecs.BOOL,
		ItemAttributeModifiers::showInTooltip,
		ItemAttributeModifiers::new
	);
	public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = Util.make(
		new DecimalFormat("#.##"), decimalFormat -> decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT))
	);

	public ItemAttributeModifiers withTooltip(boolean bl) {
		return new ItemAttributeModifiers(this.modifiers, bl);
	}

	public static ItemAttributeModifiers.Builder builder() {
		return new ItemAttributeModifiers.Builder();
	}

	public ItemAttributeModifiers withModifierAdded(Holder<Attribute> arg, AttributeModifier arg2, EquipmentSlotGroup arg3) {
		ImmutableList.Builder<ItemAttributeModifiers.Entry> builder = ImmutableList.builderWithExpectedSize(this.modifiers.size() + 1);

		for (ItemAttributeModifiers.Entry entry : this.modifiers) {
			if (!entry.matches(arg, arg2.id())) {
				builder.add(entry);
			}
		}

		builder.add(new ItemAttributeModifiers.Entry(arg, arg2, arg3));
		return new ItemAttributeModifiers(builder.build(), this.showInTooltip);
	}

	public void forEach(EquipmentSlotGroup arg, BiConsumer<Holder<Attribute>, AttributeModifier> biConsumer) {
		for (ItemAttributeModifiers.Entry entry : this.modifiers) {
			if (entry.slot.equals(arg)) {
				biConsumer.accept(entry.attribute, entry.modifier);
			}
		}
	}

	public void forEach(EquipmentSlot arg, BiConsumer<Holder<Attribute>, AttributeModifier> biConsumer) {
		for (ItemAttributeModifiers.Entry entry : this.modifiers) {
			if (entry.slot.test(arg)) {
				biConsumer.accept(entry.attribute, entry.modifier);
			}
		}
	}

	public double compute(double d, EquipmentSlot arg) {
		double e = d;

		for (ItemAttributeModifiers.Entry entry : this.modifiers) {
			if (entry.slot.test(arg)) {
				double f = entry.modifier.amount();

				e += switch (entry.modifier.operation()) {
					case ADD_VALUE -> f;
					case ADD_MULTIPLIED_BASE -> f * d;
					case ADD_MULTIPLIED_TOTAL -> f * e;
				};
			}
		}

		return e;
	}

	public static class Builder {
		private final ImmutableList.Builder<ItemAttributeModifiers.Entry> entries = ImmutableList.builder();

		Builder() {
		}

		public ItemAttributeModifiers.Builder add(Holder<Attribute> arg, AttributeModifier arg2, EquipmentSlotGroup arg3) {
			this.entries.add(new ItemAttributeModifiers.Entry(arg, arg2, arg3));
			return this;
		}

		public ItemAttributeModifiers build() {
			return new ItemAttributeModifiers(this.entries.build(), true);
		}
	}

	public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
		public static final Codec<ItemAttributeModifiers.Entry> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Attribute.CODEC.fieldOf("type").forGetter(ItemAttributeModifiers.Entry::attribute),
					AttributeModifier.MAP_CODEC.forGetter(ItemAttributeModifiers.Entry::modifier),
					EquipmentSlotGroup.CODEC.optionalFieldOf("slot", EquipmentSlotGroup.ANY).forGetter(ItemAttributeModifiers.Entry::slot)
				)
				.apply(instance, ItemAttributeModifiers.Entry::new)
		);
		public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Entry> STREAM_CODEC = StreamCodec.composite(
			Attribute.STREAM_CODEC,
			ItemAttributeModifiers.Entry::attribute,
			AttributeModifier.STREAM_CODEC,
			ItemAttributeModifiers.Entry::modifier,
			EquipmentSlotGroup.STREAM_CODEC,
			ItemAttributeModifiers.Entry::slot,
			ItemAttributeModifiers.Entry::new
		);

		public boolean matches(Holder<Attribute> arg, ResourceLocation arg2) {
			return arg.equals(this.attribute) && this.modifier.is(arg2);
		}
	}
}
