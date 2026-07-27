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


        dev.architectury.registry.fuel.FuelRegistry.register(2000, com.sawit.kotaklegend.registry.ModItems.SAWIT_OIL_3.get());
        dev.architectury.registry.fuel.FuelRegistry.register(2000, com.sawit.kotaklegend.registry.ModItems.SAWIT_OIL_2.get());
        dev.architectury.registry.fuel.FuelRegistry.register(2000, com.sawit.kotaklegend.registry.ModItems.SAWIT_OIL_1.get());

        // Composter
        net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(com.sawit.kotaklegend.registry.ModItems.SAWIT_FRUIT.get(), 0.2f);
        net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(com.sawit.kotaklegend.registry.ModItems.SAWIT_BUNCH.get(), 0.8f);


        com.sawit.kotaklegend.registry.ModBlockEntities.register();

        com.sawit.kotaklegend.registry.ModVillagers.registerTrades();

        dev.architectury.utils.EnvExecutor.runInEnv(net.fabricmc.api.EnvType.CLIENT, () -> () -> {
            dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                net.minecraft.client.renderer.RenderType.cutout(),
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_BLOCK.get(),
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK_DUMMY.get(),
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_LEAVES_DUMMY.get(),
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_LEAVES_FRUIT_DUMMY.get()
            );
            dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                com.sawit.kotaklegend.registry.ModBlockEntities.SAWIT_BE.get(),
                com.sawit.kotaklegend.client.SawitBlockEntityRenderer::new
            );

            dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(new com.sawit.kotaklegend.client.KolestrolShaderHandler());
            dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(com.sawit.kotaklegend.registry.ModEntityTypes.SAWIT_BOAT, (context) -> new com.sawit.kotaklegend.client.SawitBoatRenderer(context, false));
            dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(com.sawit.kotaklegend.registry.ModEntityTypes.SAWIT_CHEST_BOAT, (context) -> new com.sawit.kotaklegend.client.SawitBoatRenderer(context, true));
        });
    }
}
