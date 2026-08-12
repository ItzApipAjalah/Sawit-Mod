package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignItem extends StandingAndWallBlockItem {
	public SignItem(Block arg, Block arg2, Item.Properties arg3) {
		super(arg, arg2, Direction.DOWN, arg3);
	}

	public SignItem(Item.Properties arg, Block arg2, Block arg3, Direction arg4) {
		super(arg2, arg3, arg4, arg);
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos arg, Level arg2, @Nullable Player arg3, ItemStack arg4, BlockState arg5) {
		boolean bl = super.updateCustomBlockEntityTag(arg, arg2, arg3, arg4, arg5);
		if (!arg2.isClientSide
			&& !bl
			&& arg3 != null
			&& arg2.getBlockEntity(arg) instanceof SignBlockEntity signBlockEntity
			&& arg2.getBlockState(arg).getBlock() instanceof SignBlock signBlock) {
			signBlock.openTextEdit(arg3, signBlockEntity, true);
		}

		return bl;
	}
}
