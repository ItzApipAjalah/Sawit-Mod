package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;

public interface EnchantmentValueEffect {
	Codec<EnchantmentValueEffect> CODEC = BuiltInRegistries.ENCHANTMENT_VALUE_EFFECT_TYPE
		.byNameCodec()
		.dispatch(EnchantmentValueEffect::codec, Function.identity());

	static MapCodec<? extends EnchantmentValueEffect> bootstrap(Registry<MapCodec<? extends EnchantmentValueEffect>> arg) {
		Registry.register(arg, "add", AddValue.CODEC);
		Registry.register(arg, "all_of", AllOf.ValueEffects.CODEC);
		Registry.register(arg, "multiply", MultiplyValue.CODEC);
		Registry.register(arg, "remove_binomial", RemoveBinomial.CODEC);
		return Registry.register(arg, "set", SetValue.CODEC);
	}

	float process(int i, RandomSource arg, float f);

	MapCodec<? extends EnchantmentValueEffect> codec();
}
