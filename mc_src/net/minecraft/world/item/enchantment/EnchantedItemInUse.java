package net.minecraft.world.item.enchantment;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record EnchantedItemInUse(ItemStack itemStack, @Nullable EquipmentSlot inSlot, @Nullable LivingEntity owner, Consumer<Item> onBreak) {
	public EnchantedItemInUse(ItemStack arg, EquipmentSlot arg2, LivingEntity arg3) {
		this(arg, arg2, arg3, arg3x -> arg3.onEquippedItemBroken(arg3x, arg2));
	}
}
