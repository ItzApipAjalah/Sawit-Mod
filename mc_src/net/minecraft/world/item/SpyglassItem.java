package net.minecraft.world.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SpyglassItem extends Item {
	public static final int USE_DURATION = 1200;
	public static final float ZOOM_FOV_MODIFIER = 0.1F;

	public SpyglassItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public int getUseDuration(ItemStack arg, LivingEntity arg2) {
		return 1200;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack arg) {
		return ItemUseAnimation.SPYGLASS;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		arg2.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
		arg2.awardStat(Stats.ITEM_USED.get(this));
		return ItemUtils.startUsingInstantly(arg, arg2, arg3);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack arg, Level arg2, LivingEntity arg3) {
		this.stopUsing(arg3);
		return arg;
	}

	@Override
	public boolean releaseUsing(ItemStack arg, Level arg2, LivingEntity arg3, int i) {
		this.stopUsing(arg3);
		return true;
	}

	private void stopUsing(LivingEntity arg) {
		arg.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
	}
}
