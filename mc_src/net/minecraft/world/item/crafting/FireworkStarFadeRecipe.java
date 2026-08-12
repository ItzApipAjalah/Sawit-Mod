package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.Level;

public class FireworkStarFadeRecipe extends CustomRecipe {
	private static final Ingredient STAR_INGREDIENT = Ingredient.of(Items.FIREWORK_STAR);

	public FireworkStarFadeRecipe(CraftingBookCategory arg) {
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
					if (itemStack.getItem() instanceof DyeItem) {
						bl = true;
					} else {
						if (!STAR_INGREDIENT.test(itemStack)) {
							return false;
						}

						if (bl2) {
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
		IntList intList = new IntArrayList();
		ItemStack itemStack = null;

		for (int i = 0; i < arg.size(); i++) {
			ItemStack itemStack2 = arg.getItem(i);
			if (itemStack2.getItem() instanceof DyeItem dyeItem) {
				intList.add(dyeItem.getDyeColor().getFireworkColor());
			} else if (STAR_INGREDIENT.test(itemStack2)) {
				itemStack = itemStack2.copyWithCount(1);
			}
		}

		if (itemStack != null && !intList.isEmpty()) {
			itemStack.update(DataComponents.FIREWORK_EXPLOSION, FireworkExplosion.DEFAULT, intList, FireworkExplosion::withFadeColors);
			return itemStack;
		} else {
			return ItemStack.EMPTY;
		}
	}

	@Override
	public RecipeSerializer<FireworkStarFadeRecipe> getSerializer() {
		return RecipeSerializer.FIREWORK_STAR_FADE;
	}
}
