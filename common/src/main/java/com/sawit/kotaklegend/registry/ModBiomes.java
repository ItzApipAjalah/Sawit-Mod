package com.sawit.kotaklegend.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

public class ModBiomes {
    public static final ResourceKey<Biome> SAWIT_PLANTATION = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("sawitmod", "sawit_plantation"));
}
