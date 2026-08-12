package com.sawit.kotaklegend.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import com.sawit.kotaklegend.registry.ModEffects;

public class KolestrolShaderHandler implements ClientTickEvent.Client {
    private boolean blurLoaded = false;

    @Override
    public void tick(Minecraft client) {
        if (client.player != null) {
            if (client.player.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.KOLESTROL.get()))) {
                try {
                    for (java.lang.reflect.Method m : client.gameRenderer.getClass().getDeclaredMethods()) {
                        if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == net.minecraft.resources.ResourceLocation.class) {
                            m.setAccessible(true);
                            m.invoke(client.gameRenderer, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sawitmod", "phosphor"));
                            break;
                        }
                    }
                } catch (Exception e) {}
            } else {
                client.gameRenderer.checkEntityPostEffect(client.getCameraEntity());
            }
        }
    }
}
