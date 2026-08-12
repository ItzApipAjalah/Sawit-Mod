package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class NameTagItem extends Item {
	public NameTagItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack arg, Player arg2, LivingEntity arg3, InteractionHand arg4) {
		Component component = arg.get(DataComponents.CUSTOM_NAME);
		if (component != null && arg3.getType().canSerialize()) {
			if (!arg2.level().isClientSide && arg3.isAlive()) {
				arg3.setCustomName(component);
				if (arg3 instanceof Mob mob) {
					mob.setPersistenceRequired();
				}

				arg.shrink(1);
			}

			return InteractionResult.SUCCESS;
		} else {
			return InteractionResult.PASS;
		}
	}
}
