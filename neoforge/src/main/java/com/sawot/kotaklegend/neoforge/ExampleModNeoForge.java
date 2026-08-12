package com.sawot.kotaklegend.neoforge;

import net.neoforged.fml.common.Mod;

import com.sawot.kotaklegend.ExampleMod;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge() {
        // Run our common setup.
        ExampleMod.init();
    }
}
