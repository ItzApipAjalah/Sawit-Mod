package net.minecraft.world.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;

public class DiggerItem extends Item {
	public DiggerItem(ToolMaterial arg, TagKey<Block> arg2, float f, float g, Item.Properties arg3) {
		super(arg.applyToolProperties(arg3, arg2, f, g));
	}

	@Override
	public boolean hurtEnemy(ItemStack arg, LivingEntity arg2, LivingEntity arg3) {
		return true;
	}

	@Override
	public void postHurtEnemy(ItemStack arg, LivingEntity arg2, LivingEntity arg3) {
		arg.hurtAndBreak(2, arg3, EquipmentSlot.MAINHAND);
	}
}
