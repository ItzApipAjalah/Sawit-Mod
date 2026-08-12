package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BottleItem extends Item {
	public BottleItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		List<AreaEffectCloud> list = arg.getEntitiesOfClass(
			AreaEffectCloud.class, arg2.getBoundingBox().inflate(2.0), argx -> argx != null && argx.isAlive() && argx.getOwner() instanceof EnderDragon
		);
		ItemStack itemStack = arg2.getItemInHand(arg3);
		if (!list.isEmpty()) {
			AreaEffectCloud areaEffectCloud = (AreaEffectCloud)list.get(0);
			areaEffectCloud.setRadius(areaEffectCloud.getRadius() - 0.5F);
			arg.playSound(null, arg2.getX(), arg2.getY(), arg2.getZ(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.NEUTRAL, 1.0F, 1.0F);
			arg.gameEvent(arg2, GameEvent.FLUID_PICKUP, arg2.position());
			if (arg2 instanceof ServerPlayer serverPlayer) {
				CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(serverPlayer, itemStack, areaEffectCloud);
			}

			return InteractionResult.SUCCESS.heldItemTransformedTo(this.turnBottleIntoItem(itemStack, arg2, new ItemStack(Items.DRAGON_BREATH)));
		} else {
			BlockHitResult blockHitResult = getPlayerPOVHitResult(arg, arg2, ClipContext.Fluid.SOURCE_ONLY);
			if (blockHitResult.getType() == HitResult.Type.MISS) {
				return InteractionResult.PASS;
			} else {
				if (blockHitResult.getType() == HitResult.Type.BLOCK) {
					BlockPos blockPos = blockHitResult.getBlockPos();
					if (!arg.mayInteract(arg2, blockPos)) {
						return InteractionResult.PASS;
					}

					if (arg.getFluidState(blockPos).is(FluidTags.WATER)) {
						arg.playSound(arg2, arg2.getX(), arg2.getY(), arg2.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
						arg.gameEvent(arg2, GameEvent.FLUID_PICKUP, blockPos);
						return InteractionResult.SUCCESS
							.heldItemTransformedTo(this.turnBottleIntoItem(itemStack, arg2, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
					}
				}

				return InteractionResult.PASS;
			}
		}
	}

	protected ItemStack turnBottleIntoItem(ItemStack arg, Player arg2, ItemStack arg3) {
		arg2.awardStat(Stats.ITEM_USED.get(this));
		return ItemUtils.createFilledResult(arg, arg2, arg3);
	}
}
