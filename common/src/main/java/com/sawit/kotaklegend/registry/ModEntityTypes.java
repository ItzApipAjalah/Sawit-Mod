package com.sawit.kotaklegend.registry;

import com.sawit.kotaklegend.ExampleMod;
import com.sawit.kotaklegend.entity.SawitBoatEntity;
import com.sawit.kotaklegend.entity.SawitChestBoatEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ExampleMod.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<SawitBoatEntity>> SAWIT_BOAT = ENTITY_TYPES.register("sawit_boat", () ->
            EntityType.Builder.<SawitBoatEntity>of(SawitBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build("boat"));

    public static final RegistrySupplier<EntityType<SawitChestBoatEntity>> SAWIT_CHEST_BOAT = ENTITY_TYPES.register("sawit_chest_boat", () ->
            EntityType.Builder.<SawitChestBoatEntity>of(SawitChestBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .clientTrackingRange(10)
                    .build("chest_boat"));

    public static void register() {
        ENTITY_TYPES.register();
    }
}
