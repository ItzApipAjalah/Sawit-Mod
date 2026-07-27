package com.sawit.kotaklegend.client;

public class ClientSetup {
    private static boolean initialized = false;

    public static void init() {
        dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register((minecraft) -> {
            if (initialized) return;
            initialized = true;
            dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                net.minecraft.client.renderer.RenderType.cutout(),
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_BLOCK.get(),
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRUNK_DUMMY.get(),
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_LEAVES_DUMMY.get(),
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_LEAVES_FRUIT_DUMMY.get(),
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_DOOR.get(),
                com.sawit.kotaklegend.registry.ModBlocks.SAWIT_TRAPDOOR.get()
            );
            dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                com.sawit.kotaklegend.registry.ModBlockEntities.SAWIT_BE.get(),
                com.sawit.kotaklegend.client.SawitBlockEntityRenderer::new
            );
            dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                (net.minecraft.world.level.block.entity.BlockEntityType<net.minecraft.world.level.block.entity.SignBlockEntity>) (Object) com.sawit.kotaklegend.registry.ModBlockEntities.SAWIT_SIGN_BE.get(),
                net.minecraft.client.renderer.blockentity.SignRenderer::new
            );
            dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                (net.minecraft.world.level.block.entity.BlockEntityType<net.minecraft.world.level.block.entity.SignBlockEntity>) (Object) com.sawit.kotaklegend.registry.ModBlockEntities.SAWIT_HANGING_SIGN_BE.get(),
                net.minecraft.client.renderer.blockentity.HangingSignRenderer::new
            );

            // Entity model layers are automatically registered by Vanilla for WoodTypes in WoodType.VALUES

            try {
                // Read HANGING_SIGN_MATERIALS via reflection to see what texture it points to
                for (java.lang.reflect.Field field : net.minecraft.client.renderer.Sheets.class.getDeclaredFields()) {
                    if (java.util.Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        java.util.Map map = (java.util.Map) field.get(null);
                        if (map.containsKey(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE)) {
                            Object val = map.get(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE);
                            if (val instanceof net.minecraft.client.resources.model.Material) {
                                net.minecraft.client.resources.model.Material mat = (net.minecraft.client.resources.model.Material) val;
                                System.out.println("[DEBUG SAWIT] Map contains SAWIT_WOOD_TYPE. Texture path: " + mat.texture());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[DEBUG SAWIT] Failed to read maps");
                e.printStackTrace();
            }

            dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(new com.sawit.kotaklegend.client.KolestrolShaderHandler());
            dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.sawit.kotaklegend.registry.ModEntityTypes.SAWIT_BOAT, 
                (context) -> new com.sawit.kotaklegend.client.SawitBoatRenderer(context, false)
            );
            dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
                com.sawit.kotaklegend.registry.ModEntityTypes.SAWIT_CHEST_BOAT, 
                (context) -> new com.sawit.kotaklegend.client.SawitBoatRenderer(context, true)
            );
        });
    }
}
