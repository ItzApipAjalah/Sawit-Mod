package net.minecraft.world.item.component;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

public final class CustomData {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final CustomData EMPTY = new CustomData(new CompoundTag());
	public static final Codec<CustomData> CODEC = Codec.withAlternative(CompoundTag.CODEC, TagParser.AS_CODEC).xmap(CustomData::new, arg -> arg.tag);
	public static final Codec<CustomData> CODEC_WITH_ID = CODEC.validate(
		arg -> arg.getUnsafe().contains("id", 8) ? DataResult.success(arg) : DataResult.error(() -> "Missing id for entity in: " + arg)
	);
	@Deprecated
	public static final StreamCodec<ByteBuf, CustomData> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(CustomData::new, arg -> arg.tag);
	private final CompoundTag tag;

	private CustomData(CompoundTag arg) {
		this.tag = arg;
	}

	public static CustomData of(CompoundTag arg) {
		return new CustomData(arg.copy());
	}

	public static Predicate<ItemStack> itemMatcher(DataComponentType<CustomData> arg, CompoundTag arg2) {
		return arg3 -> {
			CustomData customData = arg3.getOrDefault(arg, EMPTY);
			return customData.matchedBy(arg2);
		};
	}

	public boolean matchedBy(CompoundTag arg) {
		return NbtUtils.compareNbt(arg, this.tag, true);
	}

	public static void update(DataComponentType<CustomData> arg, ItemStack arg2, Consumer<CompoundTag> consumer) {
		CustomData customData = arg2.getOrDefault(arg, EMPTY).update(consumer);
		if (customData.tag.isEmpty()) {
			arg2.remove(arg);
		} else {
			arg2.set(arg, customData);
		}
	}

	public static void set(DataComponentType<CustomData> arg, ItemStack arg2, CompoundTag arg3) {
		if (!arg3.isEmpty()) {
			arg2.set(arg, of(arg3));
		} else {
			arg2.remove(arg);
		}
	}

	public CustomData update(Consumer<CompoundTag> consumer) {
		CompoundTag compoundTag = this.tag.copy();
		consumer.accept(compoundTag);
		return new CustomData(compoundTag);
	}

	public void loadInto(Entity arg) {
		CompoundTag compoundTag = arg.saveWithoutId(new CompoundTag());
		UUID uUID = arg.getUUID();
		compoundTag.merge(this.tag);
		arg.load(compoundTag);
		arg.setUUID(uUID);
	}

	public boolean loadInto(BlockEntity arg, HolderLookup.Provider arg2) {
		CompoundTag compoundTag = arg.saveCustomOnly(arg2);
		CompoundTag compoundTag2 = compoundTag.copy();
		compoundTag.merge(this.tag);
		if (!compoundTag.equals(compoundTag2)) {
			try {
				arg.loadCustomOnly(compoundTag, arg2);
				arg.setChanged();
				return true;
			} catch (Exception var8) {
				LOGGER.warn("Failed to apply custom data to block entity at {}", arg.getBlockPos(), var8);

				try {
					arg.loadCustomOnly(compoundTag2, arg2);
				} catch (Exception var7) {
					LOGGER.warn("Failed to rollback block entity at {} after failure", arg.getBlockPos(), var7);
				}
			}
		}

		return false;
	}

	public <T> DataResult<CustomData> update(DynamicOps<Tag> dynamicOps, MapEncoder<T> mapEncoder, T object) {
		return mapEncoder.<CompoundTag>encode(object, dynamicOps, dynamicOps.mapBuilder()).build(this.tag).map(arg -> new CustomData((CompoundTag)arg));
	}

	public <T> DataResult<T> read(MapDecoder<T> mapDecoder) {
		return this.read(NbtOps.INSTANCE, mapDecoder);
	}

	public <T> DataResult<T> read(DynamicOps<Tag> dynamicOps, MapDecoder<T> mapDecoder) {
		MapLike<Tag> mapLike = dynamicOps.getMap(this.tag).getOrThrow();
		return mapDecoder.decode(dynamicOps, mapLike);
	}

	public int size() {
		return this.tag.size();
	}

	public boolean isEmpty() {
		return this.tag.isEmpty();
	}

	public CompoundTag copyTag() {
		return this.tag.copy();
	}

	public boolean contains(String string) {
		return this.tag.contains(string);
	}

	public boolean equals(Object object) {
		if (object == this) {
			return true;
		} else {
			return object instanceof CustomData customData ? this.tag.equals(customData.tag) : false;
		}
	}

	public int hashCode() {
		return this.tag.hashCode();
	}

	public String toString() {
		return this.tag.toString();
	}

	@Deprecated
	public CompoundTag getUnsafe() {
		return this.tag;
	}
}
