package com.sawit.kotaklegend.block;

import com.sawit.kotaklegend.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SawitBlockEntity extends BlockEntity {
    private boolean hasSynced = false;
    private int syncDelay = 0;

    public SawitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAWIT_BE.get(), pos, state);
    }

    public void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        if (!hasSynced) {
            syncDelay++;
            // Force a block update half a second after generation/loading to ensure client receives the block entity
            if (syncDelay >= 10) { 
                level.sendBlockUpdated(pos, state, state, 3);
                hasSynced = true;
            }
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("HasSynced", hasSynced);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        hasSynced = input.getBooleanOr("HasSynced", false);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) {
        return this.saveWithoutMetadata(provider);
    }
}
