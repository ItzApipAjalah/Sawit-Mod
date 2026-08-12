package net.minecraft.world.item.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;

public class ArmorDyeRecipe extends CustomRecipe {
	public ArmorDyeRecipe(CraftingBookCategory arg) {
		super(arg);
	}

	public boolean matches(CraftingInput arg, Level arg2) {
		if (arg.ingredientCount() < 2) {
			return false;
		} else {
			boolean bl = false;
			boolean bl2 = false;

			for (int i = 0; i < arg.size(); i++) {
				ItemStack itemStack = arg.getItem(i);
				if (!itemStack.isEmpty()) {
					if (itemStack.is(ItemTags.DYEABLE)) {
						if (bl) {
							return false;
						}

						bl = true;
					} else {
						if (!(itemStack.getItem() instanceof DyeItem)) {
							return false;
						}

						bl2 = true;
					}
				}
			}

			return bl2 && bl;
		}
	}

	public ItemStack assemble(CraftingInput arg, HolderLookup.Provider arg2) {
		List<DyeItem> list = new ArrayList();
		ItemStack itemStack = ItemStack.EMPTY;

		for (int i = 0; i < arg.size(); i++) {
			ItemStack itemStack2 = arg.getItem(i);
			if (!itemStack2.isEmpty()) {
				if (itemStack2.is(ItemTags.DYEABLE)) {
					if (!itemStack.isEmpty()) {
						return ItemStack.EMPTY;
					}

					itemStack = itemStack2.copy();
				} else {
					if (!(itemStack2.getItem() instanceof DyeItem dyeItem)) {
						return ItemStack.EMPTY;
					}

					list.add(dyeItem);
				}
			}
		}

		return !itemStack.isEmpty() && !list.isEmpty() ? DyedItemColor.applyDyes(itemStack, list) : ItemStack.EMPTY;
	}

	@Override
	public RecipeSerializer<ArmorDyeRecipe> getSerializer() {
		return RecipeSerializer.ARMOR_DYE;
	}
}
