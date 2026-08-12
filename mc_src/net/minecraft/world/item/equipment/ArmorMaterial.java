package net.minecraft.world.item.equipment;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public record ArmorMaterial(
	int durability,
	Map<ArmorType, Integer> defense,
	int enchantmentValue,
	Holder<SoundEvent> equipSound,
	float toughness,
	float knockbackResistance,
	TagKey<Item> repairIngredient,
	ResourceLocation modelId
) {
	public Item.Properties humanoidProperties(Item.Properties arg, ArmorType arg2) {
		return arg.durability(arg2.getDurability(this.durability))
			.attributes(this.createAttributes(arg2))
			.enchantable(this.enchantmentValue)
			.component(DataComponents.EQUIPPABLE, Equippable.builder(arg2.getSlot()).setEquipSound(this.equipSound).setModel(this.modelId).build())
			.repairable(this.repairIngredient);
	}

	public Item.Properties animalProperties(Item.Properties arg, HolderSet<EntityType<?>> arg2) {
		return arg.durability(ArmorType.BODY.getDurability(this.durability))
			.attributes(this.createAttributes(ArmorType.BODY))
			.repairable(this.repairIngredient)
			.component(
				DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.BODY).setEquipSound(this.equipSound).setModel(this.modelId).setAllowedEntities(arg2).build()
			);
	}

	public Item.Properties animalProperties(Item.Properties arg, Holder<SoundEvent> arg2, boolean bl, HolderSet<EntityType<?>> arg3) {
		if (bl) {
			arg = arg.durability(ArmorType.BODY.getDurability(this.durability)).repairable(this.repairIngredient);
		}

		return arg.attributes(this.createAttributes(ArmorType.BODY))
			.component(
				DataComponents.EQUIPPABLE,
				Equippable.builder(EquipmentSlot.BODY).setEquipSound(arg2).setModel(this.modelId).setAllowedEntities(arg3).setDamageOnHurt(bl).build()
			);
	}

	private ItemAttributeModifiers createAttributes(ArmorType arg) {
		int i = (Integer)this.defense.getOrDefault(arg, 0);
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
		EquipmentSlotGroup equipmentSlotGroup = EquipmentSlotGroup.bySlot(arg.getSlot());
		ResourceLocation resourceLocation = ResourceLocation.withDefaultNamespace("armor." + arg.getName());
		builder.add(Attributes.ARMOR, new AttributeModifier(resourceLocation, i, AttributeModifier.Operation.ADD_VALUE), equipmentSlotGroup);
		builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(resourceLocation, this.toughness, AttributeModifier.Operation.ADD_VALUE), equipmentSlotGroup);
		if (this.knockbackResistance > 0.0F) {
			builder.add(
				Attributes.KNOCKBACK_RESISTANCE,
				new AttributeModifier(resourceLocation, this.knockbackResistance, AttributeModifier.Operation.ADD_VALUE),
				equipmentSlotGroup
			);
		}

		return builder.build();
	}
}
