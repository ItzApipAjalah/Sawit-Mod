package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record ItemCost(Holder<Item> item, int count, DataComponentPredicate components, ItemStack itemStack) {
	public static final Codec<ItemCost> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				Item.CODEC.fieldOf("id").forGetter(ItemCost::item),
				ExtraCodecs.POSITIVE_INT.fieldOf("count").orElse(1).forGetter(ItemCost::count),
				DataComponentPredicate.CODEC.optionalFieldOf("components", DataComponentPredicate.EMPTY).forGetter(ItemCost::components)
			)
			.apply(instance, ItemCost::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemCost> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.holderRegistry(Registries.ITEM),
		ItemCost::item,
		ByteBufCodecs.VAR_INT,
		ItemCost::count,
		DataComponentPredicate.STREAM_CODEC,
		ItemCost::components,
		ItemCost::new
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, Optional<ItemCost>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);

	public ItemCost(ItemLike arg) {
		this(arg, 1);
	}

	public ItemCost(ItemLike arg, int i) {
		this(arg.asItem().builtInRegistryHolder(), i, DataComponentPredicate.EMPTY);
	}

	public ItemCost(Holder<Item> arg, int i, DataComponentPredicate arg2) {
		this(arg, i, arg2, createStack(arg, i, arg2));
	}

	public ItemCost withComponents(UnaryOperator<DataComponentPredicate.Builder> unaryOperator) {
		return new ItemCost(this.item, this.count, ((DataComponentPredicate.Builder)unaryOperator.apply(DataComponentPredicate.builder())).build());
	}

	private static ItemStack createStack(Holder<Item> arg, int i, DataComponentPredicate arg2) {
		return new ItemStack(arg, i, arg2.asPatch());
	}

	public boolean test(ItemStack arg) {
		return arg.is(this.item) && this.components.test((DataComponentHolder)arg);
	}
}
