package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public class ItemEnchantments implements TooltipProvider {
	public static final ItemEnchantments EMPTY = new ItemEnchantments(new Object2IntOpenHashMap<>(), true);
	private static final Codec<Integer> LEVEL_CODEC = Codec.intRange(1, 255);
	private static final Codec<Object2IntOpenHashMap<Holder<Enchantment>>> LEVELS_CODEC = Codec.unboundedMap(Enchantment.CODEC, LEVEL_CODEC)
		.xmap(Object2IntOpenHashMap::new, Function.identity());
	private static final Codec<ItemEnchantments> FULL_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				LEVELS_CODEC.fieldOf("levels").forGetter(arg -> arg.enchantments), Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(arg -> arg.showInTooltip)
			)
			.apply(instance, ItemEnchantments::new)
	);
	public static final Codec<ItemEnchantments> CODEC = Codec.withAlternative(
		FULL_CODEC, LEVELS_CODEC, object2IntOpenHashMap -> new ItemEnchantments(object2IntOpenHashMap, true)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemEnchantments> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.map(Object2IntOpenHashMap::new, Enchantment.STREAM_CODEC, ByteBufCodecs.VAR_INT),
		arg -> arg.enchantments,
		ByteBufCodecs.BOOL,
		arg -> arg.showInTooltip,
		ItemEnchantments::new
	);
	final Object2IntOpenHashMap<Holder<Enchantment>> enchantments;
	final boolean showInTooltip;

	ItemEnchantments(Object2IntOpenHashMap<Holder<Enchantment>> object2IntOpenHashMap, boolean bl) {
		this.enchantments = object2IntOpenHashMap;
		this.showInTooltip = bl;

		for (Entry<Holder<Enchantment>> entry : object2IntOpenHashMap.object2IntEntrySet()) {
			int i = entry.getIntValue();
			if (i < 0 || i > 255) {
				throw new IllegalArgumentException("Enchantment " + entry.getKey() + " has invalid level " + i);
			}
		}
	}

	public int getLevel(Holder<Enchantment> arg) {
		return this.enchantments.getInt(arg);
	}

	@Override
	public void addToTooltip(Item.TooltipContext arg, Consumer<Component> consumer, TooltipFlag arg2) {
		if (this.showInTooltip) {
			HolderLookup.Provider provider = arg.registries();
			HolderSet<Enchantment> holderSet = getTagOrEmpty(provider, Registries.ENCHANTMENT, EnchantmentTags.TOOLTIP_ORDER);

			for (Holder<Enchantment> holder : holderSet) {
				int i = this.enchantments.getInt(holder);
				if (i > 0) {
					consumer.accept(Enchantment.getFullname(holder, i));
				}
			}

			for (Entry<Holder<Enchantment>> entry : this.enchantments.object2IntEntrySet()) {
				Holder<Enchantment> holder2 = (Holder<Enchantment>)entry.getKey();
				if (!holderSet.contains(holder2)) {
					consumer.accept(Enchantment.getFullname((Holder<Enchantment>)entry.getKey(), entry.getIntValue()));
				}
			}
		}
	}

	private static <T> HolderSet<T> getTagOrEmpty(@Nullable HolderLookup.Provider arg, ResourceKey<Registry<T>> arg2, TagKey<T> arg3) {
		if (arg != null) {
			Optional<HolderSet.Named<T>> optional = arg.<T>lookupOrThrow(arg2).get(arg3);
			if (optional.isPresent()) {
				return (HolderSet<T>)optional.get();
			}
		}

		return HolderSet.direct();
	}

	public ItemEnchantments withTooltip(boolean bl) {
		return new ItemEnchantments(this.enchantments, bl);
	}

	public Set<Holder<Enchantment>> keySet() {
		return Collections.unmodifiableSet(this.enchantments.keySet());
	}

	public Set<Entry<Holder<Enchantment>>> entrySet() {
		return Collections.unmodifiableSet(this.enchantments.object2IntEntrySet());
	}

	public int size() {
		return this.enchantments.size();
	}

	public boolean isEmpty() {
		return this.enchantments.isEmpty();
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else {
			return !(object instanceof ItemEnchantments itemEnchantments)
				? false
				: this.showInTooltip == itemEnchantments.showInTooltip && this.enchantments.equals(itemEnchantments.enchantments);
		}
	}

	public int hashCode() {
		int i = this.enchantments.hashCode();
		return 31 * i + (this.showInTooltip ? 1 : 0);
	}

	public String toString() {
		return "ItemEnchantments{enchantments=" + this.enchantments + ", showInTooltip=" + this.showInTooltip + "}";
	}

	public static class Mutable {
		private final Object2IntOpenHashMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
		private final boolean showInTooltip;

		public Mutable(ItemEnchantments arg) {
			this.enchantments.putAll(arg.enchantments);
			this.showInTooltip = arg.showInTooltip;
		}

		public void set(Holder<Enchantment> arg, int i) {
			if (i <= 0) {
				this.enchantments.removeInt(arg);
			} else {
				this.enchantments.put(arg, Math.min(i, 255));
			}
		}

		public void upgrade(Holder<Enchantment> arg, int i) {
			if (i > 0) {
				this.enchantments.merge(arg, Math.min(i, 255), Integer::max);
			}
		}

		public void removeIf(Predicate<Holder<Enchantment>> predicate) {
			this.enchantments.keySet().removeIf(predicate);
		}

		public int getLevel(Holder<Enchantment> arg) {
			return this.enchantments.getOrDefault(arg, 0);
		}

		public Set<Holder<Enchantment>> keySet() {
			return this.enchantments.keySet();
		}

		public ItemEnchantments toImmutable() {
			return new ItemEnchantments(this.enchantments, this.showInTooltip);
		}
	}
}
