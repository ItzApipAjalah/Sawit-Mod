package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

public class AdventureModePredicate {
	private static final Codec<AdventureModePredicate> SIMPLE_CODEC = BlockPredicate.CODEC
		.flatComapMap(arg -> new AdventureModePredicate(List.of(arg), true), arg -> DataResult.error(() -> "Cannot encode"));
	private static final Codec<AdventureModePredicate> FULL_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				ExtraCodecs.nonEmptyList(BlockPredicate.CODEC.listOf()).fieldOf("predicates").forGetter(arg -> arg.predicates),
				Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(AdventureModePredicate::showInTooltip)
			)
			.apply(instance, AdventureModePredicate::new)
	);
	public static final Codec<AdventureModePredicate> CODEC = Codec.withAlternative(FULL_CODEC, SIMPLE_CODEC);
	public static final StreamCodec<RegistryFriendlyByteBuf, AdventureModePredicate> STREAM_CODEC = StreamCodec.composite(
		BlockPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
		arg -> arg.predicates,
		ByteBufCodecs.BOOL,
		AdventureModePredicate::showInTooltip,
		AdventureModePredicate::new
	);
	public static final Component CAN_BREAK_HEADER = Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY);
	public static final Component CAN_PLACE_HEADER = Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY);
	private static final Component UNKNOWN_USE = Component.translatable("item.canUse.unknown").withStyle(ChatFormatting.GRAY);
	private final List<BlockPredicate> predicates;
	private final boolean showInTooltip;
	@Nullable
	private List<Component> cachedTooltip;
	@Nullable
	private BlockInWorld lastCheckedBlock;
	private boolean lastResult;
	private boolean checksBlockEntity;

	public AdventureModePredicate(List<BlockPredicate> list, boolean bl) {
		this.predicates = list;
		this.showInTooltip = bl;
	}

	private static boolean areSameBlocks(BlockInWorld arg, @Nullable BlockInWorld arg2, boolean bl) {
		if (arg2 == null || arg.getState() != arg2.getState()) {
			return false;
		} else if (!bl) {
			return true;
		} else if (arg.getEntity() == null && arg2.getEntity() == null) {
			return true;
		} else if (arg.getEntity() != null && arg2.getEntity() != null) {
			RegistryAccess registryAccess = arg.getLevel().registryAccess();
			return Objects.equals(arg.getEntity().saveWithId(registryAccess), arg2.getEntity().saveWithId(registryAccess));
		} else {
			return false;
		}
	}

	public boolean test(BlockInWorld arg) {
		if (areSameBlocks(arg, this.lastCheckedBlock, this.checksBlockEntity)) {
			return this.lastResult;
		} else {
			this.lastCheckedBlock = arg;
			this.checksBlockEntity = false;

			for (BlockPredicate blockPredicate : this.predicates) {
				if (blockPredicate.matches(arg)) {
					this.checksBlockEntity = this.checksBlockEntity | blockPredicate.requiresNbt();
					this.lastResult = true;
					return true;
				}
			}

			this.lastResult = false;
			return false;
		}
	}

	private List<Component> tooltip() {
		if (this.cachedTooltip == null) {
			this.cachedTooltip = computeTooltip(this.predicates);
		}

		return this.cachedTooltip;
	}

	public void addToTooltip(Consumer<Component> consumer) {
		this.tooltip().forEach(consumer);
	}

	public AdventureModePredicate withTooltip(boolean bl) {
		return new AdventureModePredicate(this.predicates, bl);
	}

	private static List<Component> computeTooltip(List<BlockPredicate> list) {
		for (BlockPredicate blockPredicate : list) {
			if (blockPredicate.blocks().isEmpty()) {
				return List.of(UNKNOWN_USE);
			}
		}

		return list.stream()
			.flatMap(arg -> ((HolderSet)arg.blocks().orElseThrow()).stream())
			.distinct()
			.map(arg -> ((Block)arg.value()).getName().withStyle(ChatFormatting.DARK_GRAY))
			.toList();
	}

	public boolean showInTooltip() {
		return this.showInTooltip;
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else {
			return !(object instanceof AdventureModePredicate adventureModePredicate)
				? false
				: this.predicates.equals(adventureModePredicate.predicates) && this.showInTooltip == adventureModePredicate.showInTooltip;
		}
	}

	public int hashCode() {
		return this.predicates.hashCode() * 31 + (this.showInTooltip ? 1 : 0);
	}

	public String toString() {
		return "AdventureModePredicate{predicates=" + this.predicates + ", showInTooltip=" + this.showInTooltip + "}";
	}
}
