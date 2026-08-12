package net.minecraft.world.item.crafting;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class RecipeCache {
	private final RecipeCache.Entry[] entries;
	private WeakReference<RecipeManager> cachedRecipeManager = new WeakReference(null);

	public RecipeCache(int i) {
		this.entries = new RecipeCache.Entry[i];
	}

	public Optional<RecipeHolder<CraftingRecipe>> get(ServerLevel arg, CraftingInput arg2) {
		if (arg2.isEmpty()) {
			return Optional.empty();
		} else {
			this.validateRecipeManager(arg);

			for (int i = 0; i < this.entries.length; i++) {
				RecipeCache.Entry entry = this.entries[i];
				if (entry != null && entry.matches(arg2)) {
					this.moveEntryToFront(i);
					return Optional.ofNullable(entry.value());
				}
			}

			return this.compute(arg2, arg);
		}
	}

	private void validateRecipeManager(ServerLevel arg) {
		RecipeManager recipeManager = arg.recipeAccess();
		if (recipeManager != this.cachedRecipeManager.get()) {
			this.cachedRecipeManager = new WeakReference(recipeManager);
			Arrays.fill(this.entries, null);
		}
	}

	private Optional<RecipeHolder<CraftingRecipe>> compute(CraftingInput arg, ServerLevel arg2) {
		Optional<RecipeHolder<CraftingRecipe>> optional = arg2.recipeAccess().getRecipeFor(RecipeType.CRAFTING, arg, arg2);
		this.insert(arg, (RecipeHolder<CraftingRecipe>)optional.orElse(null));
		return optional;
	}

	private void moveEntryToFront(int i) {
		if (i > 0) {
			RecipeCache.Entry entry = this.entries[i];
			System.arraycopy(this.entries, 0, this.entries, 1, i);
			this.entries[0] = entry;
		}
	}

	private void insert(CraftingInput arg, @Nullable RecipeHolder<CraftingRecipe> arg2) {
		NonNullList<ItemStack> nonNullList = NonNullList.withSize(arg.size(), ItemStack.EMPTY);

		for (int i = 0; i < arg.size(); i++) {
			nonNullList.set(i, arg.getItem(i).copyWithCount(1));
		}

		System.arraycopy(this.entries, 0, this.entries, 1, this.entries.length - 1);
		this.entries[0] = new RecipeCache.Entry(nonNullList, arg.width(), arg.height(), arg2);
	}

	record Entry(NonNullList<ItemStack> key, int width, int height, @Nullable RecipeHolder<CraftingRecipe> value) {
		public boolean matches(CraftingInput arg) {
			if (this.width == arg.width() && this.height == arg.height()) {
				for (int i = 0; i < this.key.size(); i++) {
					if (!ItemStack.isSameItemSameComponents(this.key.get(i), arg.getItem(i))) {
						return false;
					}
				}

				return true;
			} else {
				return false;
			}
		}
	}
}
