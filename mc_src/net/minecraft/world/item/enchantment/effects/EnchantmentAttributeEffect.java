package net.minecraft.world.item.enchantment.effects;

import com.google.common.collect.HashMultimap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record EnchantmentAttributeEffect(ResourceLocation id, Holder<Attribute> attribute, LevelBasedValue amount, AttributeModifier.Operation operation)
	implements EnchantmentLocationBasedEffect {
	public static final MapCodec<EnchantmentAttributeEffect> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("id").forGetter(EnchantmentAttributeEffect::id),
				Attribute.CODEC.fieldOf("attribute").forGetter(EnchantmentAttributeEffect::attribute),
				LevelBasedValue.CODEC.fieldOf("amount").forGetter(EnchantmentAttributeEffect::amount),
				AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(EnchantmentAttributeEffect::operation)
			)
			.apply(instance, EnchantmentAttributeEffect::new)
	);

	private ResourceLocation idForSlot(StringRepresentable arg) {
		return this.id.withSuffix("/" + arg.getSerializedName());
	}

	public AttributeModifier getModifier(int i, StringRepresentable arg) {
		return new AttributeModifier(this.idForSlot(arg), this.amount().calculate(i), this.operation());
	}

	@Override
	public void onChangedBlock(ServerLevel arg, int i, EnchantedItemInUse arg2, Entity arg3, Vec3 arg4, boolean bl) {
		if (bl && arg3 instanceof LivingEntity livingEntity) {
			livingEntity.getAttributes().addTransientAttributeModifiers(this.makeAttributeMap(i, arg2.inSlot()));
		}
	}

	@Override
	public void onDeactivated(EnchantedItemInUse arg, Entity arg2, Vec3 arg3, int i) {
		if (arg2 instanceof LivingEntity livingEntity) {
			livingEntity.getAttributes().removeAttributeModifiers(this.makeAttributeMap(i, arg.inSlot()));
		}
	}

	private HashMultimap<Holder<Attribute>, AttributeModifier> makeAttributeMap(int i, EquipmentSlot arg) {
		HashMultimap<Holder<Attribute>, AttributeModifier> hashMultimap = HashMultimap.create();
		hashMultimap.put(this.attribute, this.getModifier(i, arg));
		return hashMultimap;
	}

	@Override
	public MapCodec<EnchantmentAttributeEffect> codec() {
		return CODEC;
	}
}
