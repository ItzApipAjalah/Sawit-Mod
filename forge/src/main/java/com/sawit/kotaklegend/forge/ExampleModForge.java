package com.sawit.kotaklegend.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.sawit.kotaklegend.ExampleMod;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModForge {
    public ExampleModForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(ExampleMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        ExampleMod.init();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupTerraBlender);
    }

    private void setupTerraBlender(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            terrablender.api.Regions.register(new SawitRegion(new net.minecraft.resources.ResourceLocation("sawitmod", "overworld"), 2));
        });
    }
}
