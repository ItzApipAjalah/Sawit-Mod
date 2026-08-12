package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class AxeItem extends DiggerItem {
	protected static final Map<Block, Block> STRIPPABLES = new Builder<Block, Block>()
		.put(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD)
		.put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG)
		.put(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD)
		.put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG)
		.put(Blocks.PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD)
		.put(Blocks.PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_LOG)
		.put(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD)
		.put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG)
		.put(Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD)
		.put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG)
		.put(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD)
		.put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG)
		.put(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD)
		.put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG)
		.put(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD)
		.put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG)
		.put(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM)
		.put(Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE)
		.put(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM)
		.put(Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE)
		.put(Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD)
		.put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG)
		.put(Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK)
		.build();

	public AxeItem(ToolMaterial arg, float f, float g, Item.Properties arg2) {
		super(arg, BlockTags.MINEABLE_WITH_AXE, f, g, arg2);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Level level = arg.getLevel();
		BlockPos blockpos = arg.getClickedPos();
		Player player = arg.getPlayer();
		if (playerHasShieldUseIntent(arg)) {
			return InteractionResult.PASS;
		} else {
			Optional<BlockState> optional = this.evaluateNewBlockState(level, blockpos, player, level.getBlockState(blockpos), arg);
			if (optional.isEmpty()) {
				return InteractionResult.PASS;
			} else {
				ItemStack itemstack = arg.getItemInHand();
				if (player instanceof ServerPlayer) {
					CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
				}

				level.setBlock(blockpos, (BlockState)optional.get(), 11);
				level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, (BlockState)optional.get()));
				if (player != null) {
					itemstack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(arg.getHand()));
				}

				return InteractionResult.SUCCESS;
			}
		}
	}

	private static boolean playerHasShieldUseIntent(UseOnContext arg) {
		Player player = arg.getPlayer();
		return arg.getHand().equals(InteractionHand.MAIN_HAND) && player.getOffhandItem().is(Items.SHIELD) && !player.isSecondaryUseActive();
	}

	private Optional<BlockState> evaluateNewBlockState(Level arg, BlockPos arg2, @Nullable Player arg3, BlockState arg4, UseOnContext arg5) {
		Optional<BlockState> optional = Optional.ofNullable(arg4.getToolModifiedState(arg5, ItemAbilities.AXE_STRIP, false));
		if (optional.isPresent()) {
			arg.playSound(arg3, arg2, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
			return optional;
		} else {
			Optional<BlockState> optional1 = Optional.ofNullable(arg4.getToolModifiedState(arg5, ItemAbilities.AXE_SCRAPE, false));
			if (optional1.isPresent()) {
				arg.playSound(arg3, arg2, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
				arg.levelEvent(arg3, 3005, arg2, 0);
				return optional1;
			} else {
				Optional<BlockState> optional2 = Optional.ofNullable(arg4.getToolModifiedState(arg5, ItemAbilities.AXE_WAX_OFF, false));
				if (optional2.isPresent()) {
					arg.playSound(arg3, arg2, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
					arg.levelEvent(arg3, 3004, arg2, 0);
					return optional2;
				} else {
					return Optional.empty();
				}
			}
		}
	}

	@org.jetbrains.annotations.Nullable
	public static BlockState getAxeStrippingState(BlockState originalState) {
		Block block = (Block)STRIPPABLES.get(originalState.getBlock());
		return block != null ? block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, (Direction.Axis)originalState.getValue(RotatedPillarBlock.AXIS)) : null;
	}

	private Optional<BlockState> getStripped(BlockState arg) {
		return Optional.ofNullable((Block)STRIPPABLES.get(arg.getBlock()))
			.map(arg2 -> arg2.defaultBlockState().setValue(RotatedPillarBlock.AXIS, (Direction.Axis)arg.getValue(RotatedPillarBlock.AXIS)));
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility);
	}
}
