package net.minecraft.core;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface HolderGetter<T> {
	Optional<Holder.Reference<T>> get(ResourceKey<T> arg);

	default Holder.Reference<T> getOrThrow(ResourceKey<T> arg) {
		return (Holder.Reference<T>)this.get(arg).orElseThrow(() -> new IllegalStateException("Missing element " + arg));
	}

	Optional<HolderSet.Named<T>> get(TagKey<T> arg);

	default HolderSet.Named<T> getOrThrow(TagKey<T> arg) {
		return (HolderSet.Named<T>)this.get(arg).orElseThrow(() -> new IllegalStateException("Missing tag " + arg));
	}

	public interface Provider {
		<T> Optional<? extends HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> arg);

		default <T> HolderGetter<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> arg) {
			return (HolderGetter<T>)this.lookup(arg).orElseThrow(() -> new IllegalStateException("Registry " + arg.location() + " not found"));
		}

		default <T> Optional<Holder.Reference<T>> get(ResourceKey<T> arg) {
			return this.lookup(arg.registryKey()).flatMap(arg2 -> arg2.get(arg));
		}
	}
}
