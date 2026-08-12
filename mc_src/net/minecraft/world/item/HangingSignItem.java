package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HangingSignItem extends SignItem {
	public HangingSignItem(Block arg, Block arg2, Item.Properties arg3) {
		super(arg3, arg, arg2, Direction.UP);
	}

	@Override
	protected boolean canPlace(LevelReader arg, BlockState arg2, BlockPos arg3) {
		return arg2.getBlock() instanceof WallHangingSignBlock wallHangingSignBlock && !wallHangingSignBlock.canPlace(arg2, arg, arg3)
			? false
			: super.canPlace(arg, arg2, arg3);
	}
}
