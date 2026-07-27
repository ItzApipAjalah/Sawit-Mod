package com.sawit.kotaklegend.quilt;

import com.sawit.kotaklegend.registry.ModBiomes;
import terrablender.api.Regions;
import terrablender.api.TerraBlenderApi;

public class SawitTerraBlenderPlugin implements TerraBlenderApi {
    @Override
    public void onTerraBlenderInitialized() {
        Regions.register(new SawitRegion(new net.minecraft.resources.ResourceLocation("sawitmod", "overworld"), 2));
    }
}
