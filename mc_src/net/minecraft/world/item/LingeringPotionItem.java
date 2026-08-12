package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

public class LingeringPotionItem extends ThrowablePotionItem {
	public LingeringPotionItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		PotionContents potionContents = arg.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		potionContents.addPotionTooltip(list::add, 0.25F, arg2.tickRate());
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		arg.playSound(
			null,
			arg2.getX(),
			arg2.getY(),
			arg2.getZ(),
			SoundEvents.LINGERING_POTION_THROW,
			SoundSource.NEUTRAL,
			0.5F,
			0.4F / (arg.getRandom().nextFloat() * 0.4F + 0.8F)
		);
		return super.use(arg, arg2, arg3);
	}
}
