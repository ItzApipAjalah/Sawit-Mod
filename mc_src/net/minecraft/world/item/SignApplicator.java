package net.minecraft.world.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public interface SignApplicator {
	boolean tryApplyToSign(Level arg, SignBlockEntity arg2, boolean bl, Player arg3);

	default boolean canApplyToSign(SignText arg, Player arg2) {
		return arg.hasMessage(arg2);
	}
}
