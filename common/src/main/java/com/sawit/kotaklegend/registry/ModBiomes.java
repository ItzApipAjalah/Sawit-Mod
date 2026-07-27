package com.sawit.kotaklegend.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class ModBiomes {
    public static final ResourceKey<Biome> SAWIT_PLANTATION = ResourceKey.create(Registries.BIOME, new ResourceLocation("sawitmod", "sawit_plantation"));
}
