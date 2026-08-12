package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientCodecs;

public final class Ingredient implements Predicate<ItemStack> {
	public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = IngredientCodecs.streamCodec(
		ByteBufCodecs.holderSet(Registries.ITEM).map(Ingredient::new, arg -> arg.getValuesForSync())
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> OPTIONAL_CONTENTS_STREAM_CODEC = IngredientCodecs.optionalStreamCodec(
		ByteBufCodecs.holderSet(Registries.ITEM)
			.map(
				arg -> arg.size() == 0 ? Optional.empty() : Optional.of(new Ingredient(arg)),
				optional -> (HolderSet)optional.map(arg -> arg.getValuesForSync()).orElse(HolderSet.direct())
			)
	);
	public static final Codec<HolderSet<Item>> NON_AIR_HOLDER_SET_CODEC = HolderSetCodec.create(Registries.ITEM, Item.CODEC, false);
	public static final Codec<Ingredient> CODEC = IngredientCodecs.codec(
		ExtraCodecs.nonEmptyHolderSet(NON_AIR_HOLDER_SET_CODEC).xmap(Ingredient::new, arg -> arg.values)
	);
	private final HolderSet<Item> values;
	@Nullable
	private List<Holder<Item>> items;
	@Nullable
	private ICustomIngredient customIngredient = null;

	private Ingredient(HolderSet<Item> arg) {
		arg.unwrap().ifRight(list -> {
			if (list.isEmpty()) {
				throw new UnsupportedOperationException("Ingredients can't be empty");
			} else if (list.contains(Items.AIR.builtInRegistryHolder())) {
				throw new UnsupportedOperationException("Ingredient can't contain air");
			}
		});
		this.values = arg;
	}

	public Ingredient(ICustomIngredient customIngredient) {
		this.values = HolderSet.empty();
		this.customIngredient = customIngredient;
	}

	public static boolean testOptionalIngredient(Optional<Ingredient> optional, ItemStack arg) {
		return (Boolean)optional.map(arg2 -> arg2.test(arg)).orElseGet(arg::isEmpty);
	}

	public List<Holder<Item>> items() {
		if (this.items == null) {
			if (this.customIngredient != null) {
				this.items = this.customIngredient.items().toList();
			} else {
				this.items = ImmutableList.copyOf(this.values);
			}
		}

		return this.items;
	}

	public boolean test(ItemStack arg) {
		if (this.customIngredient != null) {
			return this.customIngredient.test(arg);
		} else {
			List<Holder<Item>> list = this.items();

			for (int i = 0; i < list.size(); i++) {
				if (arg.is((Holder<Item>)list.get(i))) {
					return true;
				}
			}

			return false;
		}
	}

	public boolean equals(Object object) {
		return object instanceof Ingredient ingredient
			? Objects.equals(this.customIngredient, ingredient.customIngredient) && Objects.equals(this.values, ingredient.values)
			: false;
	}

	public int hashCode() {
		return this.customIngredient != null ? this.customIngredient.hashCode() : this.values.hashCode();
	}

	public HolderSet<Item> getValues() {
		if (this.isCustom()) {
			throw new IllegalStateException("Cannot retrieve values from custom ingredient!");
		} else {
			return this.values;
		}
	}

	private HolderSet<Item> getValuesForSync() {
		return (HolderSet<Item>)(this.isCustom() ? HolderSet.direct(this.items()) : this.values);
	}

	public boolean isSimple() {
		return this.customIngredient == null || this.customIngredient.isSimple();
	}

	@Nullable
	public ICustomIngredient getCustomIngredient() {
		return this.customIngredient;
	}

	public boolean isCustom() {
		return this.customIngredient != null;
	}

	public static Ingredient of(ItemLike arg) {
		return new Ingredient(HolderSet.direct(arg.asItem().builtInRegistryHolder()));
	}

	public static Ingredient of(ItemLike... args) {
		return of(Arrays.stream(args));
	}

	public static Ingredient of(Stream<? extends ItemLike> stream) {
		return new Ingredient(HolderSet.direct(stream.map(arg -> arg.asItem().builtInRegistryHolder()).toList()));
	}

	public static Ingredient of(HolderSet<Item> arg) {
		return new Ingredient(arg);
	}

	public SlotDisplay display() {
		return this.customIngredient != null
			? this.customIngredient.display()
			: this.values.unwrap().map(SlotDisplay.TagSlotDisplay::new, list -> new SlotDisplay.Composite(list.stream().map(Ingredient::displayForSingleItem).toList()));
	}

	public static SlotDisplay optionalIngredientToDisplay(Optional<Ingredient> optional) {
		return (SlotDisplay)optional.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE);
	}

	public static SlotDisplay displayForSingleItem(Holder<Item> arg) {
		SlotDisplay slotdisplay = new SlotDisplay.ItemSlotDisplay(arg);
		ItemStack itemstack = ((Item)arg.value()).getCraftingRemainder();
		if (!itemstack.isEmpty()) {
			SlotDisplay slotdisplay1 = new SlotDisplay.ItemStackSlotDisplay(itemstack);
			return new SlotDisplay.WithRemainder(slotdisplay, slotdisplay1);
		} else {
			return slotdisplay;
		}
	}
}
