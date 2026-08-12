package net.minecraft.world.item.component;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ConsumableListener {
	void onConsume(Level arg, LivingEntity arg2, ItemStack arg3, Consumable arg4);
}
