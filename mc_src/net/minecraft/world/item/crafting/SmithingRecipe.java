package net.minecraft.world.item.crafting;

import java.util.Optional;
import net.minecraft.world.level.Level;

public interface SmithingRecipe extends Recipe<SmithingRecipeInput> {
	@Override
	default RecipeType<SmithingRecipe> getType() {
		return RecipeType.SMITHING;
	}

	@Override
	RecipeSerializer<? extends SmithingRecipe> getSerializer();

	default boolean matches(SmithingRecipeInput arg, Level arg2) {
		return Ingredient.testOptionalIngredient(this.templateIngredient(), arg.template())
			&& Ingredient.testOptionalIngredient(this.baseIngredient(), arg.base())
			&& Ingredient.testOptionalIngredient(this.additionIngredient(), arg.addition());
	}

	Optional<Ingredient> templateIngredient();

	Optional<Ingredient> baseIngredient();

	Optional<Ingredient> additionIngredient();

	@Override
	default RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.SMITHING;
	}
}
