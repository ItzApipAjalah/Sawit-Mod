package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

public class ShapelessRecipe implements CraftingRecipe {
	final String group;
	final CraftingBookCategory category;
	final ItemStack result;
	final List<Ingredient> ingredients;
	@Nullable
	private PlacementInfo placementInfo;
	private final boolean isSimple;

	public ShapelessRecipe(String string, CraftingBookCategory arg, ItemStack arg2, List<Ingredient> list) {
		this.group = string;
		this.category = arg;
		this.result = arg2;
		this.ingredients = list;
		this.isSimple = list.stream().allMatch(Ingredient::isSimple);
	}

	@Override
	public RecipeSerializer<ShapelessRecipe> getSerializer() {
		return RecipeSerializer.SHAPELESS_RECIPE;
	}

	@Override
	public String group() {
		return this.group;
	}

	@Override
	public CraftingBookCategory category() {
		return this.category;
	}

	@Override
	public PlacementInfo placementInfo() {
		if (this.placementInfo == null) {
			this.placementInfo = PlacementInfo.create(this.ingredients);
		}

		return this.placementInfo;
	}

	public boolean matches(CraftingInput arg, Level arg2) {
		if (arg.ingredientCount() != this.ingredients.size()) {
			return false;
		} else if (!this.isSimple) {
			ArrayList<ItemStack> nonEmptyItems = new ArrayList(arg.ingredientCount());

			for (ItemStack item : arg.items()) {
				if (!item.isEmpty()) {
					nonEmptyItems.add(item);
				}
			}

			return RecipeMatcher.findMatches(nonEmptyItems, this.ingredients) != null;
		} else {
			return arg.size() == 1 && this.ingredients.size() == 1
				? ((Ingredient)this.ingredients.getFirst()).test(arg.getItem(0))
				: arg.stackedContents().canCraft(this, null);
		}
	}

	public ItemStack assemble(CraftingInput arg, HolderLookup.Provider arg2) {
		return this.result.copy();
	}

	@Override
	public List<RecipeDisplay> display() {
		return List.of(
			new ShapelessCraftingRecipeDisplay(
				this.ingredients.stream().map(Ingredient::display).toList(),
				new SlotDisplay.ItemStackSlotDisplay(this.result),
				new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
			)
		);
	}

	public static class Serializer implements RecipeSerializer<ShapelessRecipe> {
		private static final MapCodec<ShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codec.STRING.optionalFieldOf("group", "").forGetter(arg -> arg.group),
					CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(arg -> arg.category),
					ItemStack.STRICT_CODEC.fieldOf("result").forGetter(arg -> arg.result),
					Codec.lazyInitialized(() -> Ingredient.CODEC.listOf(1, ShapedRecipePattern.maxHeight * ShapedRecipePattern.maxWidth))
						.fieldOf("ingredients")
						.forGetter(arg -> arg.ingredients)
				)
				.apply(instance, ShapelessRecipe::new)
		);
		public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			arg -> arg.group,
			CraftingBookCategory.STREAM_CODEC,
			arg -> arg.category,
			ItemStack.STREAM_CODEC,
			arg -> arg.result,
			Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
			arg -> arg.ingredients,
			ShapelessRecipe::new
		);

		@Override
		public MapCodec<ShapelessRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
