package com.sawit.kotaklegend.quilt;

import net.fabricmc.api.ModInitializer;

import com.sawit.kotaklegend.fabriclike.ExampleModFabricLike;

public final class ExampleModQuilt implements ModInitializer {
    @Override
    public void onInitialize() {
        // Run the Fabric-like setup.
        ExampleModFabricLike.init();
    }
}
