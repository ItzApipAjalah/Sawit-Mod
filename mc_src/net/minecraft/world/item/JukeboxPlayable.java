package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public record JukeboxPlayable(EitherHolder<JukeboxSong> song, boolean showInTooltip) implements TooltipProvider {
	public static final Codec<JukeboxPlayable> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				EitherHolder.codec(Registries.JUKEBOX_SONG, JukeboxSong.CODEC).fieldOf("song").forGetter(JukeboxPlayable::song),
				Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(JukeboxPlayable::showInTooltip)
			)
			.apply(instance, JukeboxPlayable::new)
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, JukeboxPlayable> STREAM_CODEC = StreamCodec.composite(
		EitherHolder.streamCodec(Registries.JUKEBOX_SONG, JukeboxSong.STREAM_CODEC),
		JukeboxPlayable::song,
		ByteBufCodecs.BOOL,
		JukeboxPlayable::showInTooltip,
		JukeboxPlayable::new
	);

	@Override
	public void addToTooltip(Item.TooltipContext arg, Consumer<Component> consumer, TooltipFlag arg2) {
		HolderLookup.Provider provider = arg.registries();
		if (this.showInTooltip && provider != null) {
			this.song.unwrap(provider).ifPresent(argx -> {
				MutableComponent mutableComponent = ((JukeboxSong)argx.value()).description().copy();
				ComponentUtils.mergeStyles(mutableComponent, Style.EMPTY.withColor(ChatFormatting.GRAY));
				consumer.accept(mutableComponent);
			});
		}
	}

	public JukeboxPlayable withTooltip(boolean bl) {
		return new JukeboxPlayable(this.song, bl);
	}

	public static InteractionResult tryInsertIntoJukebox(Level arg, BlockPos arg2, ItemStack arg3, Player arg4) {
		JukeboxPlayable jukeboxPlayable = arg3.get(DataComponents.JUKEBOX_PLAYABLE);
		if (jukeboxPlayable == null) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		} else {
			BlockState blockState = arg.getBlockState(arg2);
			if (blockState.is(Blocks.JUKEBOX) && !(Boolean)blockState.getValue(JukeboxBlock.HAS_RECORD)) {
				if (!arg.isClientSide) {
					ItemStack itemStack = arg3.consumeAndReturn(1, arg4);
					if (arg.getBlockEntity(arg2) instanceof JukeboxBlockEntity jukeboxBlockEntity) {
						jukeboxBlockEntity.setTheItem(itemStack);
						arg.gameEvent(GameEvent.BLOCK_CHANGE, arg2, GameEvent.Context.of(arg4, blockState));
					}

					arg4.awardStat(Stats.PLAY_RECORD);
				}

				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
		}
	}
}
