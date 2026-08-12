package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class ShearsItem extends Item {
	public ShearsItem(Item.Properties arg) {
		super(arg);
	}

	public static Tool createToolProperties() {
		HolderGetter<Block> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
		return new Tool(
			List.of(
				Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F),
				Tool.Rule.overrideSpeed(holdergetter.getOrThrow(BlockTags.LEAVES), 15.0F),
				Tool.Rule.overrideSpeed(holdergetter.getOrThrow(BlockTags.WOOL), 5.0F),
				Tool.Rule.overrideSpeed(HolderSet.direct(Blocks.VINE.builtInRegistryHolder(), Blocks.GLOW_LICHEN.builtInRegistryHolder()), 2.0F)
			),
			1.0F,
			1
		);
	}

	@Override
	public boolean mineBlock(ItemStack arg, Level arg2, BlockState arg3, BlockPos arg4, LivingEntity arg5) {
		if (!arg2.isClientSide && !arg3.is(BlockTags.FIRE)) {
			arg.hurtAndBreak(1, arg5, EquipmentSlot.MAINHAND);
		}

		return arg3.is(BlockTags.LEAVES)
			|| arg3.is(Blocks.COBWEB)
			|| arg3.is(Blocks.SHORT_GRASS)
			|| arg3.is(Blocks.FERN)
			|| arg3.is(Blocks.DEAD_BUSH)
			|| arg3.is(Blocks.HANGING_ROOTS)
			|| arg3.is(Blocks.VINE)
			|| arg3.is(Blocks.TRIPWIRE)
			|| arg3.is(BlockTags.WOOL);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
		if (entity instanceof IShearable target) {
			BlockPos pos = entity.blockPosition();
			boolean isClient = entity.level().isClientSide();
			if (target.isShearable(player, stack, entity.level(), pos)) {
				List<ItemStack> drops = target.onSheared(player, stack, entity.level(), pos);
				if (entity.level() instanceof ServerLevel serverLevel) {
					for (ItemStack drop : drops) {
						target.spawnShearedDrop(serverLevel, pos, drop);
					}
				}

				entity.gameEvent(GameEvent.SHEAR, player);
				if (!isClient) {
					stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
				}

				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.PASS;
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(itemAbility);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Level level = arg.getLevel();
		BlockPos blockpos = arg.getClickedPos();
		BlockState blockstate = level.getBlockState(blockpos);
		BlockState blockstate1 = blockstate.getToolModifiedState(arg, ItemAbilities.SHEARS_TRIM, false);
		if (blockstate1 != null) {
			Player player = arg.getPlayer();
			ItemStack itemstack = arg.getItemInHand();
			if (player instanceof ServerPlayer) {
				CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
			}

			level.setBlockAndUpdate(blockpos, blockstate1);
			level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(arg.getPlayer(), blockstate1));
			if (player != null) {
				itemstack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(arg.getHand()));
			}

			return InteractionResult.SUCCESS;
		} else {
			return super.useOn(arg);
		}
	}
}
