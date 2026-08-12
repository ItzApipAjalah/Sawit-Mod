package net.minecraft.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ItemUtils {
	public static InteractionResult startUsingInstantly(Level arg, Player arg2, InteractionHand arg3) {
		arg2.startUsingItem(arg3);
		return InteractionResult.CONSUME;
	}

	public static ItemStack createFilledResult(ItemStack arg, Player arg2, ItemStack arg3, boolean bl) {
		boolean bl2 = arg2.hasInfiniteMaterials();
		if (bl && bl2) {
			if (!arg2.getInventory().contains(arg3)) {
				arg2.getInventory().add(arg3);
			}

			return arg;
		} else {
			arg.consume(1, arg2);
			if (arg.isEmpty()) {
				return arg3;
			} else {
				if (!arg2.getInventory().add(arg3)) {
					arg2.drop(arg3, false);
				}

				return arg;
			}
		}
	}

	public static ItemStack createFilledResult(ItemStack arg, Player arg2, ItemStack arg3) {
		return createFilledResult(arg, arg2, arg3, true);
	}

	public static void onContainerDestroyed(ItemEntity arg, Iterable<ItemStack> iterable) {
		Level level = arg.level();
		if (!level.isClientSide) {
			iterable.forEach(arg3 -> level.addFreshEntity(new ItemEntity(level, arg.getX(), arg.getY(), arg.getZ(), arg3)));
		}
	}
}
