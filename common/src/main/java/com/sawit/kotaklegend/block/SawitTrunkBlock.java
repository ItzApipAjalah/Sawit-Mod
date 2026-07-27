package com.sawit.kotaklegend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import com.sawit.kotaklegend.registry.ModBlocks;

import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.LevelReader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

public class SawitTrunkBlock extends Block implements BonemealableBlock {
    // 2x2 blocks wide collision shape (32x32 pixels)
    private static final VoxelShape TRUNK_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public SawitTrunkBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TRUNK_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }


    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Find the base SawitBlock and destroy it
        BlockPos basePos = getBasePos(level, pos);
        if (basePos != null) {
            BlockState baseState = level.getBlockState(basePos);
            if (baseState.getBlock() instanceof SawitBlock) {
                level.destroyBlock(basePos, true);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    private BlockPos getBasePos(LevelReader level, BlockPos pos) {
        BlockPos current = pos;
        while (current.getY() >= level.getMinBuildHeight()) {
            BlockState currentState = level.getBlockState(current);
            if (currentState.getBlock() instanceof SawitBlock) {
                return current;
            } else if (level.getBlockState(current.west()).getBlock() instanceof SawitBlock) {
                return current.west();
            } else if (level.getBlockState(current.north()).getBlock() instanceof SawitBlock) {
                return current.north();
            } else if (level.getBlockState(current.west().north()).getBlock() instanceof SawitBlock) {
                return current.west().north();
            } else if (!(currentState.getBlock() instanceof SawitTrunkBlock)) {
                break;
            }
            current = current.below();
        }
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.getItem() instanceof AxeItem) {
            level.playSound(player, pos, net.minecraft.sounds.SoundEvents.AXE_STRIP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!level.isClientSide) {
                level.setBlock(pos, ModBlocks.STRIPPED_SAWIT_LOG.get().defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y), 11);
                itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos basePos = getBasePos(level, pos);
        if (basePos != null) {
            BlockState baseState = level.getBlockState(basePos);
            return baseState.getBlock().use(baseState, level, basePos, player, hand, hit);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos basePos = getBasePos(level, pos);
        if (basePos != null) {
            BlockState baseState = level.getBlockState(basePos);
            return ((BonemealableBlock) baseState.getBlock()).isValidBonemealTarget(level, basePos, baseState);
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
            ((BonemealableBlock) baseState.getBlock()).performBonemeal(level, random, basePos, baseState);
        }
    }
}

