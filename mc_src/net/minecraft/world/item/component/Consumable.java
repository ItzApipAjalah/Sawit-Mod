package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.PlaySoundConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public record Consumable(
	float consumeSeconds, ItemUseAnimation animation, Holder<SoundEvent> sound, boolean hasConsumeParticles, List<ConsumeEffect> onConsumeEffects
) {
	public static final float DEFAULT_CONSUME_SECONDS = 1.6F;
	private static final int CONSUME_EFFECTS_INTERVAL = 4;
	private static final float CONSUME_EFFECTS_START_FRACTION = 0.21875F;
	public static final Codec<Consumable> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("consume_seconds", 1.6F).forGetter(Consumable::consumeSeconds),
				ItemUseAnimation.CODEC.optionalFieldOf("animation", ItemUseAnimation.EAT).forGetter(Consumable::animation),
				SoundEvent.CODEC.optionalFieldOf("sound", SoundEvents.GENERIC_EAT).forGetter(Consumable::sound),
				Codec.BOOL.optionalFieldOf("has_consume_particles", true).forGetter(Consumable::hasConsumeParticles),
				ConsumeEffect.CODEC.listOf().optionalFieldOf("on_consume_effects", List.of()).forGetter(Consumable::onConsumeEffects)
			)
			.apply(instance, Consumable::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, Consumable> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT,
		Consumable::consumeSeconds,
		ItemUseAnimation.STREAM_CODEC,
		Consumable::animation,
		SoundEvent.STREAM_CODEC,
		Consumable::sound,
		ByteBufCodecs.BOOL,
		Consumable::hasConsumeParticles,
		ConsumeEffect.STREAM_CODEC.apply(ByteBufCodecs.list()),
		Consumable::onConsumeEffects,
		Consumable::new
	);

	public InteractionResult startConsuming(LivingEntity arg, ItemStack arg2, InteractionHand arg3) {
		if (!this.canConsume(arg, arg2)) {
			return InteractionResult.FAIL;
		} else {
			boolean bl = this.consumeTicks() > 0;
			if (bl) {
				arg.startUsingItem(arg3);
				return InteractionResult.CONSUME;
			} else {
				ItemStack itemStack = this.onConsume(arg.level(), arg, arg2);
				return InteractionResult.CONSUME.heldItemTransformedTo(itemStack);
			}
		}
	}

	public ItemStack onConsume(Level arg, LivingEntity arg2, ItemStack arg3) {
		RandomSource randomSource = arg2.getRandom();
		this.emitParticlesAndSounds(randomSource, arg2, arg3, 16);
		if (arg2 instanceof ServerPlayer serverPlayer) {
			serverPlayer.awardStat(Stats.ITEM_USED.get(arg3.getItem()));
			CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, arg3);
		}

		arg3.getAllOfType(ConsumableListener.class).forEach(arg4 -> arg4.onConsume(arg, arg2, arg3, this));
		if (!arg.isClientSide) {
			this.onConsumeEffects.forEach(arg4 -> arg4.apply(arg, arg3, arg2));
		}

		arg2.gameEvent(this.animation == ItemUseAnimation.DRINK ? GameEvent.DRINK : GameEvent.EAT);
		arg3.consume(1, arg2);
		return arg3;
	}

	public boolean canConsume(LivingEntity arg, ItemStack arg2) {
		FoodProperties foodProperties = arg2.get(DataComponents.FOOD);
		return foodProperties != null && arg instanceof Player player ? player.canEat(foodProperties.canAlwaysEat()) : true;
	}

	public int consumeTicks() {
		return (int)(this.consumeSeconds * 20.0F);
	}

	public void emitParticlesAndSounds(RandomSource arg, LivingEntity arg2, ItemStack arg3, int i) {
		float f = arg.nextBoolean() ? 0.5F : 1.0F;
		float g = arg.triangle(1.0F, 0.2F);
		float h = 0.5F;
		float j = Mth.randomBetween(arg, 0.9F, 1.0F);
		float k = this.animation == ItemUseAnimation.DRINK ? 0.5F : f;
		float l = this.animation == ItemUseAnimation.DRINK ? j : g;
		if (this.hasConsumeParticles) {
			arg2.spawnItemParticles(arg3, i);
		}

		SoundEvent soundEvent = arg2 instanceof Consumable.OverrideConsumeSound overrideConsumeSound
			? overrideConsumeSound.getConsumeSound(arg3)
			: this.sound.value();
		arg2.playSound(soundEvent, k, l);
	}

	public boolean shouldEmitParticlesAndSounds(int i) {
		int j = this.consumeTicks() - i;
		int k = (int)(this.consumeTicks() * 0.21875F);
		boolean bl = j > k;
		return bl && i % 4 == 0;
	}

	public static Consumable.Builder builder() {
		return new Consumable.Builder();
	}

	public static class Builder {
		private float consumeSeconds = 1.6F;
		private ItemUseAnimation animation = ItemUseAnimation.EAT;
		private Holder<SoundEvent> sound = SoundEvents.GENERIC_EAT;
		private boolean hasConsumeParticles = true;
		private final List<ConsumeEffect> onConsumeEffects = new ArrayList();

		Builder() {
		}

		public Consumable.Builder consumeSeconds(float f) {
			this.consumeSeconds = f;
			return this;
		}

		public Consumable.Builder animation(ItemUseAnimation arg) {
			this.animation = arg;
			return this;
		}

		public Consumable.Builder sound(Holder<SoundEvent> arg) {
			this.sound = arg;
			return this;
		}

		public Consumable.Builder soundAfterConsume(Holder<SoundEvent> arg) {
			return this.onConsume(new PlaySoundConsumeEffect(arg));
		}

		public Consumable.Builder hasConsumeParticles(boolean bl) {
			this.hasConsumeParticles = bl;
			return this;
		}

		public Consumable.Builder onConsume(ConsumeEffect arg) {
			this.onConsumeEffects.add(arg);
			return this;
		}

		public Consumable build() {
			return new Consumable(this.consumeSeconds, this.animation, this.sound, this.hasConsumeParticles, this.onConsumeEffects);
		}
	}

	public interface OverrideConsumeSound {
		SoundEvent getConsumeSound(ItemStack arg);
	}
}
