package net.minecraft.world.item;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BlockItem extends Item {
	@Deprecated
	private final Block block;

	public BlockItem(Block arg, Item.Properties arg2) {
		super(arg2);
		this.block = arg;
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		InteractionResult interactionresult = this.place(new BlockPlaceContext(arg));
		return !interactionresult.consumesAction() && arg.getItemInHand().has(DataComponents.CONSUMABLE)
			? super.use(arg.getLevel(), arg.getPlayer(), arg.getHand())
			: interactionresult;
	}

	public InteractionResult place(BlockPlaceContext arg) {
		if (!this.getBlock().isEnabled(arg.getLevel().enabledFeatures())) {
			return InteractionResult.FAIL;
		} else if (!arg.canPlace()) {
			return InteractionResult.FAIL;
		} else {
			BlockPlaceContext blockplacecontext = this.updatePlacementContext(arg);
			if (blockplacecontext == null) {
				return InteractionResult.FAIL;
			} else {
				BlockState blockstate = this.getPlacementState(blockplacecontext);
				if (blockstate == null) {
					return InteractionResult.FAIL;
				} else if (!this.placeBlock(blockplacecontext, blockstate)) {
					return InteractionResult.FAIL;
				} else {
					BlockPos blockpos = blockplacecontext.getClickedPos();
					Level level = blockplacecontext.getLevel();
					Player player = blockplacecontext.getPlayer();
					ItemStack itemstack = blockplacecontext.getItemInHand();
					BlockState blockstate1 = level.getBlockState(blockpos);
					if (blockstate1.is(blockstate.getBlock())) {
						blockstate1 = this.updateBlockStateFromTag(blockpos, level, itemstack, blockstate1);
						this.updateCustomBlockEntityTag(blockpos, level, player, itemstack, blockstate1);
						updateBlockEntityComponents(level, blockpos, itemstack);
						blockstate1.getBlock().setPlacedBy(level, blockpos, blockstate1, player, itemstack);
						if (player instanceof ServerPlayer) {
							CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
						}
					}

					SoundType soundtype = blockstate1.getSoundType(level, blockpos, arg.getPlayer());
					level.playSound(
						player,
						blockpos,
						this.getPlaceSound(blockstate1, level, blockpos, arg.getPlayer()),
						SoundSource.BLOCKS,
						(soundtype.getVolume() + 1.0F) / 2.0F,
						soundtype.getPitch() * 0.8F
					);
					level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, blockstate1));
					itemstack.consume(1, player);
					return InteractionResult.SUCCESS;
				}
			}
		}
	}

	@Deprecated
	protected SoundEvent getPlaceSound(BlockState arg) {
		return arg.getSoundType().getPlaceSound();
	}

	protected SoundEvent getPlaceSound(BlockState state, Level world, BlockPos pos, Player entity) {
		return state.getSoundType(world, pos, entity).getPlaceSound();
	}

	@Nullable
	public BlockPlaceContext updatePlacementContext(BlockPlaceContext arg) {
		return arg;
	}

	private static void updateBlockEntityComponents(Level arg, BlockPos arg2, ItemStack arg3) {
		BlockEntity blockentity = arg.getBlockEntity(arg2);
		if (blockentity != null) {
			blockentity.applyComponentsFromItemStack(arg3);
			blockentity.setChanged();
		}
	}

	protected boolean updateCustomBlockEntityTag(BlockPos arg, Level arg2, @Nullable Player arg3, ItemStack arg4, BlockState arg5) {
		return updateCustomBlockEntityTag(arg2, arg3, arg, arg4);
	}

	@Nullable
	protected BlockState getPlacementState(BlockPlaceContext arg) {
		BlockState blockstate = this.getBlock().getStateForPlacement(arg);
		return blockstate != null && this.canPlace(arg, blockstate) ? blockstate : null;
	}

	private BlockState updateBlockStateFromTag(BlockPos arg, Level arg2, ItemStack arg3, BlockState arg4) {
		BlockItemStateProperties blockitemstateproperties = arg3.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
		if (blockitemstateproperties.isEmpty()) {
			return arg4;
		} else {
			BlockState blockstate = blockitemstateproperties.apply(arg4);
			if (blockstate != arg4) {
				arg2.setBlock(arg, blockstate, 2);
			}

			return blockstate;
		}
	}

	protected boolean canPlace(BlockPlaceContext arg, BlockState arg2) {
		Player player = arg.getPlayer();
		CollisionContext collisioncontext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
		return (!this.mustSurvive() || arg2.canSurvive(arg.getLevel(), arg.getClickedPos()))
			&& arg.getLevel().isUnobstructed(arg2, arg.getClickedPos(), collisioncontext);
	}

	protected boolean mustSurvive() {
		return true;
	}

	protected boolean placeBlock(BlockPlaceContext arg, BlockState arg2) {
		return arg.getLevel().setBlock(arg.getClickedPos(), arg2, 11);
	}

	public static boolean updateCustomBlockEntityTag(Level arg, @Nullable Player arg2, BlockPos arg3, ItemStack arg4) {
		MinecraftServer minecraftserver = arg.getServer();
		if (minecraftserver == null) {
			return false;
		} else {
			CustomData customdata = arg4.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
			if (!customdata.isEmpty()) {
				BlockEntity blockentity = arg.getBlockEntity(arg3);
				if (blockentity != null) {
					if (!arg.isClientSide && blockentity.onlyOpCanSetNbt() && (arg2 == null || !arg2.canUseGameMasterBlocks())) {
						return false;
					}

					return customdata.loadInto(blockentity, arg.registryAccess());
				}
			}

			return false;
		}
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		super.appendHoverText(arg, arg2, list, arg3);
		this.getBlock().appendHoverText(arg, arg2, list, arg3);
	}

	public Block getBlock() {
		return this.block;
	}

	public void registerBlocks(Map<Block, Item> map, Item arg) {
		map.put(this.getBlock(), arg);
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return !(this.getBlock() instanceof ShulkerBoxBlock);
	}

	@Override
	public void onDestroyed(ItemEntity arg) {
		ItemContainerContents itemcontainercontents = arg.getItem().set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		if (itemcontainercontents != null) {
			ItemUtils.onContainerDestroyed(arg, itemcontainercontents.nonEmptyItemsCopy());
		}
	}

	public static void setBlockEntityData(ItemStack arg, BlockEntityType<?> arg2, CompoundTag arg3) {
		arg3.remove("id");
		if (arg3.isEmpty()) {
			arg.remove(DataComponents.BLOCK_ENTITY_DATA);
		} else {
			BlockEntity.addEntityType(arg3, arg2);
			arg.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(arg3));
		}
	}

	@Override
	public FeatureFlagSet requiredFeatures() {
		return this.getBlock().requiredFeatures();
	}
}
