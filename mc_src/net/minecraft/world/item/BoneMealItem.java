package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;

public class BoneMealItem extends Item {
	public static final int GRASS_SPREAD_WIDTH = 3;
	public static final int GRASS_SPREAD_HEIGHT = 1;
	public static final int GRASS_COUNT_MULTIPLIER = 3;

	public BoneMealItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Level level = arg.getLevel();
		BlockPos blockpos = arg.getClickedPos();
		BlockPos blockpos1 = blockpos.relative(arg.getClickedFace());
		if (applyBonemeal(arg.getItemInHand(), level, blockpos, arg.getPlayer())) {
			if (!level.isClientSide) {
				arg.getPlayer().gameEvent(GameEvent.ITEM_INTERACT_FINISH);
				level.levelEvent(1505, blockpos, 15);
			}

			return InteractionResult.SUCCESS;
		} else {
			BlockState blockstate = level.getBlockState(blockpos);
			boolean flag = blockstate.isFaceSturdy(level, blockpos, arg.getClickedFace());
			if (flag && growWaterPlant(arg.getItemInHand(), level, blockpos1, arg.getClickedFace())) {
				if (!level.isClientSide) {
					arg.getPlayer().gameEvent(GameEvent.ITEM_INTERACT_FINISH);
					level.levelEvent(1505, blockpos1, 15);
				}

				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.PASS;
			}
		}
	}

	@Deprecated
	public static boolean growCrop(ItemStack arg, Level arg2, BlockPos arg3) {
		return arg2 instanceof ServerLevel ? applyBonemeal(arg, arg2, arg3, null) : false;
	}

	public static boolean applyBonemeal(ItemStack arg, Level arg2, BlockPos arg3, @Nullable Player player) {
		BlockState blockstate = arg2.getBlockState(arg3);
		BonemealEvent event = EventHooks.fireBonemealEvent(player, arg2, arg3, blockstate, arg);
		if (event.isCanceled()) {
			return event.isSuccessful();
		} else if (blockstate.getBlock() instanceof BonemealableBlock bonemealableblock && bonemealableblock.isValidBonemealTarget(arg2, arg3, blockstate)) {
			if (arg2 instanceof ServerLevel) {
				if (bonemealableblock.isBonemealSuccess(arg2, arg2.random, arg3, blockstate)) {
					bonemealableblock.performBonemeal((ServerLevel)arg2, arg2.random, arg3, blockstate);
				}

				arg.shrink(1);
			}

			return true;
		} else {
			return false;
		}
	}

	public static boolean growWaterPlant(ItemStack arg, Level arg2, BlockPos arg3, @Nullable Direction arg4) {
		if (arg2.getBlockState(arg3).is(Blocks.WATER) && arg2.getFluidState(arg3).getAmount() == 8) {
			if (!(arg2 instanceof ServerLevel)) {
				return true;
			} else {
				RandomSource randomsource = arg2.getRandom();

				label79:
				for (int i = 0; i < 128; i++) {
					BlockPos blockpos = arg3;
					BlockState blockstate = Blocks.SEAGRASS.defaultBlockState();

					for (int j = 0; j < i / 16; j++) {
						blockpos = blockpos.offset(randomsource.nextInt(3) - 1, (randomsource.nextInt(3) - 1) * randomsource.nextInt(3) / 2, randomsource.nextInt(3) - 1);
						if (arg2.getBlockState(blockpos).isCollisionShapeFullBlock(arg2, blockpos)) {
							continue label79;
						}
					}

					Holder<Biome> holder = arg2.getBiome(blockpos);
					if (holder.is(BiomeTags.PRODUCES_CORALS_FROM_BONEMEAL)) {
						if (i == 0 && arg4 != null && arg4.getAxis().isHorizontal()) {
							blockstate = (BlockState)BuiltInRegistries.BLOCK
								.getRandomElementOf(BlockTags.WALL_CORALS, arg2.random)
								.map(argx -> ((Block)argx.value()).defaultBlockState())
								.orElse(blockstate);
							if (blockstate.hasProperty(BaseCoralWallFanBlock.FACING)) {
								blockstate = blockstate.setValue(BaseCoralWallFanBlock.FACING, arg4);
							}
						} else if (randomsource.nextInt(4) == 0) {
							blockstate = (BlockState)BuiltInRegistries.BLOCK
								.getRandomElementOf(BlockTags.UNDERWATER_BONEMEALS, arg2.random)
								.map(argx -> ((Block)argx.value()).defaultBlockState())
								.orElse(blockstate);
						}
					}

					if (blockstate.is(BlockTags.WALL_CORALS, argx -> argx.hasProperty(BaseCoralWallFanBlock.FACING))) {
						for (int k = 0; !blockstate.canSurvive(arg2, blockpos) && k < 4; k++) {
							blockstate = blockstate.setValue(BaseCoralWallFanBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(randomsource));
						}
					}

					if (blockstate.canSurvive(arg2, blockpos)) {
						BlockState blockstate1 = arg2.getBlockState(blockpos);
						if (blockstate1.is(Blocks.WATER) && arg2.getFluidState(blockpos).getAmount() == 8) {
							arg2.setBlock(blockpos, blockstate, 3);
						} else if (blockstate1.is(Blocks.SEAGRASS)
							&& ((BonemealableBlock)Blocks.SEAGRASS).isValidBonemealTarget(arg2, blockpos, blockstate1)
							&& randomsource.nextInt(10) == 0) {
							((BonemealableBlock)Blocks.SEAGRASS).performBonemeal((ServerLevel)arg2, randomsource, blockpos, blockstate1);
						}
					}
				}

				arg.shrink(1);
				return true;
			}
		} else {
			return false;
		}
	}

	public static void addGrowthParticles(LevelAccessor arg, BlockPos arg2, int i) {
		BlockState blockstate = arg.getBlockState(arg2);
		if (blockstate.getBlock() instanceof BonemealableBlock bonemealableblock) {
			BlockPos blockpos = bonemealableblock.getParticlePos(arg2);
			switch (bonemealableblock.getType()) {
				case NEIGHBOR_SPREADER:
					ParticleUtils.spawnParticles(arg, blockpos, i * 3, 3.0, 1.0, false, ParticleTypes.HAPPY_VILLAGER);
					break;
				case GROWER:
					ParticleUtils.spawnParticleInBlock(arg, blockpos, i, ParticleTypes.HAPPY_VILLAGER);
			}
		} else if (blockstate.is(Blocks.WATER)) {
			ParticleUtils.spawnParticles(arg, arg2, i * 3, 3.0, 1.0, false, ParticleTypes.HAPPY_VILLAGER);
		}
	}
}
