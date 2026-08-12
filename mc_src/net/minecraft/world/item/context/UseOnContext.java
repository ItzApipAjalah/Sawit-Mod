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

public class UseOnContext {
	@Nullable
	private final Player player;
	private final InteractionHand hand;
	private final BlockHitResult hitResult;
	private final Level level;
	private final ItemStack itemStack;

	public UseOnContext(Player arg, InteractionHand arg2, BlockHitResult arg3) {
		this(arg.level(), arg, arg2, arg.getItemInHand(arg2), arg3);
	}

	public UseOnContext(Level arg, @Nullable Player arg2, InteractionHand arg3, ItemStack arg4, BlockHitResult arg5) {
		this.player = arg2;
		this.hand = arg3;
		this.hitResult = arg5;
		this.itemStack = arg4;
		this.level = arg;
	}

	protected final BlockHitResult getHitResult() {
		return this.hitResult;
	}

	public BlockPos getClickedPos() {
		return this.hitResult.getBlockPos();
	}

	public Direction getClickedFace() {
		return this.hitResult.getDirection();
	}

	public Vec3 getClickLocation() {
		return this.hitResult.getLocation();
	}

	public boolean isInside() {
		return this.hitResult.isInside();
	}

	public ItemStack getItemInHand() {
		return this.itemStack;
	}

	@Nullable
	public Player getPlayer() {
		return this.player;
	}

	public InteractionHand getHand() {
		return this.hand;
	}

	public Level getLevel() {
		return this.level;
	}

	public Direction getHorizontalDirection() {
		return this.player == null ? Direction.NORTH : this.player.getDirection();
	}

	public boolean isSecondaryUseActive() {
		return this.player != null && this.player.isSecondaryUseActive();
	}

	public float getRotation() {
		return this.player == null ? 0.0F : this.player.getYRot();
	}
}
