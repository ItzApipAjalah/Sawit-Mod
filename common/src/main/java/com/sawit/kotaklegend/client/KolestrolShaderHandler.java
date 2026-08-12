package com.sawit.kotaklegend.client;

import net.minecraft.client.Minecraft;
import com.sawit.kotaklegend.registry.ModEffects;

public class KolestrolShaderHandler {
    private static boolean hadKolestrol = false;

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null) return;
        boolean hasKolestrol = minecraft.player.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.KOLESTROL.get()));
        
        if (hasKolestrol && !hadKolestrol) {
            minecraft.player.connection.sendCommand("soup:shader add minecraft:phosphor 10");
        } else if (!hasKolestrol && hadKolestrol) {
            minecraft.player.connection.sendCommand("soup:shader remove all");
        }
        
        hadKolestrol = hasKolestrol;
    }
}
