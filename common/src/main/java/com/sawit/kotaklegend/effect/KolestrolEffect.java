package com.sawit.kotaklegend.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class KolestrolEffect extends MobEffect {
    public KolestrolEffect() {
        super(MobEffectCategory.HARMFUL, 0xCCAA22);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Trigger effect every 40 ticks (2 seconds)
        return duration % 40 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Deal 1 damage (half heart) every tick interval
        entity.hurt(entity.damageSources().magic(), 1.0f);
        
        // Add nausea and slowness to simulate sickness
        if (!entity.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, amplifier, false, false, false));
        }
        if (!entity.hasEffect(MobEffects.CONFUSION)) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, amplifier, false, false, false));
        }
    }
}
