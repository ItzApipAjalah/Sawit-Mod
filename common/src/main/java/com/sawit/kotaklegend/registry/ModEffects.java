package com.sawit.kotaklegend.registry;

import com.sawit.kotaklegend.ExampleMod;
import com.sawit.kotaklegend.effect.KolestrolEffect;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ExampleMod.MOD_ID, Registries.MOB_EFFECT);

    public static final RegistrySupplier<MobEffect> KOLESTROL = EFFECTS.register("kolestrol", KolestrolEffect::new);

    public static void register() {
        EFFECTS.register();
    }
}
