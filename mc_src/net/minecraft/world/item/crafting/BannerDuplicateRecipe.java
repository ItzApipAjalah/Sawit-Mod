package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class BannerDuplicateRecipe extends CustomRecipe {
	public BannerDuplicateRecipe(CraftingBookCategory arg) {
		super(arg);
	}

	public boolean matches(CraftingInput arg, Level arg2) {
		if (arg.ingredientCount() != 2) {
			return false;
		} else {
			DyeColor dyecolor = null;
			boolean flag = false;
			boolean flag1 = false;

			for (int i = 0; i < arg.size(); i++) {
				ItemStack itemstack = arg.getItem(i);
				if (!itemstack.isEmpty()) {
					if (!(itemstack.getItem() instanceof BannerItem banneritem)) {
						return false;
					}

					if (dyecolor == null) {
						dyecolor = banneritem.getColor();
					} else if (dyecolor != banneritem.getColor()) {
						return false;
					}

					int j = itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size();
					if (j > 6) {
						return false;
					}

					if (j > 0) {
						if (flag1) {
							return false;
						}

						flag1 = true;
					} else {
						if (flag) {
							return false;
						}

						flag = true;
					}
				}
			}

			return flag1 && flag;
		}
	}

	public ItemStack assemble(CraftingInput arg, HolderLookup.Provider arg2) {
		for (int i = 0; i < arg.size(); i++) {
			ItemStack itemstack = arg.getItem(i);
			if (!itemstack.isEmpty()) {
				int j = itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size();
				if (j > 0 && j <= 6) {
					return itemstack.copyWithCount(1);
				}
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput arg) {
		NonNullList<ItemStack> nonnulllist = NonNullList.withSize(arg.size(), ItemStack.EMPTY);

		for (int i = 0; i < nonnulllist.size(); i++) {
			ItemStack itemstack = arg.getItem(i);
			if (!itemstack.isEmpty()) {
				ItemStack itemstack1 = itemstack.getCraftingRemainder();
				if (!itemstack1.isEmpty()) {
					nonnulllist.set(i, itemstack1);
				} else if (!itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().isEmpty()) {
					nonnulllist.set(i, itemstack.copyWithCount(1));
				}
			}
		}

		return nonnulllist;
	}

	@Override
	public RecipeSerializer<BannerDuplicateRecipe> getSerializer() {
		return RecipeSerializer.BANNER_DUPLICATE;
	}
}
