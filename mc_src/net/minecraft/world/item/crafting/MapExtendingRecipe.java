package net.minecraft.world.item.crafting;

import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapExtendingRecipe extends ShapedRecipe {
	public MapExtendingRecipe(CraftingBookCategory arg) {
		super(
			"",
			arg,
			ShapedRecipePattern.of(Map.of('#', Ingredient.of(Items.PAPER), 'x', Ingredient.of(Items.FILLED_MAP)), "###", "#x#", "###"),
			new ItemStack(Items.MAP)
		);
	}

	@Override
	public boolean matches(CraftingInput arg, Level arg2) {
		if (!super.matches(arg, arg2)) {
			return false;
		} else {
			ItemStack itemStack = findFilledMap(arg);
			if (itemStack.isEmpty()) {
				return false;
			} else {
				MapItemSavedData mapItemSavedData = MapItem.getSavedData(itemStack, arg2);
				if (mapItemSavedData == null) {
					return false;
				} else {
					return mapItemSavedData.isExplorationMap() ? false : mapItemSavedData.scale < 4;
				}
			}
		}
	}

	@Override
	public ItemStack assemble(CraftingInput arg, HolderLookup.Provider arg2) {
		ItemStack itemStack = findFilledMap(arg).copyWithCount(1);
		itemStack.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.SCALE);
		return itemStack;
	}

	private static ItemStack findFilledMap(CraftingInput arg) {
		for (int i = 0; i < arg.size(); i++) {
			ItemStack itemStack = arg.getItem(i);
			if (itemStack.has(DataComponents.MAP_ID)) {
				return itemStack;
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<MapExtendingRecipe> getSerializer() {
		return RecipeSerializer.MAP_EXTENDING;
	}
}
