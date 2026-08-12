package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DoubleHighBlockItem extends BlockItem {
	public DoubleHighBlockItem(Block arg, Item.Properties arg2) {
		super(arg, arg2);
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext arg, BlockState arg2) {
		Level level = arg.getLevel();
		BlockPos blockPos = arg.getClickedPos().above();
		BlockState blockState = level.isWaterAt(blockPos) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
		level.setBlock(blockPos, blockState, 27);
		return super.placeBlock(arg, arg2);
	}
}
