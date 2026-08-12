package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class SwordItem extends Item {
	public SwordItem(ToolMaterial arg, float f, float g, Item.Properties arg2) {
		super(arg.applySwordProperties(arg2, f, g));
	}

	public SwordItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public boolean canAttackBlock(BlockState arg, Level arg2, BlockPos arg3, Player arg4) {
		return !arg4.isCreative();
	}

	@Override
	public boolean hurtEnemy(ItemStack arg, LivingEntity arg2, LivingEntity arg3) {
		return true;
	}

	@Override
	public void postHurtEnemy(ItemStack arg, LivingEntity arg2, LivingEntity arg3) {
		arg.hurtAndBreak(1, arg3, EquipmentSlot.MAINHAND);
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(itemAbility);
	}
}
