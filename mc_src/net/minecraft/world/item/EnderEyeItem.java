package net.minecraft.world.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class EnderEyeItem extends Item {
	public EnderEyeItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Level level = arg.getLevel();
		BlockPos blockPos = arg.getClickedPos();
		BlockState blockState = level.getBlockState(blockPos);
		if (!blockState.is(Blocks.END_PORTAL_FRAME) || (Boolean)blockState.getValue(EndPortalFrameBlock.HAS_EYE)) {
			return InteractionResult.PASS;
		} else if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		} else {
			BlockState blockState2 = blockState.setValue(EndPortalFrameBlock.HAS_EYE, true);
			Block.pushEntitiesUp(blockState, blockState2, level, blockPos);
			level.setBlock(blockPos, blockState2, 2);
			level.updateNeighbourForOutputSignal(blockPos, Blocks.END_PORTAL_FRAME);
			arg.getItemInHand().shrink(1);
			level.levelEvent(1503, blockPos, 0);
			BlockPattern.BlockPatternMatch blockPatternMatch = EndPortalFrameBlock.getOrCreatePortalShape().find(level, blockPos);
			if (blockPatternMatch != null) {
				BlockPos blockPos2 = blockPatternMatch.getFrontTopLeft().offset(-3, 0, -3);

				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j++) {
						level.setBlock(blockPos2.offset(i, 0, j), Blocks.END_PORTAL.defaultBlockState(), 2);
					}
				}

				level.globalLevelEvent(1038, blockPos2.offset(1, 0, 1), 0);
			}

			return InteractionResult.SUCCESS;
		}
	}

	@Override
	public int getUseDuration(ItemStack arg, LivingEntity arg2) {
		return 0;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemStack = arg2.getItemInHand(arg3);
		BlockHitResult blockHitResult = getPlayerPOVHitResult(arg, arg2, ClipContext.Fluid.NONE);
		if (blockHitResult.getType() == HitResult.Type.BLOCK && arg.getBlockState(blockHitResult.getBlockPos()).is(Blocks.END_PORTAL_FRAME)) {
			return InteractionResult.PASS;
		} else {
			arg2.startUsingItem(arg3);
			if (arg instanceof ServerLevel serverLevel) {
				BlockPos blockPos = serverLevel.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, arg2.blockPosition(), 100, false);
				if (blockPos == null) {
					return InteractionResult.CONSUME;
				}

				EyeOfEnder eyeOfEnder = new EyeOfEnder(arg, arg2.getX(), arg2.getY(0.5), arg2.getZ());
				eyeOfEnder.setItem(itemStack);
				eyeOfEnder.signalTo(blockPos);
				arg.gameEvent(GameEvent.PROJECTILE_SHOOT, eyeOfEnder.position(), GameEvent.Context.of(arg2));
				arg.addFreshEntity(eyeOfEnder);
				if (arg2 instanceof ServerPlayer serverPlayer) {
					CriteriaTriggers.USED_ENDER_EYE.trigger(serverPlayer, blockPos);
				}

				float f = Mth.lerp(arg.random.nextFloat(), 0.33F, 0.5F);
				arg.playSound(null, arg2.getX(), arg2.getY(), arg2.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, f);
				itemStack.consume(1, arg2);
				arg2.awardStat(Stats.ITEM_USED.get(this));
			}

			return InteractionResult.SUCCESS_SERVER;
		}
	}
}
