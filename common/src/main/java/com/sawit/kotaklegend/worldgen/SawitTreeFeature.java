package com.sawit.kotaklegend.worldgen;

import com.mojang.serialization.Codec;
import com.sawit.kotaklegend.registry.ModBlocks;
import com.sawit.kotaklegend.block.SawitBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SawitTreeFeature extends Feature<NoneFeatureConfiguration> {

    public SawitTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        net.minecraft.util.RandomSource random = context.random();

        // Ensure we are placing on dirt/grass
        BlockState blockBelow = level.getBlockState(pos.below());
        if (!blockBelow.is(net.minecraft.tags.BlockTags.DIRT)) {
            return false;
        }

        // Check if there is enough space (11 blocks above) for a 2x2 trunk
        for (int i = 0; i <= 11; i++) {
            BlockPos p = pos.above(i);
            BlockState b1 = level.getBlockState(p);
            BlockState b2 = level.getBlockState(p.east());
            BlockState b3 = level.getBlockState(p.south());
            BlockState b4 = level.getBlockState(p.east().south());
            
            // Abort if another sawit tree is already here
            if (b1.is(ModBlocks.SAWIT_BLOCK.get()) || b1.is(ModBlocks.SAWIT_TRUNK.get())) return false;
            if (b2.is(ModBlocks.SAWIT_BLOCK.get()) || b2.is(ModBlocks.SAWIT_TRUNK.get())) return false;
            if (b3.is(ModBlocks.SAWIT_BLOCK.get()) || b3.is(ModBlocks.SAWIT_TRUNK.get())) return false;
            if (b4.is(ModBlocks.SAWIT_BLOCK.get()) || b4.is(ModBlocks.SAWIT_TRUNK.get())) return false;

            if (i > 0 && !b1.canBeReplaced()) return false;
            if (!b2.canBeReplaced()) return false;
            if (!b3.canBeReplaced()) return false;
            if (!b4.canBeReplaced()) return false;
        }

        int age = 5; // Force all generated trees to be mature + fruit
        BlockState sawitState = ModBlocks.SAWIT_BLOCK.get().defaultBlockState().setValue(SawitBlock.AGE, age);

        level.setBlock(pos, sawitState, 3);
        level.setBlock(pos.east(), ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
        level.setBlock(pos.south(), ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
        level.setBlock(pos.east().south(), ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);

        for (int i = 1; i <= 11; i++) {
            BlockPos p = pos.above(i);
            level.setBlock(p, ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
            level.setBlock(p.east(), ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
            level.setBlock(p.south(), ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
            level.setBlock(p.east().south(), ModBlocks.SAWIT_TRUNK.get().defaultBlockState(), 3);
        }

        return true;
    }
}
