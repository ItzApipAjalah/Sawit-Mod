package net.minecraft.world.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class FlintAndSteelItem extends Item {
	public FlintAndSteelItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Player player = arg.getPlayer();
		Level level = arg.getLevel();
		BlockPos blockpos = arg.getClickedPos();
		BlockState blockstate = level.getBlockState(blockpos);
		BlockState blockstate2 = blockstate.getToolModifiedState(arg, ItemAbilities.FIRESTARTER_LIGHT, false);
		if (blockstate2 == null) {
			BlockPos blockpos1 = blockpos.relative(arg.getClickedFace());
			if (BaseFireBlock.canBePlacedAt(level, blockpos1, arg.getHorizontalDirection())) {
				level.playSound(player, blockpos1, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
				BlockState blockstate1 = BaseFireBlock.getState(level, blockpos1);
				level.setBlock(blockpos1, blockstate1, 11);
				level.gameEvent(player, GameEvent.BLOCK_PLACE, blockpos);
				ItemStack itemstack = arg.getItemInHand();
				if (player instanceof ServerPlayer) {
					CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)player, blockpos1, itemstack);
					itemstack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(arg.getHand()));
				}

				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.FAIL;
			}
		} else {
			level.playSound(player, blockpos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
			level.setBlock(blockpos, blockstate2, 11);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, blockpos);
			if (player != null) {
				arg.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(arg.getHand()));
			}

			return InteractionResult.SUCCESS;
		}
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_FLINT_ACTIONS.contains(itemAbility);
	}
}
