package com.sawit.kotaklegend.client;

public class ClientSetup {
    private static boolean initialized = false;

    public static void init() {
        dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(minecraft -> {
            com.sawit.kotaklegend.client.KolestrolShaderHandler.tick(minecraft);
        });

        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register((minecraft) -> {
            if (initialized)
                return;
            initialized = true;

            // Set Render Type for Dummy Leaves to Cutout using ItemBlockRenderTypes
            try {
                Class<?> renderTypesClass = net.minecraft.client.renderer.ItemBlockRenderTypes.class;
                Class<?> chunkLayerClass = net.minecraft.client.renderer.chunk.ChunkSectionLayer.class;
                Object cutout = Enum.valueOf((Class<Enum>) chunkLayerClass, "CUTOUT");
                
                for (java.lang.reflect.Method m : renderTypesClass.getDeclaredMethods()) {
                    if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == net.minecraft.world.level.block.Block.class && m.getParameterTypes()[1] == chunkLayerClass) {
                        m.invoke(null, com.sawit.kotaklegend.registry.ModBlocks.SAWIT_LEAVES_DUMMY.get(), cutout);
                        m.invoke(null, com.sawit.kotaklegend.registry.ModBlocks.SAWIT_LEAVES_FRUIT_DUMMY.get(), cutout);
                        m.invoke(null, com.sawit.kotaklegend.registry.ModBlocks.SAWIT_DOOR.get(), cutout);
                        m.invoke(null, com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRAPDOOR.get(), cutout);
                        m.invoke(null, com.sawit.kotaklegend.registry.ModBlocks.SAWIT_BLOCK.get(), cutout);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Register WoodType materials into Sheets maps for Signs (addWoodType was removed in 1.21.x)
            try {
                net.minecraft.world.level.block.state.properties.WoodType woodType =
                        com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE;
                net.minecraft.resources.Identifier sawitLoc =
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("sawitmod", "sawit");
                net.minecraft.resources.Identifier sawitHangingLoc =
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("sawitmod", "sawit");

                net.minecraft.client.renderer.Sheets.SIGN_MATERIALS.put(
                        woodType, net.minecraft.client.renderer.Sheets.SIGN_MAPPER.apply(sawitLoc));
                net.minecraft.client.renderer.Sheets.HANGING_SIGN_MATERIALS.put(
                        woodType, net.minecraft.client.renderer.Sheets.HANGING_SIGN_MAPPER.apply(sawitHangingLoc));
            } catch (Exception e) {
                e.printStackTrace();
            }


            dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                    com.sawit.kotaklegend.registry.ModBlockEntities.SAWIT_BE.get(),
                    com.sawit.kotaklegend.client.SawitBlockEntityRenderer::new);

            dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                    (net.minecraft.world.level.block.entity.BlockEntityType<net.minecraft.world.level.block.entity.SignBlockEntity>) (Object) com.sawit.kotaklegend.registry.ModBlockEntities.SAWIT_SIGN_BE
                            .get(),
                    net.minecraft.client.renderer.blockentity.SignRenderer::new);

            dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                    (net.minecraft.world.level.block.entity.BlockEntityType<net.minecraft.world.level.block.entity.SignBlockEntity>) (Object) com.sawit.kotaklegend.registry.ModBlockEntities.SAWIT_HANGING_SIGN_BE
                            .get(),

                    net.minecraft.client.renderer.blockentity.HangingSignRenderer::new);

            dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                    com.sawit.kotaklegend.registry.ModEntityTypes.SAWIT_BOAT,
                    (context) -> new com.sawit.kotaklegend.client.SawitBoatRenderer(context, false));

            dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                    com.sawit.kotaklegend.registry.ModEntityTypes.SAWIT_CHEST_BOAT,
                    (context) -> new com.sawit.kotaklegend.client.SawitBoatRenderer(context, true));




        });
    }
}
