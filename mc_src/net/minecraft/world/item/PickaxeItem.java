package net.minecraft.world.item;

import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class PickaxeItem extends DiggerItem {
	public PickaxeItem(ToolMaterial arg, float f, float g, Item.Properties arg2) {
		super(arg, BlockTags.MINEABLE_WITH_PICKAXE, f, g, arg2);
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility);
	}
}
