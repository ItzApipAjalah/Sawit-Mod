package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.extensions.IDispensibleContainerItemExtension;

public interface DispensibleContainerItem extends IDispensibleContainerItemExtension {
	default void checkExtraContent(@Nullable Player arg, Level arg2, ItemStack arg3, BlockPos arg4) {
	}

	@Deprecated
	boolean emptyContents(@Nullable Player arg, Level arg2, BlockPos arg3, @Nullable BlockHitResult arg4);
}
