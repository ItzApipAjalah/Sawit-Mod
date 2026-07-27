package com.sawit.kotaklegend;

public final class ExampleMod {
    public static final String MOD_ID = "sawitmod";

    public static void init() {
        com.sawit.kotaklegend.registry.ModBlocks.register();
        com.sawit.kotaklegend.registry.ModEffects.register();
        com.sawit.kotaklegend.registry.ModItems.register();
        com.sawit.kotaklegend.registry.ModEntityTypes.register();
        com.sawit.kotaklegend.registry.ModFeatures.register();
        com.sawit.kotaklegend.registry.ModVillagers.register();


        dev.architectury.event.events.common.LifecycleEvent.SETUP.register(() -> {
            dev.architectury.registry.fuel.FuelRegistry.register(2000, com.sawit.kotaklegend.registry.ModItems.SAWIT_OIL_3.get());
            dev.architectury.registry.fuel.FuelRegistry.register(2000, com.sawit.kotaklegend.registry.ModItems.SAWIT_OIL_2.get());
            dev.architectury.registry.fuel.FuelRegistry.register(2000, com.sawit.kotaklegend.registry.ModItems.SAWIT_OIL_1.get());

            // Composter
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(com.sawit.kotaklegend.registry.ModItems.SAWIT_FRUIT.get(), 0.2f);
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(com.sawit.kotaklegend.registry.ModItems.SAWIT_BUNCH.get(), 0.8f);
            
            // Villager Trades
            com.sawit.kotaklegend.registry.ModVillagers.registerTrades();
        });


        com.sawit.kotaklegend.registry.ModBlockEntities.register();

        dev.architectury.utils.EnvExecutor.runInEnv(net.fabricmc.api.EnvType.CLIENT, () -> com.sawit.kotaklegend.client.ClientSetup::init);
    }
}
