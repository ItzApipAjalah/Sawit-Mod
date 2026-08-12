package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ScaffoldingBlockItem extends BlockItem {
	public ScaffoldingBlockItem(Block arg, Item.Properties arg2) {
		super(arg, arg2);
	}

	@Nullable
	@Override
	public BlockPlaceContext updatePlacementContext(BlockPlaceContext arg) {
		BlockPos blockPos = arg.getClickedPos();
		Level level = arg.getLevel();
		BlockState blockState = level.getBlockState(blockPos);
		Block block = this.getBlock();
		if (!blockState.is(block)) {
			return ScaffoldingBlock.getDistance(level, blockPos) == 7 ? null : arg;
		} else {
			Direction direction;
			if (arg.isSecondaryUseActive()) {
				direction = arg.isInside() ? arg.getClickedFace().getOpposite() : arg.getClickedFace();
			} else {
				direction = arg.getClickedFace() == Direction.UP ? arg.getHorizontalDirection() : Direction.UP;
			}

			int i = 0;
			BlockPos.MutableBlockPos mutableBlockPos = blockPos.mutable().move(direction);

			while (i < 7) {
				if (!level.isClientSide && !level.isInWorldBounds(mutableBlockPos)) {
					Player player = arg.getPlayer();
					int j = level.getMaxY();
					if (player instanceof ServerPlayer && mutableBlockPos.getY() > j) {
						((ServerPlayer)player).sendSystemMessage(Component.translatable("build.tooHigh", j).withStyle(ChatFormatting.RED), true);
					}
					break;
				}

				blockState = level.getBlockState(mutableBlockPos);
				if (!blockState.is(this.getBlock())) {
					if (blockState.canBeReplaced(arg)) {
						return BlockPlaceContext.at(arg, mutableBlockPos, direction);
					}
					break;
				}

				mutableBlockPos.move(direction);
				if (direction.getAxis().isHorizontal()) {
					i++;
				}
			}

			return null;
		}
	}

	@Override
	protected boolean mustSurvive() {
		return false;
	}
}
