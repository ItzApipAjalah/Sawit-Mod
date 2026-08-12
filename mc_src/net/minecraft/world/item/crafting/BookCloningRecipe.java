package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public class BookCloningRecipe extends CustomRecipe {
	public BookCloningRecipe(CraftingBookCategory arg) {
		super(arg);
	}

	public boolean matches(CraftingInput arg, Level arg2) {
		if (arg.ingredientCount() < 2) {
			return false;
		} else {
			boolean flag = false;
			boolean flag1 = false;

			for (int i = 0; i < arg.size(); i++) {
				ItemStack itemstack = arg.getItem(i);
				if (!itemstack.isEmpty()) {
					if (itemstack.is(Items.WRITTEN_BOOK)) {
						if (flag1) {
							return false;
						}

						flag1 = true;
					} else {
						if (!itemstack.is(Items.WRITABLE_BOOK)) {
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
		int i = 0;
		ItemStack itemstack = ItemStack.EMPTY;

		for (int j = 0; j < arg.size(); j++) {
			ItemStack itemstack1 = arg.getItem(j);
			if (!itemstack1.isEmpty()) {
				if (itemstack1.is(Items.WRITTEN_BOOK)) {
					if (!itemstack.isEmpty()) {
						return ItemStack.EMPTY;
					}

					itemstack = itemstack1;
				} else {
					if (!itemstack1.is(Items.WRITABLE_BOOK)) {
						return ItemStack.EMPTY;
					}

					i++;
				}
			}
		}

		WrittenBookContent writtenbookcontent = itemstack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		if (!itemstack.isEmpty() && i >= 1 && writtenbookcontent != null) {
			WrittenBookContent writtenbookcontent1 = writtenbookcontent.tryCraftCopy();
			if (writtenbookcontent1 == null) {
				return ItemStack.EMPTY;
			} else {
				ItemStack itemstack2 = itemstack.copyWithCount(i);
				itemstack2.set(DataComponents.WRITTEN_BOOK_CONTENT, writtenbookcontent1);
				return itemstack2;
			}
		} else {
			return ItemStack.EMPTY;
		}
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput arg) {
		NonNullList<ItemStack> nonnulllist = NonNullList.withSize(arg.size(), ItemStack.EMPTY);

		for (int i = 0; i < nonnulllist.size(); i++) {
			ItemStack itemstack = arg.getItem(i);
			ItemStack itemstack1 = itemstack.getCraftingRemainder();
			if (!itemstack1.isEmpty()) {
				nonnulllist.set(i, itemstack1);
			} else if (itemstack.getItem() instanceof WrittenBookItem) {
				nonnulllist.set(i, itemstack.copyWithCount(1));
				break;
			}
		}

		return nonnulllist;
	}

	@Override
	public RecipeSerializer<BookCloningRecipe> getSerializer() {
		return RecipeSerializer.BOOK_CLONING;
	}
}
