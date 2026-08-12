package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GameMasterBlockItem extends BlockItem {
	public GameMasterBlockItem(Block arg, Item.Properties arg2) {
		super(arg, arg2);
	}

	@Nullable
	@Override
	protected BlockState getPlacementState(BlockPlaceContext arg) {
		Player player = arg.getPlayer();
		return player != null && !player.canUseGameMasterBlocks() ? null : super.getPlacementState(arg);
	}
}
