package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record ConditionalEffect<T>(T effect, Optional<LootItemCondition> requirements) {
	public static Codec<LootItemCondition> conditionCodec(ContextKeySet arg) {
		return LootItemCondition.DIRECT_CODEC
			.validate(
				arg2 -> {
					ProblemReporter.Collector collector = new ProblemReporter.Collector();
					ValidationContext validationContext = new ValidationContext(collector, arg);
					arg2.validate(validationContext);
					return (DataResult)collector.getReport()
						.map(string -> DataResult.error(() -> "Validation error in enchantment effect condition: " + string))
						.orElseGet(() -> DataResult.success(arg2));
				}
			);
	}

	public static <T> Codec<ConditionalEffect<T>> codec(Codec<T> codec, ContextKeySet arg) {
		return RecordCodecBuilder.create(
			instance -> instance.group(
					codec.fieldOf("effect").forGetter(ConditionalEffect::effect),
					conditionCodec(arg).optionalFieldOf("requirements").forGetter(ConditionalEffect::requirements)
				)
				.apply(instance, ConditionalEffect::new)
		);
	}

	public boolean matches(LootContext arg) {
		return this.requirements.isEmpty() ? true : ((LootItemCondition)this.requirements.get()).test(arg);
	}
}
