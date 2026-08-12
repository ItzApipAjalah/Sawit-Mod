package net.minecraft.world.item;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

public class BucketItem extends Item implements DispensibleContainerItem {
	public final Fluid content;

	public BucketItem(Fluid arg, Item.Properties arg2) {
		super(arg2);
		this.content = arg;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemstack = arg2.getItemInHand(arg3);
		BlockHitResult blockhitresult = getPlayerPOVHitResult(arg, arg2, this.content == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
		if (blockhitresult.getType() == HitResult.Type.MISS) {
			return InteractionResult.PASS;
		} else if (blockhitresult.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		} else {
			BlockPos blockpos = blockhitresult.getBlockPos();
			Direction direction = blockhitresult.getDirection();
			BlockPos blockpos1 = blockpos.relative(direction);
			if (!arg.mayInteract(arg2, blockpos) || !arg2.mayUseItemAt(blockpos1, direction, itemstack)) {
				return InteractionResult.FAIL;
			} else if (this.content == Fluids.EMPTY) {
				BlockState blockstate1 = arg.getBlockState(blockpos);
				if (blockstate1.getBlock() instanceof BucketPickup bucketpickup) {
					ItemStack itemstack3 = bucketpickup.pickupBlock(arg2, arg, blockpos, blockstate1);
					if (!itemstack3.isEmpty()) {
						arg2.awardStat(Stats.ITEM_USED.get(this));
						bucketpickup.getPickupSound(blockstate1).ifPresent(arg2x -> arg2.playSound(arg2x, 1.0F, 1.0F));
						arg.gameEvent(arg2, GameEvent.FLUID_PICKUP, blockpos);
						ItemStack itemstack2 = ItemUtils.createFilledResult(itemstack, arg2, itemstack3);
						if (!arg.isClientSide) {
							CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)arg2, itemstack3);
						}

						return InteractionResult.SUCCESS.heldItemTransformedTo(itemstack2);
					}
				}

				return InteractionResult.FAIL;
			} else {
				BlockState blockstate = arg.getBlockState(blockpos);
				BlockPos blockpos2 = this.canBlockContainFluid(arg2, arg, blockpos, blockstate) ? blockpos : blockpos1;
				if (this.emptyContents(arg2, arg, blockpos2, blockhitresult, itemstack)) {
					this.checkExtraContent(arg2, arg, itemstack, blockpos2);
					if (arg2 instanceof ServerPlayer) {
						CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)arg2, blockpos2, itemstack);
					}

					arg2.awardStat(Stats.ITEM_USED.get(this));
					ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, arg2, getEmptySuccessItem(itemstack, arg2));
					return InteractionResult.SUCCESS.heldItemTransformedTo(itemstack1);
				} else {
					return InteractionResult.FAIL;
				}
			}
		}
	}

	public static ItemStack getEmptySuccessItem(ItemStack arg, Player arg2) {
		return !arg2.hasInfiniteMaterials() ? new ItemStack(Items.BUCKET) : arg;
	}

	@Override
	public void checkExtraContent(@Nullable Player arg, Level arg2, ItemStack arg3, BlockPos arg4) {
	}

	@Deprecated
	@Override
	public boolean emptyContents(@Nullable Player arg, Level arg2, BlockPos arg3, @Nullable BlockHitResult arg4) {
		return this.emptyContents(arg, arg2, arg3, arg4, null);
	}

	@Override
	public boolean emptyContents(@Nullable Player arg, Level arg2, BlockPos arg3, @Nullable BlockHitResult arg4, @Nullable ItemStack container) {
		if (!(this.content instanceof FlowingFluid flowingfluid)) {
			return false;
		} else {
			BlockState blockstate = arg2.getBlockState(arg3);
			Block var17 = blockstate.getBlock();
			boolean bl = blockstate.canBeReplaced(this.content);
			boolean flag2;
			if (blockstate.isAir()
				|| bl
				|| var17 instanceof LiquidBlockContainer liquidblockcontainer && liquidblockcontainer.canPlaceLiquid(arg, arg2, arg3, blockstate, this.content)) {
				flag2 = true;
			} else {
				flag2 = false;
			}

			Optional<FluidStack> containedFluidStack = Optional.ofNullable(container).flatMap(FluidUtil::getFluidContained);
			if (!flag2) {
				return arg4 != null && this.emptyContents(arg, arg2, arg4.getBlockPos().relative(arg4.getDirection()), null, container);
			} else if (containedFluidStack.isPresent() && this.content.getFluidType().isVaporizedOnPlacement(arg2, arg3, (FluidStack)containedFluidStack.get())) {
				this.content.getFluidType().onVaporize(arg, arg2, arg3, (FluidStack)containedFluidStack.get());
				return true;
			} else if (arg2.dimensionType().ultraWarm() && this.content.is(FluidTags.WATER)) {
				int l = arg3.getX();
				int i = arg3.getY();
				int j = arg3.getZ();
				arg2.playSound(arg, arg3, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (arg2.random.nextFloat() - arg2.random.nextFloat()) * 0.8F);

				for (int k = 0; k < 8; k++) {
					arg2.addParticle(ParticleTypes.LARGE_SMOKE, l + Math.random(), i + Math.random(), j + Math.random(), 0.0, 0.0, 0.0);
				}

				return true;
			} else if (var17 instanceof LiquidBlockContainer liquidblockcontainer1 && liquidblockcontainer1.canPlaceLiquid(arg, arg2, arg3, blockstate, this.content)) {
				liquidblockcontainer1.placeLiquid(arg2, arg3, blockstate, flowingfluid.getSource(false));
				this.playEmptySound(arg, arg2, arg3);
				return true;
			} else {
				if (!arg2.isClientSide && bl && !blockstate.liquid()) {
					arg2.destroyBlock(arg3, true);
				}

				if (!arg2.setBlock(arg3, this.content.defaultFluidState().createLegacyBlock(), 11) && !blockstate.getFluidState().isSource()) {
					return false;
				} else {
					this.playEmptySound(arg, arg2, arg3);
					return true;
				}
			}
		}
	}

	protected void playEmptySound(@Nullable Player arg, LevelAccessor arg2, BlockPos arg3) {
		SoundEvent soundevent = this.content.getFluidType().getSound(arg, arg2, arg3, SoundActions.BUCKET_EMPTY);
		if (soundevent == null) {
			soundevent = this.content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
		}

		arg2.playSound(arg, arg3, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
		arg2.gameEvent(arg, GameEvent.FLUID_PLACE, arg3);
	}

	protected boolean canBlockContainFluid(@Nullable Player player, Level worldIn, BlockPos posIn, BlockState blockstate) {
		return blockstate.getBlock() instanceof LiquidBlockContainer
			&& ((LiquidBlockContainer)blockstate.getBlock()).canPlaceLiquid(player, worldIn, posIn, blockstate, this.content);
	}
}
