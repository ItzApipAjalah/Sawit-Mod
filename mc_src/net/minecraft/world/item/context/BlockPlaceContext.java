package net.minecraft.world.item.context;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BlockPlaceContext extends UseOnContext {
	private final BlockPos relativePos;
	protected boolean replaceClicked = true;

	public BlockPlaceContext(Player arg, InteractionHand arg2, ItemStack arg3, BlockHitResult arg4) {
		this(arg.level(), arg, arg2, arg3, arg4);
	}

	public BlockPlaceContext(UseOnContext arg) {
		this(arg.getLevel(), arg.getPlayer(), arg.getHand(), arg.getItemInHand(), arg.getHitResult());
	}

	public BlockPlaceContext(Level arg, @Nullable Player arg2, InteractionHand arg3, ItemStack arg4, BlockHitResult arg5) {
		super(arg, arg2, arg3, arg4, arg5);
		this.relativePos = arg5.getBlockPos().relative(arg5.getDirection());
		this.replaceClicked = arg.getBlockState(arg5.getBlockPos()).canBeReplaced(this);
	}

	public static BlockPlaceContext at(BlockPlaceContext arg, BlockPos arg2, Direction arg3) {
		return new BlockPlaceContext(
			arg.getLevel(),
			arg.getPlayer(),
			arg.getHand(),
			arg.getItemInHand(),
			new BlockHitResult(
				new Vec3(arg2.getX() + 0.5 + arg3.getStepX() * 0.5, arg2.getY() + 0.5 + arg3.getStepY() * 0.5, arg2.getZ() + 0.5 + arg3.getStepZ() * 0.5),
				arg3,
				arg2,
				false
			)
		);
	}

	@Override
	public BlockPos getClickedPos() {
		return this.replaceClicked ? super.getClickedPos() : this.relativePos;
	}

	public boolean canPlace() {
		return this.replaceClicked || this.getLevel().getBlockState(this.getClickedPos()).canBeReplaced(this);
	}

	public boolean replacingClickedOnBlock() {
		return this.replaceClicked;
	}

	public Direction getNearestLookingDirection() {
		return Direction.orderedByNearest(this.getPlayer())[0];
	}

	public Direction getNearestLookingVerticalDirection() {
		return Direction.getFacingAxis(this.getPlayer(), Direction.Axis.Y);
	}

	public Direction[] getNearestLookingDirections() {
		Direction[] directions = Direction.orderedByNearest(this.getPlayer());
		if (this.replaceClicked) {
			return directions;
		} else {
			Direction direction = this.getClickedFace();
			int i = 0;

			while (i < directions.length && directions[i] != direction.getOpposite()) {
				i++;
			}

			if (i > 0) {
				System.arraycopy(directions, 0, directions, 1, i);
				directions[0] = direction.getOpposite();
			}

			return directions;
		}
	}
}
