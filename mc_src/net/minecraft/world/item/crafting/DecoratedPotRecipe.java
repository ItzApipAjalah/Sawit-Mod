package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;

public class DecoratedPotRecipe extends CustomRecipe {
	public DecoratedPotRecipe(CraftingBookCategory arg) {
		super(arg);
	}

	private static ItemStack back(CraftingInput arg) {
		return arg.getItem(1, 0);
	}

	private static ItemStack left(CraftingInput arg) {
		return arg.getItem(0, 1);
	}

	private static ItemStack right(CraftingInput arg) {
		return arg.getItem(2, 1);
	}

	private static ItemStack front(CraftingInput arg) {
		return arg.getItem(1, 2);
	}

	public boolean matches(CraftingInput arg, Level arg2) {
		return arg.width() == 3 && arg.height() == 3 && arg.ingredientCount() == 4
			? back(arg).is(ItemTags.DECORATED_POT_INGREDIENTS)
				&& left(arg).is(ItemTags.DECORATED_POT_INGREDIENTS)
				&& right(arg).is(ItemTags.DECORATED_POT_INGREDIENTS)
				&& front(arg).is(ItemTags.DECORATED_POT_INGREDIENTS)
			: false;
	}

	public ItemStack assemble(CraftingInput arg, HolderLookup.Provider arg2) {
		PotDecorations potDecorations = new PotDecorations(back(arg).getItem(), left(arg).getItem(), right(arg).getItem(), front(arg).getItem());
		return DecoratedPotBlockEntity.createDecoratedPotItem(potDecorations);
	}

	@Override
	public RecipeSerializer<DecoratedPotRecipe> getSerializer() {
		return RecipeSerializer.DECORATED_POT_RECIPE;
	}
}
