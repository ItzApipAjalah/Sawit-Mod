package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class SolidBucketItem extends BlockItem implements DispensibleContainerItem {
	private final SoundEvent placeSound;

	public SolidBucketItem(Block arg, SoundEvent arg2, Item.Properties arg3) {
		super(arg, arg3);
		this.placeSound = arg2;
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		InteractionResult interactionResult = super.useOn(arg);
		Player player = arg.getPlayer();
		if (interactionResult.consumesAction() && player != null) {
			player.setItemInHand(arg.getHand(), BucketItem.getEmptySuccessItem(arg.getItemInHand(), player));
		}

		return interactionResult;
	}

	@Override
	protected SoundEvent getPlaceSound(BlockState arg) {
		return this.placeSound;
	}

	@Override
	public boolean emptyContents(@Nullable Player arg, Level arg2, BlockPos arg3, @Nullable BlockHitResult arg4) {
		if (arg2.isInWorldBounds(arg3) && arg2.isEmptyBlock(arg3)) {
			if (!arg2.isClientSide) {
				arg2.setBlock(arg3, this.getBlock().defaultBlockState(), 3);
			}

			arg2.gameEvent(arg, GameEvent.FLUID_PLACE, arg3);
			arg2.playSound(arg, arg3, this.placeSound, SoundSource.BLOCKS, 1.0F, 1.0F);
			return true;
		} else {
			return false;
		}
	}
}
