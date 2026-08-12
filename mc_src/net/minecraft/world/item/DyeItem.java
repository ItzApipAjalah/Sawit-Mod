package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class DyeItem extends Item implements SignApplicator {
	private static final Map<DyeColor, DyeItem> ITEM_BY_COLOR = Maps.newEnumMap(DyeColor.class);
	private final DyeColor dyeColor;

	public DyeItem(DyeColor arg, Item.Properties arg2) {
		super(arg2);
		this.dyeColor = arg;
		ITEM_BY_COLOR.put(arg, this);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack arg, Player arg2, LivingEntity arg3, InteractionHand arg4) {
		if (arg3 instanceof Sheep sheep && sheep.isAlive() && !sheep.isSheared() && sheep.getColor() != this.dyeColor) {
			sheep.level().playSound(arg2, sheep, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
			if (!arg2.level().isClientSide) {
				sheep.setColor(this.dyeColor);
				arg.shrink(1);
			}

			return InteractionResult.SUCCESS;
		} else {
			return InteractionResult.PASS;
		}
	}

	public DyeColor getDyeColor() {
		return this.dyeColor;
	}

	public static DyeItem byColor(DyeColor arg) {
		return (DyeItem)ITEM_BY_COLOR.get(arg);
	}

	@Override
	public boolean tryApplyToSign(Level arg, SignBlockEntity arg2, boolean bl, Player arg3) {
		if (arg2.updateText(argx -> argx.setColor(this.getDyeColor()), bl)) {
			arg.playSound(null, arg2.getBlockPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			return true;
		} else {
			return false;
		}
	}
}
