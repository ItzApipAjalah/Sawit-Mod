package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record TargetedConditionalEffect<T>(EnchantmentTarget enchanted, EnchantmentTarget affected, T effect, Optional<LootItemCondition> requirements) {
	public static <S> Codec<TargetedConditionalEffect<S>> codec(Codec<S> codec, ContextKeySet arg) {
		return RecordCodecBuilder.create(
			instance -> instance.group(
					EnchantmentTarget.CODEC.fieldOf("enchanted").forGetter(TargetedConditionalEffect::enchanted),
					EnchantmentTarget.CODEC.fieldOf("affected").forGetter(TargetedConditionalEffect::affected),
					codec.fieldOf("effect").forGetter(TargetedConditionalEffect::effect),
					ConditionalEffect.conditionCodec(arg).optionalFieldOf("requirements").forGetter(TargetedConditionalEffect::requirements)
				)
				.apply(instance, TargetedConditionalEffect::new)
		);
	}

	public static <S> Codec<TargetedConditionalEffect<S>> equipmentDropsCodec(Codec<S> codec, ContextKeySet arg) {
		return RecordCodecBuilder.create(
			instance -> instance.group(
					EnchantmentTarget.CODEC
						.validate(
							argxx -> argxx != EnchantmentTarget.DAMAGING_ENTITY ? DataResult.success(argxx) : DataResult.error(() -> "enchanted must be attacker or victim")
						)
						.fieldOf("enchanted")
						.forGetter(TargetedConditionalEffect::enchanted),
					codec.fieldOf("effect").forGetter(TargetedConditionalEffect::effect),
					ConditionalEffect.conditionCodec(arg).optionalFieldOf("requirements").forGetter(TargetedConditionalEffect::requirements)
				)
				.apply(instance, (argxx, object, optional) -> new TargetedConditionalEffect<>(argxx, EnchantmentTarget.VICTIM, object, optional))
		);
	}

	public boolean matches(LootContext arg) {
		return this.requirements.isEmpty() ? true : ((LootItemCondition)this.requirements.get()).test(arg);
	}
}
