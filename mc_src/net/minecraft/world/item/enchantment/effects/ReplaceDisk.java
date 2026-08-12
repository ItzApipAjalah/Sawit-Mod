package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.Vec3;

public record ReplaceDisk(
	LevelBasedValue radius,
	LevelBasedValue height,
	Vec3i offset,
	Optional<BlockPredicate> predicate,
	BlockStateProvider blockState,
	Optional<Holder<GameEvent>> triggerGameEvent
) implements EnchantmentEntityEffect {
	public static final MapCodec<ReplaceDisk> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
				LevelBasedValue.CODEC.fieldOf("radius").forGetter(ReplaceDisk::radius),
				LevelBasedValue.CODEC.fieldOf("height").forGetter(ReplaceDisk::height),
				Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(ReplaceDisk::offset),
				BlockPredicate.CODEC.optionalFieldOf("predicate").forGetter(ReplaceDisk::predicate),
				BlockStateProvider.CODEC.fieldOf("block_state").forGetter(ReplaceDisk::blockState),
				GameEvent.CODEC.optionalFieldOf("trigger_game_event").forGetter(ReplaceDisk::triggerGameEvent)
			)
			.apply(instance, ReplaceDisk::new)
	);

	@Override
	public void apply(ServerLevel arg, int i, EnchantedItemInUse arg2, Entity arg3, Vec3 arg4) {
		BlockPos blockPos = BlockPos.containing(arg4).offset(this.offset);
		RandomSource randomSource = arg3.getRandom();
		int j = (int)this.radius.calculate(i);
		int k = (int)this.height.calculate(i);

		for (BlockPos blockPos2 : BlockPos.betweenClosed(blockPos.offset(-j, 0, -j), blockPos.offset(j, Math.min(k - 1, 0), j))) {
			if (blockPos2.distToCenterSqr(arg4.x(), blockPos2.getY() + 0.5, arg4.z()) < Mth.square(j)
				&& (Boolean)this.predicate.map(arg3x -> arg3x.test(arg, blockPos2)).orElse(true)
				&& arg.setBlockAndUpdate(blockPos2, this.blockState.getState(randomSource, blockPos2))) {
				this.triggerGameEvent.ifPresent(arg4x -> arg.gameEvent(arg3, arg4x, blockPos2));
			}
		}
	}

	@Override
	public MapCodec<ReplaceDisk> codec() {
		return CODEC;
	}
}
