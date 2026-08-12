package net.minecraft.world.item.crafting.display;

import net.minecraft.core.Registry;

public class RecipeDisplays {
	public static RecipeDisplay.Type<?> bootstrap(Registry<RecipeDisplay.Type<?>> arg) {
		Registry.register(arg, "crafting_shapeless", ShapelessCraftingRecipeDisplay.TYPE);
		Registry.register(arg, "crafting_shaped", ShapedCraftingRecipeDisplay.TYPE);
		Registry.register(arg, "furnace", FurnaceRecipeDisplay.TYPE);
		Registry.register(arg, "stonecutter", StonecutterRecipeDisplay.TYPE);
		return Registry.register(arg, "smithing", SmithingRecipeDisplay.TYPE);
	}
}
