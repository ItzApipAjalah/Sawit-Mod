package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public interface AllOf {
	static <T, A extends T> MapCodec<A> codec(Codec<T> codec, Function<List<T>, A> function, Function<A, List<T>> function2) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(codec.listOf().fieldOf("effects").forGetter(function2)).apply(instance, function));
	}

	static AllOf.EntityEffects entityEffects(EnchantmentEntityEffect... args) {
		return new AllOf.EntityEffects(List.of(args));
	}

	static AllOf.LocationBasedEffects locationBasedEffects(EnchantmentLocationBasedEffect... args) {
		return new AllOf.LocationBasedEffects(List.of(args));
	}

	static AllOf.ValueEffects valueEffects(EnchantmentValueEffect... args) {
		return new AllOf.ValueEffects(List.of(args));
	}

	public record EntityEffects(List<EnchantmentEntityEffect> effects) implements EnchantmentEntityEffect {
		public static final MapCodec<AllOf.EntityEffects> CODEC = AllOf.codec(EnchantmentEntityEffect.CODEC, AllOf.EntityEffects::new, AllOf.EntityEffects::effects);

		@Override
		public void apply(ServerLevel arg, int i, EnchantedItemInUse arg2, Entity arg3, Vec3 arg4) {
			for (EnchantmentEntityEffect enchantmentEntityEffect : this.effects) {
				enchantmentEntityEffect.apply(arg, i, arg2, arg3, arg4);
			}
		}

		@Override
		public MapCodec<AllOf.EntityEffects> codec() {
			return CODEC;
		}
	}

	public record LocationBasedEffects(List<EnchantmentLocationBasedEffect> effects) implements EnchantmentLocationBasedEffect {
		public static final MapCodec<AllOf.LocationBasedEffects> CODEC = AllOf.codec(
			EnchantmentLocationBasedEffect.CODEC, AllOf.LocationBasedEffects::new, AllOf.LocationBasedEffects::effects
		);

		@Override
		public void onChangedBlock(ServerLevel arg, int i, EnchantedItemInUse arg2, Entity arg3, Vec3 arg4, boolean bl) {
			for (EnchantmentLocationBasedEffect enchantmentLocationBasedEffect : this.effects) {
				enchantmentLocationBasedEffect.onChangedBlock(arg, i, arg2, arg3, arg4, bl);
			}
		}

		@Override
		public void onDeactivated(EnchantedItemInUse arg, Entity arg2, Vec3 arg3, int i) {
			for (EnchantmentLocationBasedEffect enchantmentLocationBasedEffect : this.effects) {
				enchantmentLocationBasedEffect.onDeactivated(arg, arg2, arg3, i);
			}
		}

		@Override
		public MapCodec<AllOf.LocationBasedEffects> codec() {
			return CODEC;
		}
	}

	public record ValueEffects(List<EnchantmentValueEffect> effects) implements EnchantmentValueEffect {
		public static final MapCodec<AllOf.ValueEffects> CODEC = AllOf.codec(EnchantmentValueEffect.CODEC, AllOf.ValueEffects::new, AllOf.ValueEffects::effects);

		@Override
		public float process(int i, RandomSource arg, float f) {
			for (EnchantmentValueEffect enchantmentValueEffect : this.effects) {
				f = enchantmentValueEffect.process(i, arg, f);
			}

			return f;
		}

		@Override
		public MapCodec<AllOf.ValueEffects> codec() {
			return CODEC;
		}
	}
}
