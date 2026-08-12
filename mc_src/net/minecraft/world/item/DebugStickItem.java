package net.minecraft.world.item;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class DebugStickItem extends Item {
	public DebugStickItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public boolean canAttackBlock(BlockState arg, Level arg2, BlockPos arg3, Player arg4) {
		if (!arg2.isClientSide) {
			this.handleInteraction(arg4, arg, arg2, arg3, false, arg4.getItemInHand(InteractionHand.MAIN_HAND));
		}

		return false;
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Player player = arg.getPlayer();
		Level level = arg.getLevel();
		if (!level.isClientSide && player != null) {
			BlockPos blockPos = arg.getClickedPos();
			if (!this.handleInteraction(player, level.getBlockState(blockPos), level, blockPos, true, arg.getItemInHand())) {
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.SUCCESS;
	}

	private boolean handleInteraction(Player arg, BlockState arg2, LevelAccessor arg3, BlockPos arg4, boolean bl, ItemStack arg5) {
		if (!arg.canUseGameMasterBlocks()) {
			return false;
		} else {
			Holder<Block> holder = arg2.getBlockHolder();
			StateDefinition<Block, BlockState> stateDefinition = holder.value().getStateDefinition();
			Collection<Property<?>> collection = stateDefinition.getProperties();
			if (collection.isEmpty()) {
				message(arg, Component.translatable(this.descriptionId + ".empty", holder.getRegisteredName()));
				return false;
			} else {
				DebugStickState debugStickState = arg5.get(DataComponents.DEBUG_STICK_STATE);
				if (debugStickState == null) {
					return false;
				} else {
					Property<?> property = (Property<?>)debugStickState.properties().get(holder);
					if (bl) {
						if (property == null) {
							property = (Property<?>)collection.iterator().next();
						}

						BlockState blockState = cycleState(arg2, property, arg.isSecondaryUseActive());
						arg3.setBlock(arg4, blockState, 18);
						message(arg, Component.translatable(this.descriptionId + ".update", property.getName(), getNameHelper(blockState, property)));
					} else {
						property = getRelative(collection, property, arg.isSecondaryUseActive());
						arg5.set(DataComponents.DEBUG_STICK_STATE, debugStickState.withProperty(holder, property));
						message(arg, Component.translatable(this.descriptionId + ".select", property.getName(), getNameHelper(arg2, property)));
					}

					return true;
				}
			}
		}
	}

	private static <T extends Comparable<T>> BlockState cycleState(BlockState arg, Property<T> arg2, boolean bl) {
		return arg.setValue(arg2, getRelative(arg2.getPossibleValues(), arg.getValue(arg2), bl));
	}

	private static <T> T getRelative(Iterable<T> iterable, @Nullable T object, boolean bl) {
		return bl ? Util.findPreviousInIterable(iterable, (T)object) : Util.findNextInIterable(iterable, (T)object);
	}

	private static void message(Player arg, Component arg2) {
		((ServerPlayer)arg).sendSystemMessage(arg2, true);
	}

	private static <T extends Comparable<T>> String getNameHelper(BlockState arg, Property<T> arg2) {
		return arg2.getName(arg.getValue(arg2));
	}
}
