package com.sawit.kotaklegend.neoforge;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import com.sawit.kotaklegend.ExampleMod;
@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge(IEventBus modEventBus) {
        ExampleMod.init();
        modEventBus.addListener(this::setupTerraBlender);
    }
    private void setupTerraBlender(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            terrablender.api.Regions.register(new SawitRegion(new net.minecraft.resources.ResourceLocation("sawitmod", "overworld"), 2));
        });
    }
}
