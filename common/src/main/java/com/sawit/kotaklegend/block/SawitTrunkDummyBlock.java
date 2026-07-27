package com.sawit.kotaklegend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.LevelReader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

public class SawitTrunkDummyBlock extends Block implements BonemealableBlock {

    // Standard 1x1 block collision for perfect physics
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public SawitTrunkDummyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        // Find the main trunk in the 2x2 grid and destroy it
        BlockPos[] checks = {pos.west(), pos.north(), pos.west().north()};
        for (BlockPos check : checks) {
            if (level.getBlockState(check).getBlock() instanceof SawitTrunkBlock) {
                level.destroyBlock(check, true);
                break;
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    private BlockPos getBasePos(LevelReader level, BlockPos pos) {
        BlockPos current = pos.below();
        while (current.getY() >= level.getMinBuildHeight()) {
            BlockState belowState = level.getBlockState(current);
            if (belowState.getBlock() instanceof SawitBlock) {
                return current;
            } else if (level.getBlockState(current.west()).getBlock() instanceof SawitBlock) {
                return current.west();
            } else if (level.getBlockState(current.north()).getBlock() instanceof SawitBlock) {
                return current.north();
            } else if (level.getBlockState(current.west().north()).getBlock() instanceof SawitBlock) {
                return current.west().north();
            } else if (!(belowState.getBlock() instanceof SawitTrunkBlock || belowState.getBlock() instanceof SawitTrunkDummyBlock)) {
                break;
            }
            current = current.below();
        }
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos basePos = getBasePos(level, pos);
        if (basePos != null) {
            BlockState baseState = level.getBlockState(basePos);
            BlockHitResult newHit = new BlockHitResult(hit.getLocation(), hit.getDirection(), basePos, hit.isInside());
            return baseState.getBlock().use(baseState, level, basePos, player, hand, newHit);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        BlockPos basePos = getBasePos(level, pos);
        if (basePos != null) {
            BlockState baseState = level.getBlockState(basePos);
            if (baseState.getBlock() instanceof BonemealableBlock) {
                return ((BonemealableBlock) baseState.getBlock()).isValidBonemealTarget(level, basePos, baseState, isClient);
            }
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos basePos = getBasePos(level, pos);
        if (basePos != null) {
            BlockState baseState = level.getBlockState(basePos);
            if (baseState.getBlock() instanceof BonemealableBlock) {
                ((BonemealableBlock) baseState.getBlock()).performBonemeal(level, random, basePos, baseState);
            }
        }
    }
}
