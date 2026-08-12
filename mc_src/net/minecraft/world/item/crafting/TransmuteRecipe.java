package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public class TransmuteRecipe implements CraftingRecipe {
	final String group;
	final CraftingBookCategory category;
	final Ingredient input;
	final Ingredient material;
	final Holder<Item> result;
	@Nullable
	private PlacementInfo placementInfo;

	public TransmuteRecipe(String string, CraftingBookCategory arg, Ingredient arg2, Ingredient arg3, Holder<Item> arg4) {
		this.group = string;
		this.category = arg;
		this.input = arg2;
		this.material = arg3;
		this.result = arg4;
	}

	public boolean matches(CraftingInput arg, Level arg2) {
		if (arg.ingredientCount() != 2) {
			return false;
		} else {
			boolean bl = false;
			boolean bl2 = false;

			for (int i = 0; i < arg.size(); i++) {
				ItemStack itemStack = arg.getItem(i);
				if (!itemStack.isEmpty()) {
					if (!bl && this.input.test(itemStack) && itemStack.getItem() != this.result.value()) {
						bl = true;
					} else {
						if (bl2 || !this.material.test(itemStack)) {
							return false;
						}

						bl2 = true;
					}
				}
			}

			return bl && bl2;
		}
	}

	public ItemStack assemble(CraftingInput arg, HolderLookup.Provider arg2) {
		ItemStack itemStack = ItemStack.EMPTY;

		for (int i = 0; i < arg.size(); i++) {
			ItemStack itemStack2 = arg.getItem(i);
			if (!itemStack2.isEmpty() && this.input.test(itemStack2) && itemStack2.getItem() != this.result.value()) {
				itemStack = itemStack2;
			}
		}

		return itemStack.transmuteCopy(this.result.value(), 1);
	}

	@Override
	public List<RecipeDisplay> display() {
		return List.of(
			new ShapelessCraftingRecipeDisplay(
				List.of(this.input.display(), this.material.display()), new SlotDisplay.ItemSlotDisplay(this.result), new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
			)
		);
	}

	@Override
	public RecipeSerializer<TransmuteRecipe> getSerializer() {
		return RecipeSerializer.TRANSMUTE;
	}

	@Override
	public String group() {
		return this.group;
	}

	@Override
	public PlacementInfo placementInfo() {
		if (this.placementInfo == null) {
			this.placementInfo = PlacementInfo.create(List.of(this.input, this.material));
		}

		return this.placementInfo;
	}

	@Override
	public CraftingBookCategory category() {
		return this.category;
	}

	public static class Serializer implements RecipeSerializer<TransmuteRecipe> {
		private static final MapCodec<TransmuteRecipe> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					Codec.STRING.optionalFieldOf("group", "").forGetter(arg -> arg.group),
					CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(arg -> arg.category),
					Ingredient.CODEC.fieldOf("input").forGetter(arg -> arg.input),
					Ingredient.CODEC.fieldOf("material").forGetter(arg -> arg.material),
					Item.CODEC.fieldOf("result").forGetter(arg -> arg.result)
				)
				.apply(instance, TransmuteRecipe::new)
		);
		public static final StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			arg -> arg.group,
			CraftingBookCategory.STREAM_CODEC,
			arg -> arg.category,
			Ingredient.CONTENTS_STREAM_CODEC,
			arg -> arg.input,
			Ingredient.CONTENTS_STREAM_CODEC,
			arg -> arg.material,
			ByteBufCodecs.holderRegistry(Registries.ITEM),
			arg -> arg.result,
			TransmuteRecipe::new
		);

		@Override
		public MapCodec<TransmuteRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
