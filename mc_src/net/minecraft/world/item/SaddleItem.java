package net.minecraft.world.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;

public class SaddleItem extends Item {
	public SaddleItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack arg, Player arg2, LivingEntity arg3, InteractionHand arg4) {
		if (arg3 instanceof Saddleable saddleable && arg3.isAlive() && !saddleable.isSaddled() && saddleable.isSaddleable()) {
			if (!arg2.level().isClientSide) {
				saddleable.equipSaddle(arg.split(1), SoundSource.NEUTRAL);
				arg3.level().gameEvent(arg3, GameEvent.EQUIP, arg3.position());
			}

			return InteractionResult.SUCCESS;
		} else {
			return InteractionResult.PASS;
		}
	}
}
