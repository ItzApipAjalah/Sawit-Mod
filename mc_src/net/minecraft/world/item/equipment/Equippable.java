package net.minecraft.world.item.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public record Equippable(
	EquipmentSlot slot,
	Holder<SoundEvent> equipSound,
	Optional<ResourceLocation> model,
	Optional<ResourceLocation> cameraOverlay,
	Optional<HolderSet<EntityType<?>>> allowedEntities,
	boolean dispensable,
	boolean swappable,
	boolean damageOnHurt
) {
	public static final Codec<Equippable> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				EquipmentSlot.CODEC.fieldOf("slot").forGetter(Equippable::slot),
				SoundEvent.CODEC.optionalFieldOf("equip_sound", SoundEvents.ARMOR_EQUIP_GENERIC).forGetter(Equippable::equipSound),
				ResourceLocation.CODEC.optionalFieldOf("model").forGetter(Equippable::model),
				ResourceLocation.CODEC.optionalFieldOf("camera_overlay").forGetter(Equippable::cameraOverlay),
				RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("allowed_entities").forGetter(Equippable::allowedEntities),
				Codec.BOOL.optionalFieldOf("dispensable", true).forGetter(Equippable::dispensable),
				Codec.BOOL.optionalFieldOf("swappable", true).forGetter(Equippable::swappable),
				Codec.BOOL.optionalFieldOf("damage_on_hurt", true).forGetter(Equippable::damageOnHurt)
			)
			.apply(instance, Equippable::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, Equippable> STREAM_CODEC = StreamCodec.composite(
		EquipmentSlot.STREAM_CODEC,
		Equippable::slot,
		SoundEvent.STREAM_CODEC,
		Equippable::equipSound,
		ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional),
		Equippable::model,
		ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional),
		Equippable::cameraOverlay,
		ByteBufCodecs.holderSet(Registries.ENTITY_TYPE).apply(ByteBufCodecs::optional),
		Equippable::allowedEntities,
		ByteBufCodecs.BOOL,
		Equippable::dispensable,
		ByteBufCodecs.BOOL,
		Equippable::swappable,
		ByteBufCodecs.BOOL,
		Equippable::damageOnHurt,
		Equippable::new
	);

	public static Equippable llamaSwag(DyeColor arg) {
		return builder(EquipmentSlot.BODY)
			.setEquipSound(SoundEvents.LLAMA_SWAG)
			.setModel((ResourceLocation)EquipmentModels.CARPETS.get(arg))
			.setAllowedEntities(EntityType.LLAMA, EntityType.TRADER_LLAMA)
			.build();
	}

	public static Equippable.Builder builder(EquipmentSlot arg) {
		return new Equippable.Builder(arg);
	}

	public InteractionResult swapWithEquipmentSlot(ItemStack arg, Player arg2) {
		if (!arg2.canUseSlot(this.slot)) {
			return InteractionResult.PASS;
		} else {
			ItemStack itemStack = arg2.getItemBySlot(this.slot);
			if ((!EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || arg2.isCreative())
				&& !ItemStack.isSameItemSameComponents(arg, itemStack)) {
				if (!arg2.level().isClientSide()) {
					arg2.awardStat(Stats.ITEM_USED.get(arg.getItem()));
				}

				if (arg.getCount() <= 1) {
					ItemStack itemStack2 = itemStack.isEmpty() ? arg : itemStack.copyAndClear();
					ItemStack itemStack3 = arg2.isCreative() ? arg.copy() : arg.copyAndClear();
					arg2.setItemSlot(this.slot, itemStack3);
					return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack2);
				} else {
					ItemStack itemStack2 = itemStack.copyAndClear();
					ItemStack itemStack3 = arg.consumeAndReturn(1, arg2);
					arg2.setItemSlot(this.slot, itemStack3);
					if (!arg2.getInventory().add(itemStack2)) {
						arg2.drop(itemStack2, false);
					}

					return InteractionResult.SUCCESS.heldItemTransformedTo(arg);
				}
			} else {
				return InteractionResult.FAIL;
			}
		}
	}

	public boolean canBeEquippedBy(EntityType<?> arg) {
		return this.allowedEntities.isEmpty() || ((HolderSet)this.allowedEntities.get()).contains(arg.builtInRegistryHolder());
	}

	public static class Builder {
		private final EquipmentSlot slot;
		private Holder<SoundEvent> equipSound = SoundEvents.ARMOR_EQUIP_GENERIC;
		private Optional<ResourceLocation> model = Optional.empty();
		private Optional<ResourceLocation> cameraOverlay = Optional.empty();
		private Optional<HolderSet<EntityType<?>>> allowedEntities = Optional.empty();
		private boolean dispensable = true;
		private boolean swappable = true;
		private boolean damageOnHurt = true;

		Builder(EquipmentSlot arg) {
			this.slot = arg;
		}

		public Equippable.Builder setEquipSound(Holder<SoundEvent> arg) {
			this.equipSound = arg;
			return this;
		}

		public Equippable.Builder setModel(ResourceLocation arg) {
			this.model = Optional.of(arg);
			return this;
		}

		public Equippable.Builder setCameraOverlay(ResourceLocation arg) {
			this.cameraOverlay = Optional.of(arg);
			return this;
		}

		public Equippable.Builder setAllowedEntities(EntityType<?>... args) {
			return this.setAllowedEntities(HolderSet.direct(EntityType::builtInRegistryHolder, args));
		}

		public Equippable.Builder setAllowedEntities(HolderSet<EntityType<?>> arg) {
			this.allowedEntities = Optional.of(arg);
			return this;
		}

		public Equippable.Builder setDispensable(boolean bl) {
			this.dispensable = bl;
			return this;
		}

		public Equippable.Builder setSwappable(boolean bl) {
			this.swappable = bl;
			return this;
		}

		public Equippable.Builder setDamageOnHurt(boolean bl) {
			this.damageOnHurt = bl;
			return this;
		}

		public Equippable build() {
			return new Equippable(this.slot, this.equipSound, this.model, this.cameraOverlay, this.allowedEntities, this.dispensable, this.swappable, this.damageOnHurt);
		}
	}
}
