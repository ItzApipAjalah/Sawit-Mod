package net.minecraft.world.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class GlowInkSacItem extends Item implements SignApplicator {
	public GlowInkSacItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public boolean tryApplyToSign(Level arg, SignBlockEntity arg2, boolean bl, Player arg3) {
		if (arg2.updateText(argx -> argx.setHasGlowingText(true), bl)) {
			arg.playSound(null, arg2.getBlockPos(), SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			return true;
		} else {
			return false;
		}
	}
}
