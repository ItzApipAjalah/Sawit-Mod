package com.sawit.kotaklegend.registry;

import com.sawit.kotaklegend.ExampleMod;
import com.sawit.kotaklegend.block.SawitBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ExampleMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<SawitBlockEntity>> SAWIT_BE = BLOCK_ENTITIES.register("sawit_be", () ->
            BlockEntityType.Builder.of(SawitBlockEntity::new, ModBlocks.SAWIT_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.sawit.kotaklegend.block.entity.SawitSignBlockEntity>> SAWIT_SIGN_BE = BLOCK_ENTITIES.register("sawit_sign_be", () ->
            BlockEntityType.Builder.of(com.sawit.kotaklegend.block.entity.SawitSignBlockEntity::new, ModBlocks.SAWIT_SIGN.get(), ModBlocks.SAWIT_WALL_SIGN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<com.sawit.kotaklegend.block.entity.SawitHangingSignBlockEntity>> SAWIT_HANGING_SIGN_BE = BLOCK_ENTITIES.register("sawit_hanging_sign_be", () ->
            BlockEntityType.Builder.of(com.sawit.kotaklegend.block.entity.SawitHangingSignBlockEntity::new, ModBlocks.SAWIT_HANGING_SIGN.get(), ModBlocks.SAWIT_WALL_HANGING_SIGN.get()).build(null));


    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
