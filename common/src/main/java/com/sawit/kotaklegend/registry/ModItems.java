package com.sawit.kotaklegend.registry;

import com.sawit.kotaklegend.ExampleMod;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {
    private static Item.Properties props(String name) {
        return new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, name)));
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ExampleMod.MOD_ID, Registries.ITEM);

    // Sawit seeds will place the sawit block
    public static final RegistrySupplier<Item> SAWIT_SEEDS = ITEMS.register("sawit_seeds", () ->
            new BlockItem(ModBlocks.SAWIT_BLOCK.get(), props("sawit_seeds")));

    public static final RegistrySupplier<Item> SAWIT_FRUIT = ITEMS.register("sawit_fruit", () ->
            new Item(props("sawit_fruit").food(
                new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.2f)
                    .build(),
                net.minecraft.world.item.component.Consumable.builder()
                    .onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.KOLESTROL.get()), 300, 0), 1.0f))
                    .build()
            )));

    public static final RegistrySupplier<Item> SAWIT_BUNCH = ITEMS.register("sawit_bunch", () ->
            new Item(props("sawit_bunch")));

    public static final RegistrySupplier<Item> JELANTAH_OIL = ITEMS.register("jelantah_oil", () ->
            new com.sawit.kotaklegend.item.SawitOilItem(props("jelantah_oil").craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE), 0, true));

    public static final RegistrySupplier<Item> SAWIT_OIL_1 = ITEMS.register("sawit_oil_1", () ->
            new com.sawit.kotaklegend.item.SawitOilItem(props("sawit_oil_1").craftRemainder(ModItems.JELANTAH_OIL.get()), 1, false));

    public static final RegistrySupplier<Item> SAWIT_OIL_2 = ITEMS.register("sawit_oil_2", () ->
            new com.sawit.kotaklegend.item.SawitOilItem(props("sawit_oil_2").craftRemainder(ModItems.SAWIT_OIL_1.get()), 2, false));

    public static final RegistrySupplier<Item> SAWIT_OIL_3 = ITEMS.register("sawit_oil_3", () ->
            new com.sawit.kotaklegend.item.SawitOilItem(props("sawit_oil_3").craftRemainder(ModItems.SAWIT_OIL_2.get()), 3, false));

    public static final RegistrySupplier<Item> SAWIT_LOG = ITEMS.register("sawit_log", () ->
            new BlockItem(ModBlocks.SAWIT_LOG.get(), props("sawit_log")));

    public static final RegistrySupplier<Item> SAWIT_WOOD = ITEMS.register("sawit_wood", () ->
            new BlockItem(ModBlocks.SAWIT_WOOD.get(), props("sawit_wood")));

    public static final RegistrySupplier<Item> STRIPPED_SAWIT_LOG = ITEMS.register("stripped_sawit_log", () ->
            new BlockItem(ModBlocks.STRIPPED_SAWIT_LOG.get(), props("stripped_sawit_log")));

    public static final RegistrySupplier<Item> STRIPPED_SAWIT_WOOD = ITEMS.register("stripped_sawit_wood", () ->
            new BlockItem(ModBlocks.STRIPPED_SAWIT_WOOD.get(), props("stripped_sawit_wood")));

    public static final RegistrySupplier<Item> SAWIT_PLANKS = ITEMS.register("sawit_planks", () ->
            new BlockItem(ModBlocks.SAWIT_PLANKS.get(), props("sawit_planks")));

    public static final RegistrySupplier<Item> SAWIT_BOAT = ITEMS.register("sawit_boat", () ->
            new com.sawit.kotaklegend.item.SawitBoatItem(false, props("sawit_boat").stacksTo(1)));

    public static final RegistrySupplier<Item> SAWIT_CHEST_BOAT = ITEMS.register("sawit_chest_boat", () ->
            new com.sawit.kotaklegend.item.SawitBoatItem(true, props("sawit_chest_boat").stacksTo(1)));

    public static final RegistrySupplier<Item> SAWIT_BAG = ITEMS.register("sawit_bag", () ->
            new BlockItem(ModBlocks.SAWIT_BAG.get(), props("sawit_bag")));

    public static final RegistrySupplier<Item> SAWIT_STAIRS = ITEMS.register("sawit_stairs", () ->
            new BlockItem(ModBlocks.SAWIT_STAIRS.get(), props("sawit_stairs")));
    public static final RegistrySupplier<Item> SAWIT_SLAB = ITEMS.register("sawit_slab", () ->
            new BlockItem(ModBlocks.SAWIT_SLAB.get(), props("sawit_slab")));
    public static final RegistrySupplier<Item> SAWIT_FENCE = ITEMS.register("sawit_fence", () ->
            new BlockItem(ModBlocks.SAWIT_FENCE.get(), props("sawit_fence")));
    public static final RegistrySupplier<Item> SAWIT_FENCE_GATE = ITEMS.register("sawit_fence_gate", () ->
            new BlockItem(ModBlocks.SAWIT_FENCE_GATE.get(), props("sawit_fence_gate")));
    public static final RegistrySupplier<Item> SAWIT_DOOR = ITEMS.register("sawit_door", () ->
            new BlockItem(ModBlocks.SAWIT_DOOR.get(), props("sawit_door")));
    public static final RegistrySupplier<Item> SAWIT_TRAPDOOR = ITEMS.register("sawit_trapdoor", () ->
            new BlockItem(ModBlocks.SAWIT_TRAPDOOR.get(), props("sawit_trapdoor")));
    public static final RegistrySupplier<Item> SAWIT_BUTTON = ITEMS.register("sawit_button", () ->
            new BlockItem(ModBlocks.SAWIT_BUTTON.get(), props("sawit_button")));
    public static final RegistrySupplier<Item> SAWIT_PRESSURE_PLATE = ITEMS.register("sawit_pressure_plate", () ->
            new BlockItem(ModBlocks.SAWIT_PRESSURE_PLATE.get(), props("sawit_pressure_plate")));

    public static final RegistrySupplier<Item> SAWIT_SIGN = ITEMS.register("sawit_sign", () ->
            new net.minecraft.world.item.SignItem(ModBlocks.SAWIT_SIGN.get(), ModBlocks.SAWIT_WALL_SIGN.get(), props("sawit_sign").stacksTo(16)));

    public static final RegistrySupplier<Item> SAWIT_HANGING_SIGN = ITEMS.register("sawit_hanging_sign", () ->
            new net.minecraft.world.item.HangingSignItem(ModBlocks.SAWIT_HANGING_SIGN.get(), ModBlocks.SAWIT_WALL_HANGING_SIGN.get(), props("sawit_hanging_sign").stacksTo(16)));

    public static void register() {
        ITEMS.register();
        
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.NATURAL_BLOCKS, SAWIT_SEEDS);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.FOOD_AND_DRINKS, SAWIT_FRUIT);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.NATURAL_BLOCKS, SAWIT_BUNCH);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS, SAWIT_OIL_3);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS, SAWIT_OIL_2);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS, SAWIT_OIL_1);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS, JELANTAH_OIL);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_LOG);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_WOOD);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, STRIPPED_SAWIT_LOG);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, STRIPPED_SAWIT_WOOD);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_PLANKS);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_BAG);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_STAIRS);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_SLAB);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_FENCE);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_FENCE_GATE);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_DOOR);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_TRAPDOOR);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_BUTTON);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_PRESSURE_PLATE);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_SIGN);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, SAWIT_HANGING_SIGN);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES, SAWIT_BOAT);
        dev.architectury.registry.CreativeTabRegistry.append(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES, SAWIT_CHEST_BOAT);
    }
}
