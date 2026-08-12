package net.minecraft.world.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SplashPotionItem extends ThrowablePotionItem {
	public SplashPotionItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		arg.playSound(
			null, arg2.getX(), arg2.getY(), arg2.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (arg.getRandom().nextFloat() * 0.4F + 0.8F)
		);
		return super.use(arg, arg2, arg3);
	}
}
