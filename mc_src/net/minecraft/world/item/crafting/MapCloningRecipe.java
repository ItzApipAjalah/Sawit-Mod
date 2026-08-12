package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class MapCloningRecipe extends CustomRecipe {
	public MapCloningRecipe(CraftingBookCategory arg) {
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
					if (itemStack.has(DataComponents.MAP_ID)) {
						if (bl2) {
							return false;
						}

						bl2 = true;
					} else {
						if (!itemStack.is(Items.MAP)) {
							return false;
						}

						bl = true;
					}
				}
			}

			return bl2 && bl;
		}
	}

	public ItemStack assemble(CraftingInput arg, HolderLookup.Provider arg2) {
		int i = 0;
		ItemStack itemStack = ItemStack.EMPTY;

		for (int j = 0; j < arg.size(); j++) {
			ItemStack itemStack2 = arg.getItem(j);
			if (!itemStack2.isEmpty()) {
				if (itemStack2.has(DataComponents.MAP_ID)) {
					if (!itemStack.isEmpty()) {
						return ItemStack.EMPTY;
					}

					itemStack = itemStack2;
				} else {
					if (!itemStack2.is(Items.MAP)) {
						return ItemStack.EMPTY;
					}

					i++;
				}
			}
		}

		return !itemStack.isEmpty() && i >= 1 ? itemStack.copyWithCount(i + 1) : ItemStack.EMPTY;
	}

	@Override
	public RecipeSerializer<MapCloningRecipe> getSerializer() {
		return RecipeSerializer.MAP_CLONING;
	}
}
