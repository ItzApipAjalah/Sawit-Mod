package net.minecraft.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public class PlaceOnWaterBlockItem extends BlockItem {
	public PlaceOnWaterBlockItem(Block arg, Item.Properties arg2) {
		super(arg, arg2);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		BlockHitResult blockHitResult = getPlayerPOVHitResult(arg, arg2, ClipContext.Fluid.SOURCE_ONLY);
		BlockHitResult blockHitResult2 = blockHitResult.withPosition(blockHitResult.getBlockPos().above());
		return super.useOn(new UseOnContext(arg2, arg3, blockHitResult2));
	}
}
