package net.minecraft.world.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

public class AirItem extends Item {
	private final Block block;

	public AirItem(Block arg, Item.Properties arg2) {
		super(arg2);
		this.block = arg;
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		super.appendHoverText(arg, arg2, list, arg3);
		this.block.appendHoverText(arg, arg2, list, arg3);
	}

	@Override
	public Component getName(ItemStack arg) {
		return this.getName();
	}
}
