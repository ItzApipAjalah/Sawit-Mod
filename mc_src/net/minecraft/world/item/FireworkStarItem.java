package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.FireworkExplosion;

public class FireworkStarItem extends Item {
	public FireworkStarItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		FireworkExplosion fireworkExplosion = arg.get(DataComponents.FIREWORK_EXPLOSION);
		if (fireworkExplosion != null) {
			fireworkExplosion.addToTooltip(arg2, list::add, arg3);
		}
	}
}
