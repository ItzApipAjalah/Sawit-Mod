package net.minecraft.world.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EmptyMapItem extends Item {
	public EmptyMapItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemStack = arg2.getItemInHand(arg3);
		if (arg.isClientSide) {
			return InteractionResult.SUCCESS;
		} else {
			itemStack.consume(1, arg2);
			arg2.awardStat(Stats.ITEM_USED.get(this));
			arg2.level().playSound(null, arg2, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, arg2.getSoundSource(), 1.0F, 1.0F);
			ItemStack itemStack2 = MapItem.create(arg, arg2.getBlockX(), arg2.getBlockZ(), (byte)0, true, false);
			if (itemStack.isEmpty()) {
				return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack2);
			} else {
				if (!arg2.getInventory().add(itemStack2.copy())) {
					arg2.drop(itemStack2, false);
				}

				return InteractionResult.SUCCESS;
			}
		}
	}
}
