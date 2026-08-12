package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public interface CraftingRecipe extends Recipe<CraftingInput> {
	@Override
	default RecipeType<CraftingRecipe> getType() {
		return RecipeType.CRAFTING;
	}

	@Override
	RecipeSerializer<? extends CraftingRecipe> getSerializer();

	CraftingBookCategory category();

	default NonNullList<ItemStack> getRemainingItems(CraftingInput arg) {
		return defaultCraftingReminder(arg);
	}

	static NonNullList<ItemStack> defaultCraftingReminder(CraftingInput arg) {
		NonNullList<ItemStack> nonnulllist = NonNullList.withSize(arg.size(), ItemStack.EMPTY);

		for (int i = 0; i < nonnulllist.size(); i++) {
			ItemStack item = arg.getItem(i);
			nonnulllist.set(i, item.getCraftingRemainder());
		}

		return nonnulllist;
	}

	@Override
	default RecipeBookCategory recipeBookCategory() {
		return switch (this.category()) {
			case BUILDING -> RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
			case EQUIPMENT -> RecipeBookCategories.CRAFTING_EQUIPMENT;
			case REDSTONE -> RecipeBookCategories.CRAFTING_REDSTONE;
			case MISC -> RecipeBookCategories.CRAFTING_MISC;
		};
	}
}
