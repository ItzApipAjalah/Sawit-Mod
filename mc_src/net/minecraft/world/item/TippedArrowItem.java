package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class TippedArrowItem extends ArrowItem {
	public TippedArrowItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public ItemStack getDefaultInstance() {
		ItemStack itemStack = super.getDefaultInstance();
		itemStack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
		return itemStack;
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		PotionContents potionContents = arg.get(DataComponents.POTION_CONTENTS);
		if (potionContents != null) {
			potionContents.addPotionTooltip(list::add, 0.125F, arg2.tickRate());
		}
	}

	@Override
	public Component getName(ItemStack arg) {
		PotionContents potionContents = arg.get(DataComponents.POTION_CONTENTS);
		return potionContents != null ? potionContents.getName(this.descriptionId + ".effect.") : super.getName(arg);
	}
}
