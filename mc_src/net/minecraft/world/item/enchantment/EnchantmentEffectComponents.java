package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.enchantment.effects.DamageImmunity;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public interface EnchantmentEffectComponents {
	Codec<DataComponentType<?>> COMPONENT_CODEC = Codec.lazyInitialized(() -> BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.byNameCodec());
	Codec<DataComponentMap> CODEC = DataComponentMap.makeCodec(COMPONENT_CODEC);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> DAMAGE_PROTECTION = register(
		"damage_protection", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
	);
	DataComponentType<List<ConditionalEffect<DamageImmunity>>> DAMAGE_IMMUNITY = register(
		"damage_immunity", arg -> arg.persistent(ConditionalEffect.codec(DamageImmunity.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> DAMAGE = register(
		"damage", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> SMASH_DAMAGE_PER_FALLEN_BLOCK = register(
		"smash_damage_per_fallen_block", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> KNOCKBACK = register(
		"knockback", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> ARMOR_EFFECTIVENESS = register(
		"armor_effectiveness", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
	);
	DataComponentType<List<TargetedConditionalEffect<EnchantmentEntityEffect>>> POST_ATTACK = register(
		"post_attack", arg -> arg.persistent(TargetedConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> HIT_BLOCK = register(
		"hit_block", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.HIT_BLOCK).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> ITEM_DAMAGE = register(
		"item_damage", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
	);
	DataComponentType<List<EnchantmentAttributeEffect>> ATTRIBUTES = register(
		"attributes", arg -> arg.persistent(EnchantmentAttributeEffect.CODEC.codec().listOf())
	);
	DataComponentType<List<TargetedConditionalEffect<EnchantmentValueEffect>>> EQUIPMENT_DROPS = register(
		"equipment_drops",
		arg -> arg.persistent(TargetedConditionalEffect.equipmentDropsCodec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentLocationBasedEffect>>> LOCATION_CHANGED = register(
		"location_changed", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentLocationBasedEffect.CODEC, LootContextParamSets.ENCHANTED_LOCATION).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> TICK = register(
		"tick", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> AMMO_USE = register(
		"ammo_use", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_PIERCING = register(
		"projectile_piercing", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> PROJECTILE_SPAWNED = register(
		"projectile_spawned", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_SPREAD = register(
		"projectile_spread", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_COUNT = register(
		"projectile_count", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> TRIDENT_RETURN_ACCELERATION = register(
		"trident_return_acceleration", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> FISHING_TIME_REDUCTION = register(
		"fishing_time_reduction", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> FISHING_LUCK_BONUS = register(
		"fishing_luck_bonus", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> BLOCK_EXPERIENCE = register(
		"block_experience", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> MOB_EXPERIENCE = register(
		"mob_experience", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
	);
	DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> REPAIR_WITH_XP = register(
		"repair_with_xp", arg -> arg.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
	);
	DataComponentType<EnchantmentValueEffect> CROSSBOW_CHARGE_TIME = register("crossbow_charge_time", arg -> arg.persistent(EnchantmentValueEffect.CODEC));
	DataComponentType<List<CrossbowItem.ChargingSounds>> CROSSBOW_CHARGING_SOUNDS = register(
		"crossbow_charging_sounds", arg -> arg.persistent(CrossbowItem.ChargingSounds.CODEC.listOf())
	);
	DataComponentType<List<Holder<SoundEvent>>> TRIDENT_SOUND = register("trident_sound", arg -> arg.persistent(SoundEvent.CODEC.listOf()));
	DataComponentType<Unit> PREVENT_EQUIPMENT_DROP = register("prevent_equipment_drop", arg -> arg.persistent(Unit.CODEC));
	DataComponentType<Unit> PREVENT_ARMOR_CHANGE = register("prevent_armor_change", arg -> arg.persistent(Unit.CODEC));
	DataComponentType<EnchantmentValueEffect> TRIDENT_SPIN_ATTACK_STRENGTH = register(
		"trident_spin_attack_strength", arg -> arg.persistent(EnchantmentValueEffect.CODEC)
	);

	static DataComponentType<?> bootstrap(Registry<DataComponentType<?>> arg) {
		return DAMAGE_PROTECTION;
	}

	private static <T> DataComponentType<T> register(String string, UnaryOperator<DataComponentType.Builder<T>> unaryOperator) {
		return Registry.register(
			BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, string, ((DataComponentType.Builder)unaryOperator.apply(DataComponentType.builder())).build()
		);
	}
}
