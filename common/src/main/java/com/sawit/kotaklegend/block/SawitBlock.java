package com.sawit.kotaklegend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import com.sawit.kotaklegend.registry.ModItems;

public class SawitBlock extends BushBlock implements BonemealableBlock, EntityBlock {
    @Override
    protected com.mojang.serialization.MapCodec<SawitBlock> codec() {
        return simpleCodec(SawitBlock::new);
    }

    public static final int MAX_AGE = 5;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;
    
    private static final VoxelShape GIANT_TREE_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 8.0D, 10.0D),
            Block.box(5.0D, 0.0D, 5.0D, 11.0D, 12.0D, 11.0D),
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D),
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 24.0D, 13.0D),
            GIANT_TREE_SHAPE, // Giant Hitbox for AGE 4
            GIANT_TREE_SHAPE  // Giant Hitbox for AGE 5
    };

    public SawitBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(AGE) >= 4) {
            return GIANT_TREE_SHAPE;
        }
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE && level.getRawBrightness(pos.above(), 0) >= 9) {
            if (random.nextInt(10) == 0) {
                growTree(level, pos, state, age + 1);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(AGE) == MAX_AGE) {
            popResource(level, pos, new ItemStack(ModItems.SAWIT_BUNCH.get(), 8));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            level.setBlock(pos, state.setValue(AGE, 4), 2);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (state.getValue(AGE) < 4) {
            return 1.0F; // Instabreak like a crop
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int newAge = Math.min(MAX_AGE, state.getValue(AGE) + 1);
        growTree(level, pos, state, newAge);
    }

    private void growTree(Level level, BlockPos pos, BlockState state, int newAge) {
        if (newAge >= 4 && state.getValue(AGE) < 4) {
            // Check if there is enough space (11 blocks above) for a 2x2 trunk
            for (int i = 0; i <= 11; i++) {
                BlockPos p = pos.above(i);
                BlockState b1 = level.getBlockState(p);
                BlockState b2 = level.getBlockState(p.east());
                BlockState b3 = level.getBlockState(p.south());
                BlockState b4 = level.getBlockState(p.east().south());
                
                // Abort if another sawit tree is in the way (skip i=0 = self)
                if (i > 0 && (b1.is(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_BLOCK.get()) || b1.is(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get()))) return;
                if (b2.is(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_BLOCK.get()) || b2.is(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get())) return;
                if (b3.is(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_BLOCK.get()) || b3.is(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get())) return;
                if (b4.is(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_BLOCK.get()) || b4.is(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get())) return;

                if (i > 0 && !b1.canBeReplaced()) return;
                if (!b2.canBeReplaced()) return;
                if (!b3.canBeReplaced()) return;
                if (!b4.canBeReplaced()) return;
            }
            level.setBlock(pos, state.setValue(AGE, newAge), 3);
            level.setBlock(pos.east(), com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
            level.setBlock(pos.south(), com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
            level.setBlock(pos.east().south(), com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
            
            for (int i = 1; i <= 11; i++) {
                BlockPos p = pos.above(i);
                level.setBlock(p, com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
                level.setBlock(p.east(), com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
                level.setBlock(p.south(), com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
                level.setBlock(p.east().south(), com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
            }
        } else {
            level.setBlock(pos, state.setValue(AGE, newAge), 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && state.getValue(AGE) >= 4) {
            for (int i = 0; i <= 11; i++) {
                BlockPos p = pos.above(i);
                if (i > 0 && level.getBlockState(p).getBlock() instanceof SawitTrunkBlock) {
                    level.destroyBlock(p, false);
                }
                if (level.getBlockState(p.east()).getBlock() instanceof SawitTrunkBlock) level.destroyBlock(p.east(), false);
                if (level.getBlockState(p.south()).getBlock() instanceof SawitTrunkBlock) level.destroyBlock(p.south(), false);
                if (level.getBlockState(p.east().south()).getBlock() instanceof SawitTrunkBlock) level.destroyBlock(p.east().south(), false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SawitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof SawitBlockEntity sawit) {
                sawit.tick(lvl, pos, st);
            }
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}

