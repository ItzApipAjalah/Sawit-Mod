package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class HoeItem extends DiggerItem {
	@Deprecated
	protected static final Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> TILLABLES = Maps.<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>>newHashMap(
		ImmutableMap.of(
			Blocks.GRASS_BLOCK,
			Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())),
			Blocks.DIRT_PATH,
			Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())),
			Blocks.DIRT,
			Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())),
			Blocks.COARSE_DIRT,
			Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.DIRT.defaultBlockState())),
			Blocks.ROOTED_DIRT,
			Pair.of(arg -> true, changeIntoStateAndDropItem(Blocks.DIRT.defaultBlockState(), Items.HANGING_ROOTS))
		)
	);

	public HoeItem(ToolMaterial arg, float f, float g, Item.Properties arg2) {
		super(arg, BlockTags.MINEABLE_WITH_HOE, f, g, arg2);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Level level = arg.getLevel();
		BlockPos blockpos = arg.getClickedPos();
		BlockState toolModifiedState = level.getBlockState(blockpos).getToolModifiedState(arg, ItemAbilities.HOE_TILL, false);
		Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> pair = toolModifiedState == null ? null : Pair.of(ctx -> true, changeIntoState(toolModifiedState));
		if (pair == null) {
			return InteractionResult.PASS;
		} else {
			Predicate<UseOnContext> predicate = pair.getFirst();
			Consumer<UseOnContext> consumer = pair.getSecond();
			if (predicate.test(arg)) {
				Player player = arg.getPlayer();
				level.playSound(player, blockpos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
				if (!level.isClientSide) {
					consumer.accept(arg);
					if (player != null) {
						arg.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(arg.getHand()));
					}
				}

				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.PASS;
			}
		}
	}

	public static Consumer<UseOnContext> changeIntoState(BlockState arg) {
		return arg2 -> {
			arg2.getLevel().setBlock(arg2.getClickedPos(), arg, 11);
			arg2.getLevel().gameEvent(GameEvent.BLOCK_CHANGE, arg2.getClickedPos(), GameEvent.Context.of(arg2.getPlayer(), arg));
		};
	}

	public static Consumer<UseOnContext> changeIntoStateAndDropItem(BlockState arg, ItemLike arg2) {
		return arg3 -> {
			arg3.getLevel().setBlock(arg3.getClickedPos(), arg, 11);
			arg3.getLevel().gameEvent(GameEvent.BLOCK_CHANGE, arg3.getClickedPos(), GameEvent.Context.of(arg3.getPlayer(), arg));
			Block.popResourceFromFace(arg3.getLevel(), arg3.getClickedPos(), arg3.getClickedFace(), new ItemStack(arg2));
		};
	}

	public static boolean onlyIfAirAbove(UseOnContext arg) {
		return arg.getClickedFace() != Direction.DOWN && arg.getLevel().getBlockState(arg.getClickedPos().above()).isAir();
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_HOE_ACTIONS.contains(itemAbility);
	}
}
