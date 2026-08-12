package com.sawit.kotaklegend.block.entity;

import com.sawit.kotaklegend.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SawitHangingSignBlockEntity extends HangingSignBlockEntity {
    public SawitHangingSignBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.SAWIT_HANGING_SIGN_BE.get();
    }

    @Override
    public boolean isValidBlockState(BlockState state) {
        return true;
    }
}
