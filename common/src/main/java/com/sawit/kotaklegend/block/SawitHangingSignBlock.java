package com.sawit.kotaklegend.block;

import com.sawit.kotaklegend.block.entity.SawitHangingSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

public class SawitHangingSignBlock extends CeilingHangingSignBlock {
    public SawitHangingSignBlock(Properties properties, WoodType woodType) {
        super(woodType, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SawitHangingSignBlockEntity(pos, state);
    }
}

