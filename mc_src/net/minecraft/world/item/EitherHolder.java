package net.minecraft.world.item;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record EitherHolder<T>(Optional<Holder<T>> holder, ResourceKey<T> key) {
	public EitherHolder(Holder<T> arg) {
		this(Optional.of(arg), (ResourceKey<T>)arg.unwrapKey().orElseThrow());
	}

	public EitherHolder(ResourceKey<T> arg) {
		this(Optional.empty(), arg);
	}

	public static <T> Codec<EitherHolder<T>> codec(ResourceKey<Registry<T>> arg, Codec<Holder<T>> codec) {
		return Codec.either(codec, ResourceKey.codec(arg).comapFlatMap(argx -> DataResult.error(() -> "Cannot parse as key without registry"), Function.identity()))
			.xmap(EitherHolder::fromEither, EitherHolder::asEither);
	}

	public static <T> StreamCodec<RegistryFriendlyByteBuf, EitherHolder<T>> streamCodec(
		ResourceKey<Registry<T>> arg, StreamCodec<RegistryFriendlyByteBuf, Holder<T>> arg2
	) {
		return StreamCodec.composite(ByteBufCodecs.either(arg2, ResourceKey.streamCodec(arg)), EitherHolder::asEither, EitherHolder::fromEither);
	}

	public Either<Holder<T>, ResourceKey<T>> asEither() {
		return (Either<Holder<T>, ResourceKey<T>>)this.holder.map(Either::left).orElseGet(() -> Either.right(this.key));
	}

	public static <T> EitherHolder<T> fromEither(Either<Holder<T>, ResourceKey<T>> either) {
		return either.map(EitherHolder::new, EitherHolder::new);
	}

	public Optional<T> unwrap(Registry<T> arg) {
		return this.holder.map(Holder::value).or(() -> arg.getOptional(this.key));
	}

	public Optional<Holder<T>> unwrap(HolderLookup.Provider arg) {
		return this.holder.or(() -> arg.lookupOrThrow(this.key.registryKey()).get(this.key));
	}
}
