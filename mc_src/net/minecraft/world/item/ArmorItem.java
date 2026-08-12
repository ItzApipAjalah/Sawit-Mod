package net.minecraft.world.item;

import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public class ArmorItem extends Item {
	public ArmorItem(ArmorMaterial arg, ArmorType arg2, Item.Properties arg3) {
		super(arg.humanoidProperties(arg3, arg2));
	}
}
