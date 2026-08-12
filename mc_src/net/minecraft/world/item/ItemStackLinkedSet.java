package net.minecraft.world.item;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import java.util.Set;
import javax.annotation.Nullable;

public class ItemStackLinkedSet {
	public static final Strategy<? super ItemStack> TYPE_AND_TAG = new Strategy<ItemStack>() {
		public int hashCode(@Nullable ItemStack arg) {
			return ItemStack.hashItemAndComponents(arg);
		}

		public boolean equals(@Nullable ItemStack arg, @Nullable ItemStack arg2) {
			return arg == arg2 || arg != null && arg2 != null && arg.isEmpty() == arg2.isEmpty() && ItemStack.isSameItemSameComponents(arg, arg2);
		}
	};

	public static Set<ItemStack> createTypeAndComponentsSet() {
		return new ObjectLinkedOpenCustomHashSet<>(TYPE_AND_TAG);
	}
}
