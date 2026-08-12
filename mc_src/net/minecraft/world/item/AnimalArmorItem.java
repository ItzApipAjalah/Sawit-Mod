package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.equipment.ArmorMaterial;

public class AnimalArmorItem extends Item {
	private final AnimalArmorItem.BodyType bodyType;

	public AnimalArmorItem(ArmorMaterial arg, AnimalArmorItem.BodyType arg2, Item.Properties arg3) {
		super(arg.animalProperties(arg3, arg2.allowedEntities));
		this.bodyType = arg2;
	}

	public AnimalArmorItem(ArmorMaterial arg, AnimalArmorItem.BodyType arg2, Holder<SoundEvent> arg3, boolean bl, Item.Properties arg4) {
		super(arg.animalProperties(arg4, arg3, bl, arg2.allowedEntities));
		this.bodyType = arg2;
	}

	@Override
	public SoundEvent getBreakingSound() {
		return this.bodyType.breakingSound;
	}

	public static enum BodyType {
		EQUESTRIAN(SoundEvents.ITEM_BREAK, EntityType.HORSE),
		CANINE(SoundEvents.WOLF_ARMOR_BREAK, EntityType.WOLF);

		final SoundEvent breakingSound;
		final HolderSet<EntityType<?>> allowedEntities;

		private BodyType(SoundEvent arg, EntityType<?>... args) {
			this.breakingSound = arg;
			this.allowedEntities = HolderSet.direct(EntityType::builtInRegistryHolder, args);
		}
	}
}
