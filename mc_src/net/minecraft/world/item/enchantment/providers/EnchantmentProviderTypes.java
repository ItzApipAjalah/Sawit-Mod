package net.minecraft.world.item.enchantment.providers;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public interface EnchantmentProviderTypes {
	static MapCodec<? extends EnchantmentProvider> bootstrap(Registry<MapCodec<? extends EnchantmentProvider>> arg) {
		Registry.register(arg, "by_cost", EnchantmentsByCost.CODEC);
		Registry.register(arg, "by_cost_with_difficulty", EnchantmentsByCostWithDifficulty.CODEC);
		return Registry.register(arg, "single", SingleEnchantment.CODEC);
	}
}
