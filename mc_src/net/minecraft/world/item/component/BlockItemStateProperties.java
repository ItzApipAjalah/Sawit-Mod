package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public record BlockItemStateProperties(Map<String, String> properties) {
	public static final BlockItemStateProperties EMPTY = new BlockItemStateProperties(Map.of());
	public static final Codec<BlockItemStateProperties> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
		.xmap(BlockItemStateProperties::new, BlockItemStateProperties::properties);
	private static final StreamCodec<ByteBuf, Map<String, String>> PROPERTIES_STREAM_CODEC = ByteBufCodecs.map(
		Object2ObjectOpenHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8
	);
	public static final StreamCodec<ByteBuf, BlockItemStateProperties> STREAM_CODEC = PROPERTIES_STREAM_CODEC.map(
		BlockItemStateProperties::new, BlockItemStateProperties::properties
	);

	public <T extends Comparable<T>> BlockItemStateProperties with(Property<T> arg, T comparable) {
		return new BlockItemStateProperties(Util.copyAndPut(this.properties, arg.getName(), arg.getName((T)comparable)));
	}

	public <T extends Comparable<T>> BlockItemStateProperties with(Property<T> arg, BlockState arg2) {
		return this.with(arg, arg2.getValue(arg));
	}

	@Nullable
	public <T extends Comparable<T>> T get(Property<T> arg) {
		String string = (String)this.properties.get(arg.getName());
		return (T)(string == null ? null : arg.getValue(string).orElse(null));
	}

	public BlockState apply(BlockState arg) {
		StateDefinition<Block, BlockState> stateDefinition = arg.getBlock().getStateDefinition();

		for (Entry<String, String> entry : this.properties.entrySet()) {
			Property<?> property = stateDefinition.getProperty((String)entry.getKey());
			if (property != null) {
				arg = updateState(arg, property, (String)entry.getValue());
			}
		}

		return arg;
	}

	private static <T extends Comparable<T>> BlockState updateState(BlockState arg, Property<T> arg2, String string) {
		return (BlockState)arg2.getValue(string).map(comparable -> arg.setValue(arg2, comparable)).orElse(arg);
	}

	public boolean isEmpty() {
		return this.properties.isEmpty();
	}
}
