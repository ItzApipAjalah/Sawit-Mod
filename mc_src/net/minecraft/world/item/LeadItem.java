package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class LeadItem extends Item {
	public LeadItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Level level = arg.getLevel();
		BlockPos blockPos = arg.getClickedPos();
		BlockState blockState = level.getBlockState(blockPos);
		if (blockState.is(BlockTags.FENCES)) {
			Player player = arg.getPlayer();
			if (!level.isClientSide && player != null) {
				return bindPlayerMobs(player, level, blockPos);
			}
		}

		return InteractionResult.PASS;
	}

	public static InteractionResult bindPlayerMobs(Player arg, Level arg2, BlockPos arg3) {
		LeashFenceKnotEntity leashFenceKnotEntity = null;
		List<Leashable> list = leashableInArea(arg2, arg3, arg2x -> arg2x.getLeashHolder() == arg);

		for (Leashable leashable : list) {
			if (leashFenceKnotEntity == null) {
				leashFenceKnotEntity = LeashFenceKnotEntity.getOrCreateKnot(arg2, arg3);
				leashFenceKnotEntity.playPlacementSound();
			}

			leashable.setLeashedTo(leashFenceKnotEntity, true);
		}

		if (!list.isEmpty()) {
			arg2.gameEvent(GameEvent.BLOCK_ATTACH, arg3, GameEvent.Context.of(arg));
			return InteractionResult.SUCCESS_SERVER;
		} else {
			return InteractionResult.PASS;
		}
	}

	public static List<Leashable> leashableInArea(Level arg, BlockPos arg2, Predicate<Leashable> predicate) {
		double d = 7.0;
		int i = arg2.getX();
		int j = arg2.getY();
		int k = arg2.getZ();
		AABB aABB = new AABB(i - 7.0, j - 7.0, k - 7.0, i + 7.0, j + 7.0, k + 7.0);
		return arg.getEntitiesOfClass(Entity.class, aABB, argx -> argx instanceof Leashable leashable && predicate.test(leashable))
			.stream()
			.map(Leashable.class::cast)
			.toList();
	}
}
