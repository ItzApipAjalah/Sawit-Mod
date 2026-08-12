package net.minecraft.core;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.IRegistryExtension;

public interface Registry<T> extends Keyable, HolderLookup.RegistryLookup<T>, IdMap<T>, IRegistryExtension<T> {
	@Override
	ResourceKey<? extends Registry<T>> key();

	default Codec<T> byNameCodec() {
		return this.referenceHolderWithLifecycle().flatComapMap(Holder.Reference::value, object -> this.safeCastToReference(this.wrapAsHolder((T)object)));
	}

	default Codec<Holder<T>> holderByNameCodec() {
		return this.referenceHolderWithLifecycle().flatComapMap(arg -> arg, this::safeCastToReference);
	}

	private Codec<Holder.Reference<T>> referenceHolderWithLifecycle() {
		Codec<Holder.Reference<T>> codec = ResourceLocation.CODEC
			.comapFlatMap(
				arg -> (DataResult)this.get(arg).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.key() + ": " + arg)),
				arg -> arg.key().location()
			);
		return ExtraCodecs.overrideLifecycle(
			codec, arg -> (Lifecycle)this.registrationInfo(arg.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental())
		);
	}

	private DataResult<Holder.Reference<T>> safeCastToReference(Holder<T> arg) {
		return arg.getDelegate() instanceof Holder.Reference reference
			? DataResult.success(reference)
			: DataResult.error(() -> "Unregistered holder in " + this.key() + ": " + arg);
	}

	@Override
	default <U> Stream<U> keys(DynamicOps<U> dynamicOps) {
		return this.keySet().stream().map(arg -> dynamicOps.createString(arg.toString()));
	}

	@Nullable
	ResourceLocation getKey(T object);

	Optional<ResourceKey<T>> getResourceKey(T object);

	@Override
	int getId(@Nullable T object);

	@Nullable
	T getValue(@Nullable ResourceKey<T> arg);

	@Nullable
	T getValue(@Nullable ResourceLocation arg);

	Optional<RegistrationInfo> registrationInfo(ResourceKey<T> arg);

	default Optional<T> getOptional(@Nullable ResourceLocation arg) {
		return Optional.ofNullable(this.getValue(arg));
	}

	default Optional<T> getOptional(@Nullable ResourceKey<T> arg) {
		return Optional.ofNullable(this.getValue(arg));
	}

	Optional<Holder.Reference<T>> getAny();

	default T getValueOrThrow(ResourceKey<T> arg) {
		T t = this.getValue(arg);
		if (t == null) {
			throw new IllegalStateException("Missing key in " + this.key() + ": " + arg);
		} else {
			return t;
		}
	}

	Set<ResourceLocation> keySet();

	Set<Entry<ResourceKey<T>, T>> entrySet();

	Set<ResourceKey<T>> registryKeySet();

	Optional<Holder.Reference<T>> getRandom(RandomSource arg);

	default Stream<T> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	boolean containsKey(ResourceLocation arg);

	boolean containsKey(ResourceKey<T> arg);

	static <T> T register(Registry<? super T> arg, String string, T object) {
		return register(arg, ResourceLocation.parse(string), (T)object);
	}

	static <V, T extends V> T register(Registry<V> arg, ResourceLocation arg2, T object) {
		return register(arg, ResourceKey.create(arg.key(), arg2), (T)object);
	}

	static <V, T extends V> T register(Registry<V> arg, ResourceKey<V> arg2, T object) {
		((WritableRegistry)arg).register(arg2, object, RegistrationInfo.BUILT_IN);
		return (T)object;
	}

	static <T> Holder.Reference<T> registerForHolder(Registry<T> arg, ResourceKey<T> arg2, T object) {
		return (Holder.Reference<T>)((WritableRegistry)arg).register(arg2, object, RegistrationInfo.BUILT_IN);
	}

	static <T> Holder.Reference<T> registerForHolder(Registry<T> arg, ResourceLocation arg2, T object) {
		return registerForHolder(arg, ResourceKey.create(arg.key(), arg2), (T)object);
	}

	Registry<T> freeze();

	Holder.Reference<T> createIntrusiveHolder(T object);

	Optional<Holder.Reference<T>> get(int i);

	Optional<Holder.Reference<T>> get(ResourceLocation arg);

	Holder<T> wrapAsHolder(T object);

	default Iterable<Holder<T>> getTagOrEmpty(TagKey<T> arg) {
		return DataFixUtils.orElse(this.get(arg), List.of());
	}

	default Optional<Holder<T>> getRandomElementOf(TagKey<T> arg, RandomSource arg2) {
		return this.get(arg).flatMap(arg2x -> arg2x.getRandomElement(arg2));
	}

	Stream<HolderSet.Named<T>> getTags();

	default IdMap<Holder<T>> asHolderIdMap() {
		return new IdMap<Holder<T>>() {
			public int getId(Holder<T> arg) {
				return Registry.this.getId(arg.value());
			}

			@Nullable
			public Holder<T> byId(int i) {
				return (Holder<T>)Registry.this.get(i).orElse(null);
			}

			@Override
			public int size() {
				return Registry.this.size();
			}

			public Iterator<Holder<T>> iterator() {
				return Registry.this.listElements().map(arg -> arg).iterator();
			}
		};
	}

	Registry.PendingTags<T> prepareTagReload(TagLoader.LoadResult<T> arg);

	public interface PendingTags<T> {
		ResourceKey<? extends Registry<? extends T>> key();

		HolderLookup.RegistryLookup<T> lookup();

		void apply();

		int size();
	}
}
