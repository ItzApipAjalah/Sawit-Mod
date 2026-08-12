package net.minecraft.world.item;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public class StandingAndWallBlockItem extends BlockItem {
	protected final Block wallBlock;
	private final Direction attachmentDirection;

	public StandingAndWallBlockItem(Block arg, Block arg2, Direction arg3, Item.Properties arg4) {
		super(arg, arg4);
		this.wallBlock = arg2;
		this.attachmentDirection = arg3;
	}

	protected boolean canPlace(LevelReader arg, BlockState arg2, BlockPos arg3) {
		return arg2.canSurvive(arg, arg3);
	}

	@Nullable
	@Override
	protected BlockState getPlacementState(BlockPlaceContext arg) {
		BlockState blockState = this.wallBlock.getStateForPlacement(arg);
		BlockState blockState2 = null;
		LevelReader levelReader = arg.getLevel();
		BlockPos blockPos = arg.getClickedPos();

		for (Direction direction : arg.getNearestLookingDirections()) {
			if (direction != this.attachmentDirection.getOpposite()) {
				BlockState blockState3 = direction == this.attachmentDirection ? this.getBlock().getStateForPlacement(arg) : blockState;
				if (blockState3 != null && this.canPlace(levelReader, blockState3, blockPos)) {
					blockState2 = blockState3;
					break;
				}
			}
		}

		return blockState2 != null && levelReader.isUnobstructed(blockState2, blockPos, CollisionContext.empty()) ? blockState2 : null;
	}

	@Override
	public void registerBlocks(Map<Block, Item> map, Item arg) {
		super.registerBlocks(map, arg);
		map.put(this.wallBlock, arg);
	}
}
