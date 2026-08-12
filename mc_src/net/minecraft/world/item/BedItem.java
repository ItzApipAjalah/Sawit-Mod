package net.minecraft.world.item;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BedItem extends BlockItem {
	public BedItem(Block arg, Item.Properties arg2) {
		super(arg, arg2);
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext arg, BlockState arg2) {
		return arg.getLevel().setBlock(arg.getClickedPos(), arg2, 26);
	}
}
