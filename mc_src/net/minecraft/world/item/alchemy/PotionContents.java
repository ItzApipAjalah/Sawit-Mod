package net.minecraft.world.item.alchemy;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.AttributeUtil;

public record PotionContents(Optional<Holder<Potion>> potion, Optional<Integer> customColor, List<MobEffectInstance> customEffects, Optional<String> customName)
	implements ConsumableListener {
	public static final PotionContents EMPTY = new PotionContents(Optional.empty(), Optional.empty(), List.of(), Optional.empty());
	private static final Component NO_EFFECT = Component.translatable("effect.none").withStyle(ChatFormatting.GRAY);
	private static final int BASE_POTION_COLOR = -13083194;
	private static final Codec<PotionContents> FULL_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				Potion.CODEC.optionalFieldOf("potion").forGetter(PotionContents::potion),
				Codec.INT.optionalFieldOf("custom_color").forGetter(PotionContents::customColor),
				MobEffectInstance.CODEC.listOf().optionalFieldOf("custom_effects", List.of()).forGetter(PotionContents::customEffects),
				Codec.STRING.optionalFieldOf("custom_name").forGetter(PotionContents::customName)
			)
			.apply(instance, PotionContents::new)
	);
	public static final Codec<PotionContents> CODEC = Codec.withAlternative(FULL_CODEC, Potion.CODEC, PotionContents::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, PotionContents> STREAM_CODEC = StreamCodec.composite(
		Potion.STREAM_CODEC.apply(ByteBufCodecs::optional),
		PotionContents::potion,
		ByteBufCodecs.INT.apply(ByteBufCodecs::optional),
		PotionContents::customColor,
		MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()),
		PotionContents::customEffects,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional),
		PotionContents::customName,
		PotionContents::new
	);

	public PotionContents(Holder<Potion> arg) {
		this(Optional.of(arg), Optional.empty(), List.of(), Optional.empty());
	}

	public static ItemStack createItemStack(Item arg, Holder<Potion> arg2) {
		ItemStack itemstack = new ItemStack(arg);
		itemstack.set(DataComponents.POTION_CONTENTS, new PotionContents(arg2));
		return itemstack;
	}

	public boolean is(Holder<Potion> arg) {
		return this.potion.isPresent() && ((Holder)this.potion.get()).is(arg) && this.customEffects.isEmpty();
	}

	public Iterable<MobEffectInstance> getAllEffects() {
		if (this.potion.isEmpty()) {
			return this.customEffects;
		} else {
			return (Iterable<MobEffectInstance>)(this.customEffects.isEmpty()
				? ((Potion)((Holder)this.potion.get()).value()).getEffects()
				: Iterables.concat(((Potion)((Holder)this.potion.get()).value()).getEffects(), this.customEffects));
		}
	}

	public void forEachEffect(Consumer<MobEffectInstance> consumer) {
		if (this.potion.isPresent()) {
			for (MobEffectInstance mobeffectinstance : ((Potion)((Holder)this.potion.get()).value()).getEffects()) {
				consumer.accept(new MobEffectInstance(mobeffectinstance));
			}
		}

		for (MobEffectInstance mobeffectinstance1 : this.customEffects) {
			consumer.accept(new MobEffectInstance(mobeffectinstance1));
		}
	}

	public PotionContents withPotion(Holder<Potion> arg) {
		return new PotionContents(Optional.of(arg), this.customColor, this.customEffects, this.customName);
	}

	public PotionContents withEffectAdded(MobEffectInstance arg) {
		return new PotionContents(this.potion, this.customColor, Util.copyAndAdd(this.customEffects, arg), this.customName);
	}

	public int getColor() {
		return this.customColor.isPresent() ? (Integer)this.customColor.get() : getColor(this.getAllEffects());
	}

	public static int getColor(Holder<Potion> arg) {
		return getColor(((Potion)arg.value()).getEffects());
	}

	public static int getColor(Iterable<MobEffectInstance> iterable) {
		return getColorOptional(iterable).orElse(-13083194);
	}

	public Component getName(String string) {
		String s = (String)this.customName.or(() -> this.potion.map(arg -> ((Potion)arg.value()).name())).orElse("empty");
		return Component.translatable(string + s);
	}

	public static OptionalInt getColorOptional(Iterable<MobEffectInstance> iterable) {
		int i = 0;
		int j = 0;
		int k = 0;
		int l = 0;

		for (MobEffectInstance mobeffectinstance : iterable) {
			if (mobeffectinstance.isVisible()) {
				int i1 = mobeffectinstance.getEffect().value().getColor();
				int j1 = mobeffectinstance.getAmplifier() + 1;
				i += j1 * ARGB.red(i1);
				j += j1 * ARGB.green(i1);
				k += j1 * ARGB.blue(i1);
				l += j1;
			}
		}

		return l == 0 ? OptionalInt.empty() : OptionalInt.of(ARGB.color(i / l, j / l, k / l));
	}

	public boolean hasEffects() {
		return !this.customEffects.isEmpty() ? true : this.potion.isPresent() && !((Potion)((Holder)this.potion.get()).value()).getEffects().isEmpty();
	}

	public List<MobEffectInstance> customEffects() {
		return Lists.transform(this.customEffects, MobEffectInstance::new);
	}

	public void addPotionTooltip(Consumer<Component> consumer, float f, float g) {
		addPotionTooltip(this.getAllEffects(), consumer, f, g);
	}

	public void applyToLivingEntity(LivingEntity arg) {
		if (arg.level() instanceof ServerLevel serverlevel) {
			Player player1 = arg instanceof Player player ? player : null;
			this.forEachEffect(arg2 -> {
				if (arg2.getEffect().value().isInstantenous()) {
					arg2.getEffect().value().applyInstantenousEffect(serverlevel, player1, player1, arg, arg2.getAmplifier(), 1.0);
				} else {
					arg.addEffect(arg2);
				}
			});
		}
	}

	public static void addPotionTooltip(Iterable<MobEffectInstance> iterable, Consumer<Component> consumer, float f, float g) {
		List<Pair<Holder<Attribute>, AttributeModifier>> list = Lists.<Pair<Holder<Attribute>, AttributeModifier>>newArrayList();
		boolean flag = true;

		for (MobEffectInstance mobeffectinstance : iterable) {
			flag = false;
			MutableComponent mutablecomponent = Component.translatable(mobeffectinstance.getDescriptionId());
			Holder<MobEffect> holder = mobeffectinstance.getEffect();
			holder.value().createModifiers(mobeffectinstance.getAmplifier(), (arg, arg2) -> list.add(new Pair<>(arg, arg2)));
			if (mobeffectinstance.getAmplifier() > 0) {
				mutablecomponent = Component.translatable(
					"potion.withAmplifier", mutablecomponent, Component.translatable("potion.potency." + mobeffectinstance.getAmplifier())
				);
			}

			if (!mobeffectinstance.endsWithin(20)) {
				mutablecomponent = Component.translatable("potion.withDuration", mutablecomponent, MobEffectUtil.formatDuration(mobeffectinstance, f, g));
			}

			consumer.accept(mutablecomponent.withStyle(holder.value().getCategory().getTooltipFormatting()));
		}

		if (flag) {
			consumer.accept(NO_EFFECT);
		}

		if (!list.isEmpty()) {
			consumer.accept(CommonComponents.EMPTY);
			consumer.accept(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));
			AttributeUtil.addPotionTooltip(list, consumer);
		}
	}

	@Override
	public void onConsume(Level arg, LivingEntity arg2, ItemStack arg3, Consumable arg4) {
		this.applyToLivingEntity(arg2);
	}
}
