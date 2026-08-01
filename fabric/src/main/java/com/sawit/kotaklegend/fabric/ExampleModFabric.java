package com.sawit.kotaklegend.fabric;

import net.fabricmc.api.ModInitializer;

import com.sawit.kotaklegend.ExampleMod;

public final class ExampleModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ExampleMod.init();
        dev.architectury.event.events.common.LifecycleEvent.SETUP.register(() -> {
            terrablender.api.Regions.register(new SawitRegion(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sawitmod", "overworld"), 2));
        });
    }
}
