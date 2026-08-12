package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class InstrumentItem extends Item {
	private final TagKey<Instrument> instruments;

	public InstrumentItem(TagKey<Instrument> arg, Item.Properties arg2) {
		super(arg2);
		this.instruments = arg;
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		super.appendHoverText(arg, arg2, list, arg3);
		HolderLookup.Provider provider = arg2.registries();
		if (provider != null) {
			Optional<Holder<Instrument>> optional = this.getInstrument(arg, provider);
			if (optional.isPresent()) {
				MutableComponent mutableComponent = ((Instrument)((Holder)optional.get()).value()).description().copy();
				ComponentUtils.mergeStyles(mutableComponent, Style.EMPTY.withColor(ChatFormatting.GRAY));
				list.add(mutableComponent);
			}
		}
	}

	public static ItemStack create(Item arg, Holder<Instrument> arg2) {
		ItemStack itemStack = new ItemStack(arg);
		itemStack.set(DataComponents.INSTRUMENT, arg2);
		return itemStack;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemStack = arg2.getItemInHand(arg3);
		Optional<? extends Holder<Instrument>> optional = this.getInstrument(itemStack, arg2.registryAccess());
		if (optional.isPresent()) {
			Instrument instrument = (Instrument)((Holder)optional.get()).value();
			arg2.startUsingItem(arg3);
			play(arg, arg2, instrument);
			arg2.getCooldowns().addCooldown(itemStack, Mth.floor(instrument.useDuration() * 20.0F));
			arg2.awardStat(Stats.ITEM_USED.get(this));
			return InteractionResult.CONSUME;
		} else {
			return InteractionResult.FAIL;
		}
	}

	@Override
	public int getUseDuration(ItemStack arg, LivingEntity arg2) {
		Optional<Holder<Instrument>> optional = this.getInstrument(arg, arg2.registryAccess());
		return (Integer)optional.map(argx -> Mth.floor(((Instrument)argx.value()).useDuration() * 20.0F)).orElse(0);
	}

	private Optional<Holder<Instrument>> getInstrument(ItemStack arg, HolderLookup.Provider arg2) {
		Holder<Instrument> holder = arg.get(DataComponents.INSTRUMENT);
		if (holder != null) {
			return Optional.of(holder);
		} else {
			Optional<HolderSet.Named<Instrument>> optional = arg2.lookupOrThrow(Registries.INSTRUMENT).get(this.instruments);
			if (optional.isPresent()) {
				Iterator<Holder<Instrument>> iterator = ((HolderSet.Named)optional.get()).iterator();
				if (iterator.hasNext()) {
					return Optional.of((Holder)iterator.next());
				}
			}

			return Optional.empty();
		}
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack arg) {
		return ItemUseAnimation.TOOT_HORN;
	}

	private static void play(Level arg, Player arg2, Instrument arg3) {
		SoundEvent soundEvent = arg3.soundEvent().value();
		float f = arg3.range() / 16.0F;
		arg.playSound(arg2, arg2, soundEvent, SoundSource.RECORDS, f, 1.0F);
		arg.gameEvent(GameEvent.INSTRUMENT_PLAY, arg2.position(), GameEvent.Context.of(arg2));
	}
}
