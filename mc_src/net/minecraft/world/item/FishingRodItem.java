package net.minecraft.world.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.EventHooks;

public class FishingRodItem extends Item {
	public FishingRodItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemstack = arg2.getItemInHand(arg3);
		if (arg2.fishing != null) {
			if (!arg.isClientSide) {
				int i = arg2.fishing.retrieve(itemstack);
				ItemStack original = itemstack.copy();
				itemstack.hurtAndBreak(i, arg2, LivingEntity.getSlotForHand(arg3));
				if (itemstack.isEmpty()) {
					EventHooks.onPlayerDestroyItem(arg2, original, arg3);
				}
			}

			arg.playSound(
				null,
				arg2.getX(),
				arg2.getY(),
				arg2.getZ(),
				SoundEvents.FISHING_BOBBER_RETRIEVE,
				SoundSource.NEUTRAL,
				1.0F,
				0.4F / (arg.getRandom().nextFloat() * 0.4F + 0.8F)
			);
			arg2.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
		} else {
			arg.playSound(
				null,
				arg2.getX(),
				arg2.getY(),
				arg2.getZ(),
				SoundEvents.FISHING_BOBBER_THROW,
				SoundSource.NEUTRAL,
				0.5F,
				0.4F / (arg.getRandom().nextFloat() * 0.4F + 0.8F)
			);
			if (arg instanceof ServerLevel serverlevel) {
				int j = (int)(EnchantmentHelper.getFishingTimeReduction(serverlevel, itemstack, arg2) * 20.0F);
				int k = EnchantmentHelper.getFishingLuckBonus(serverlevel, itemstack, arg2);
				Projectile.spawnProjectile(new FishingHook(arg2, arg, k, j, itemstack), serverlevel, itemstack);
			}

			arg2.awardStat(Stats.ITEM_USED.get(this));
			arg2.gameEvent(GameEvent.ITEM_INTERACT_START);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_FISHING_ROD_ACTIONS.contains(itemAbility);
	}
}
