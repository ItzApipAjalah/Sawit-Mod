package com.sawit.kotaklegend.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import com.sawit.kotaklegend.registry.ModEffects;
import com.sawit.kotaklegend.mixin.GameRendererAccessor;

public class KolestrolShaderHandler implements ClientTickEvent.Client {
    private boolean blurLoaded = false;

    @Override
    public void tick(Minecraft client) {
        if (client.player != null) {
            if (client.player.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.KOLESTROL.get()))) {
                if (client.gameRenderer.currentEffect() == null) {
                    ((GameRendererAccessor) client.gameRenderer).invokeLoadEffect(ResourceLocation.fromNamespaceAndPath("sawitmod", "shaders/post/phosphor.json"));
                    blurLoaded = true;
                }
            } else if (blurLoaded) {
                client.gameRenderer.shutdownEffect();
                blurLoaded = false;
            }
        }
    }
}
