package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public interface EnchantmentLocationBasedEffect {
	Codec<EnchantmentLocationBasedEffect> CODEC = BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE
		.byNameCodec()
		.dispatch(EnchantmentLocationBasedEffect::codec, Function.identity());

	static MapCodec<? extends EnchantmentLocationBasedEffect> bootstrap(Registry<MapCodec<? extends EnchantmentLocationBasedEffect>> arg) {
		Registry.register(arg, "all_of", AllOf.LocationBasedEffects.CODEC);
		Registry.register(arg, "apply_mob_effect", ApplyMobEffect.CODEC);
		Registry.register(arg, "attribute", EnchantmentAttributeEffect.CODEC);
		Registry.register(arg, "change_item_damage", ChangeItemDamage.CODEC);
		Registry.register(arg, "damage_entity", DamageEntity.CODEC);
		Registry.register(arg, "explode", ExplodeEffect.CODEC);
		Registry.register(arg, "ignite", Ignite.CODEC);
		Registry.register(arg, "play_sound", PlaySoundEffect.CODEC);
		Registry.register(arg, "replace_block", ReplaceBlock.CODEC);
		Registry.register(arg, "replace_disk", ReplaceDisk.CODEC);
		Registry.register(arg, "run_function", RunFunction.CODEC);
		Registry.register(arg, "set_block_properties", SetBlockProperties.CODEC);
		Registry.register(arg, "spawn_particles", SpawnParticlesEffect.CODEC);
		return Registry.register(arg, "summon_entity", SummonEntityEffect.CODEC);
	}

	void onChangedBlock(ServerLevel arg, int i, EnchantedItemInUse arg2, Entity arg3, Vec3 arg4, boolean bl);

	default void onDeactivated(EnchantedItemInUse arg, Entity arg2, Vec3 arg3, int i) {
	}

	MapCodec<? extends EnchantmentLocationBasedEffect> codec();
}
