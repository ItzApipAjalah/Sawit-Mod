package net.minecraft.world.level;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.extensions.ILevelReaderExtension;

public interface LevelReader extends BlockAndTintGetter, CollisionGetter, SignalGetter, BiomeManager.NoiseBiomeSource, ILevelReaderExtension {
	@Nullable
	ChunkAccess getChunk(int i, int j, ChunkStatus arg, boolean bl);

	@Deprecated
	boolean hasChunk(int i, int j);

	int getHeight(Heightmap.Types arg, int i, int j);

	int getSkyDarken();

	BiomeManager getBiomeManager();

	default Holder<Biome> getBiome(BlockPos arg) {
		return this.getBiomeManager().getBiome(arg);
	}

	default Stream<BlockState> getBlockStatesIfLoaded(AABB arg) {
		int i = Mth.floor(arg.minX);
		int j = Mth.floor(arg.maxX);
		int k = Mth.floor(arg.minY);
		int l = Mth.floor(arg.maxY);
		int i1 = Mth.floor(arg.minZ);
		int j1 = Mth.floor(arg.maxZ);
		return this.hasChunksAt(i, k, i1, j, l, j1) ? this.getBlockStates(arg) : Stream.empty();
	}

	@Override
	default int getBlockTint(BlockPos arg, ColorResolver arg2) {
		return arg2.getColor(this.getBiome(arg).value(), arg.getX(), arg.getZ());
	}

	@Override
	default Holder<Biome> getNoiseBiome(int i, int j, int k) {
		ChunkAccess chunkaccess = this.getChunk(QuartPos.toSection(i), QuartPos.toSection(k), ChunkStatus.BIOMES, false);
		return chunkaccess != null ? chunkaccess.getNoiseBiome(i, j, k) : this.getUncachedNoiseBiome(i, j, k);
	}

	Holder<Biome> getUncachedNoiseBiome(int i, int j, int k);

	boolean isClientSide();

	int getSeaLevel();

	DimensionType dimensionType();

	@Override
	default int getMinY() {
		return this.dimensionType().minY();
	}

	@Override
	default int getHeight() {
		return this.dimensionType().height();
	}

	default BlockPos getHeightmapPos(Heightmap.Types arg, BlockPos arg2) {
		return new BlockPos(arg2.getX(), this.getHeight(arg, arg2.getX(), arg2.getZ()), arg2.getZ());
	}

	default boolean isEmptyBlock(BlockPos arg) {
		return this.getBlockState(arg).isAir();
	}

	default boolean canSeeSkyFromBelowWater(BlockPos arg) {
		if (arg.getY() >= this.getSeaLevel()) {
			return this.canSeeSky(arg);
		} else {
			BlockPos blockpos = new BlockPos(arg.getX(), this.getSeaLevel(), arg.getZ());
			if (!this.canSeeSky(blockpos)) {
				return false;
			} else {
				for (BlockPos blockpos1 = blockpos.below(); blockpos1.getY() > arg.getY(); blockpos1 = blockpos1.below()) {
					BlockState blockstate = this.getBlockState(blockpos1);
					if (blockstate.getLightBlock() > 0 && !blockstate.liquid()) {
						return false;
					}
				}

				return true;
			}
		}
	}

	default float getPathfindingCostFromLightLevels(BlockPos arg) {
		return this.getLightLevelDependentMagicValue(arg) - 0.5F;
	}

	@Deprecated
	default float getLightLevelDependentMagicValue(BlockPos arg) {
		float f = this.getMaxLocalRawBrightness(arg) / 15.0F;
		float f1 = f / (4.0F - 3.0F * f);
		return Mth.lerp(this.dimensionType().ambientLight(), f1, 1.0F);
	}

	default ChunkAccess getChunk(BlockPos arg) {
		return this.getChunk(SectionPos.blockToSectionCoord(arg.getX()), SectionPos.blockToSectionCoord(arg.getZ()));
	}

	default ChunkAccess getChunk(int i, int j) {
		return this.getChunk(i, j, ChunkStatus.FULL, true);
	}

	default ChunkAccess getChunk(int i, int j, ChunkStatus arg) {
		return this.getChunk(i, j, arg, true);
	}

	@Nullable
	@Override
	default BlockGetter getChunkForCollisions(int i, int j) {
		return this.getChunk(i, j, ChunkStatus.EMPTY, false);
	}

	default boolean isWaterAt(BlockPos arg) {
		return this.getFluidState(arg).is(FluidTags.WATER);
	}

	default boolean containsAnyLiquid(AABB arg) {
		int i = Mth.floor(arg.minX);
		int j = Mth.ceil(arg.maxX);
		int k = Mth.floor(arg.minY);
		int l = Mth.ceil(arg.maxY);
		int i1 = Mth.floor(arg.minZ);
		int j1 = Mth.ceil(arg.maxZ);
		BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

		for (int k1 = i; k1 < j; k1++) {
			for (int l1 = k; l1 < l; l1++) {
				for (int i2 = i1; i2 < j1; i2++) {
					BlockState blockstate = this.getBlockState(blockpos$mutableblockpos.set(k1, l1, i2));
					if (!blockstate.getFluidState().isEmpty()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	default int getMaxLocalRawBrightness(BlockPos arg) {
		return this.getMaxLocalRawBrightness(arg, this.getSkyDarken());
	}

	default int getMaxLocalRawBrightness(BlockPos arg, int i) {
		return arg.getX() >= -30000000 && arg.getZ() >= -30000000 && arg.getX() < 30000000 && arg.getZ() < 30000000 ? this.getRawBrightness(arg, i) : 15;
	}

	@Deprecated
	default boolean hasChunkAt(int i, int j) {
		return this.hasChunk(SectionPos.blockToSectionCoord(i), SectionPos.blockToSectionCoord(j));
	}

	@Deprecated
	default boolean hasChunkAt(BlockPos arg) {
		return this.hasChunkAt(arg.getX(), arg.getZ());
	}

	@Deprecated
	default boolean hasChunksAt(BlockPos arg, BlockPos arg2) {
		return this.hasChunksAt(arg.getX(), arg.getY(), arg.getZ(), arg2.getX(), arg2.getY(), arg2.getZ());
	}

	@Deprecated
	default boolean hasChunksAt(int i, int j, int k, int l, int m, int n) {
		return m >= this.getMinY() && j <= this.getMaxY() ? this.hasChunksAt(i, k, l, n) : false;
	}

	@Deprecated
	default boolean hasChunksAt(int m, int n, int o, int p) {
		int i = SectionPos.blockToSectionCoord(m);
		int j = SectionPos.blockToSectionCoord(o);
		int k = SectionPos.blockToSectionCoord(n);
		int l = SectionPos.blockToSectionCoord(p);

		for (int i1 = i; i1 <= j; i1++) {
			for (int j1 = k; j1 <= l; j1++) {
				if (!this.hasChunk(i1, j1)) {
					return false;
				}
			}
		}

		return true;
	}

	RegistryAccess registryAccess();

	FeatureFlagSet enabledFeatures();

	default <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<? extends T>> arg) {
		Registry<T> registry = this.registryAccess().lookupOrThrow(arg);
		return registry.filterFeatures(this.enabledFeatures());
	}
}
