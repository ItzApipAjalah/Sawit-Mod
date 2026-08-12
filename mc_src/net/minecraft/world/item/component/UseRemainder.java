package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record UseRemainder(ItemStack convertInto) {
	public static final Codec<UseRemainder> CODEC = ItemStack.CODEC.xmap(UseRemainder::new, UseRemainder::convertInto);
	public static final StreamCodec<RegistryFriendlyByteBuf, UseRemainder> STREAM_CODEC = StreamCodec.composite(
		ItemStack.STREAM_CODEC, UseRemainder::convertInto, UseRemainder::new
	);

	public ItemStack convertIntoRemainder(ItemStack arg, int i, boolean bl, UseRemainder.OnExtraCreatedRemainder arg2) {
		if (bl) {
			return arg;
		} else if (arg.getCount() >= i) {
			return arg;
		} else {
			ItemStack itemStack = this.convertInto.copy();
			if (arg.isEmpty()) {
				return itemStack;
			} else {
				arg2.apply(itemStack);
				return arg;
			}
		}
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else if (object != null && this.getClass() == object.getClass()) {
			UseRemainder useRemainder = (UseRemainder)object;
			return ItemStack.matches(this.convertInto, useRemainder.convertInto);
		} else {
			return false;
		}
	}

	public int hashCode() {
		return ItemStack.hashItemAndComponents(this.convertInto);
	}

	@FunctionalInterface
	public interface OnExtraCreatedRemainder {
		void apply(ItemStack arg);
	}
}
