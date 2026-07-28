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
                (context) -> {
                    net.minecraft.client.renderer.blockentity.SignRenderer renderer = new net.minecraft.client.renderer.blockentity.SignRenderer(context);
                    try {
                        for (java.lang.reflect.Field field : net.minecraft.client.renderer.blockentity.SignRenderer.class.getDeclaredFields()) {
                            if (java.util.Map.class.isAssignableFrom(field.getType())) {
                                field.setAccessible(true);
                                java.util.Map map = (java.util.Map) field.get(renderer);
                                if (map != null && !map.containsKey(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE)) {
                                    net.minecraft.client.model.geom.ModelPart part = context.bakeLayer(new net.minecraft.client.model.geom.ModelLayerLocation(new net.minecraft.resources.ResourceLocation("minecraft", "sign/sawit"), "main"));
                                    net.minecraft.client.renderer.blockentity.SignRenderer.SignModel signModel = new net.minecraft.client.renderer.blockentity.SignRenderer.SignModel(part);
                                    try {
                                        map.put(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE, signModel);
                                    } catch (UnsupportedOperationException e) {
                                        java.util.Map newMap = new java.util.HashMap(map);
                                        newMap.put(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE, signModel);
                                        field.set(renderer, newMap);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                    return renderer;
                }
            );
            dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                (net.minecraft.world.level.block.entity.BlockEntityType<net.minecraft.world.level.block.entity.SignBlockEntity>) (Object) com.sawit.kotaklegend.registry.ModBlockEntities.SAWIT_HANGING_SIGN_BE.get(),
                (context) -> {
                    net.minecraft.client.renderer.blockentity.HangingSignRenderer renderer = new net.minecraft.client.renderer.blockentity.HangingSignRenderer(context);
                    try {
                        for (java.lang.reflect.Field field : net.minecraft.client.renderer.blockentity.HangingSignRenderer.class.getDeclaredFields()) {
                            if (java.util.Map.class.isAssignableFrom(field.getType())) {
                                field.setAccessible(true);
                                java.util.Map map = (java.util.Map) field.get(renderer);
                                if (map != null && !map.containsKey(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE)) {
                                    net.minecraft.client.model.geom.ModelPart part = context.bakeLayer(new net.minecraft.client.model.geom.ModelLayerLocation(new net.minecraft.resources.ResourceLocation("minecraft", "hanging_sign/sawit"), "main"));
                                    net.minecraft.client.renderer.blockentity.HangingSignRenderer.HangingSignModel signModel = new net.minecraft.client.renderer.blockentity.HangingSignRenderer.HangingSignModel(part);
                                    try {
                                        map.put(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE, signModel);
                                    } catch (UnsupportedOperationException e) {
                                        java.util.Map newMap = new java.util.HashMap(map);
                                        newMap.put(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE, signModel);
                                        field.set(renderer, newMap);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                    return renderer;
                }
            );

            boolean isRegisteredByVanilla = net.minecraft.world.level.block.state.properties.WoodType.values().anyMatch(w -> w == com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE);
            if (!isRegisteredByVanilla) {
                dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                    new net.minecraft.client.model.geom.ModelLayerLocation(new net.minecraft.resources.ResourceLocation("minecraft", "sign/sawit"), "main"),
                    net.minecraft.client.renderer.blockentity.SignRenderer::createSignLayer
                );
                dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                    new net.minecraft.client.model.geom.ModelLayerLocation(new net.minecraft.resources.ResourceLocation("minecraft", "hanging_sign/sawit"), "main"),
                    net.minecraft.client.renderer.blockentity.HangingSignRenderer::createHangingSignLayer
                );
            }

            // Entity model layers are automatically registered by Vanilla for WoodTypes in WoodType.VALUES

            try {
                for (java.lang.reflect.Field field : net.minecraft.client.renderer.Sheets.class.getDeclaredFields()) {
                    if (java.util.Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        java.util.Map<net.minecraft.world.level.block.state.properties.WoodType, net.minecraft.client.resources.model.Material> map = 
                            (java.util.Map<net.minecraft.world.level.block.state.properties.WoodType, net.minecraft.client.resources.model.Material>) field.get(null);
                        
                        if (!map.containsKey(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE)) {
                            if (map.containsKey(net.minecraft.world.level.block.state.properties.WoodType.OAK)) {
                                net.minecraft.client.resources.model.Material oakMat = map.get(net.minecraft.world.level.block.state.properties.WoodType.OAK);
                                boolean isHanging = oakMat.texture().getPath().contains("hanging");
                                net.minecraft.client.resources.model.Material sawitMat = new net.minecraft.client.resources.model.Material(
                                    net.minecraft.client.renderer.Sheets.SIGN_SHEET, 
                                    new net.minecraft.resources.ResourceLocation(isHanging ? "entity/signs/hanging/sawit" : "entity/signs/sawit")
                                );
                                map.put(com.sawit.kotaklegend.registry.ModBlocks.SAWIT_WOOD_TYPE, sawitMat);
                                System.out.println("[DEBUG SAWIT] Successfully injected sawit sign material into Sheets!");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[DEBUG SAWIT] Failed to inject sign materials");
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
