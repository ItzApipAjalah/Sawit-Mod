package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;

public class ItemFrameItem extends HangingEntityItem {
	public ItemFrameItem(EntityType<? extends HangingEntity> arg, Item.Properties arg2) {
		super(arg, arg2);
	}

	@Override
	protected boolean mayPlace(Player arg, Direction arg2, ItemStack arg3, BlockPos arg4) {
		return !arg.level().isOutsideBuildHeight(arg4) && arg.mayUseItemAt(arg4, arg2, arg3);
	}
}
