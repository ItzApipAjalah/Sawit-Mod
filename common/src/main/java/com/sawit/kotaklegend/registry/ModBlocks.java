package com.sawit.kotaklegend.registry;

import com.sawit.kotaklegend.ExampleMod;
import com.sawit.kotaklegend.block.SawitBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public class ModBlocks {
    private static BlockSetType registerBlockSetType(BlockSetType type) {
        try {
            java.lang.reflect.Method m = BlockSetType.class.getDeclaredMethod("register", BlockSetType.class);
            m.setAccessible(true);
            return (BlockSetType) m.invoke(null, type);
        } catch (Exception e) {
            try {
                java.lang.reflect.Method m = BlockSetType.class.getDeclaredMethod("m_271928_", BlockSetType.class);
                m.setAccessible(true);
                return (BlockSetType) m.invoke(null, type);
            } catch (Exception e2) {}
            return type;
        }
    }

    private static WoodType registerWoodType(WoodType type) {
        try {
            java.lang.reflect.Method m = WoodType.class.getDeclaredMethod("register", WoodType.class);
            m.setAccessible(true);
            return (WoodType) m.invoke(null, type);
        } catch (Exception e) {
            try {
                java.lang.reflect.Method m = WoodType.class.getDeclaredMethod("m_272210_", WoodType.class);
                m.setAccessible(true);
                return (WoodType) m.invoke(null, type);
            } catch (Exception e2) {
                // Fallback: inject into the static Collection/Set field
                try {
                    for (java.lang.reflect.Field field : WoodType.class.getDeclaredFields()) {
                        if (java.util.Collection.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            java.util.Collection<WoodType> col = (java.util.Collection<WoodType>) field.get(null);
                            if (col != null && col.contains(WoodType.OAK)) {
                                col.add(type);
                                return type;
                            }
                        }
                    }
                } catch (Exception e3) {}
            }
            return type;
        }
    }

    public static final BlockSetType SAWIT_BLOCK_SET = registerBlockSetType(new BlockSetType("sawit"));
    public static final WoodType SAWIT_WOOD_TYPE = registerWoodType(new WoodType("sawit", SAWIT_BLOCK_SET));

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ExampleMod.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> SAWIT_BLOCK = BLOCKS.register("sawit", () ->
            new SawitBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().strength(2.0f, 2.0f).sound(SoundType.CROP)));

    public static final RegistrySupplier<Block> SAWIT_TRUNK = BLOCKS.register("sawit_trunk", () ->
            new com.sawit.kotaklegend.block.SawitTrunkBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistrySupplier<Block> SAWIT_TRUNK_DUMMY = BLOCKS.register("sawit_trunk_dummy", () ->
            new com.sawit.kotaklegend.block.SawitTrunkDummyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion().noLootTable()));

    public static final RegistrySupplier<Block> SAWIT_LOG = BLOCKS.register("sawit_log", () ->
            new com.sawit.kotaklegend.block.StrippableLogBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD), () -> ModBlocks.STRIPPED_SAWIT_LOG.get()));

    public static final RegistrySupplier<Block> SAWIT_WOOD = BLOCKS.register("sawit_wood", () ->
            new com.sawit.kotaklegend.block.StrippableLogBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD), () -> ModBlocks.STRIPPED_SAWIT_WOOD.get()));

    public static final RegistrySupplier<Block> STRIPPED_SAWIT_LOG = BLOCKS.register("stripped_sawit_log", () ->
            new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD)));

    public static final RegistrySupplier<Block> STRIPPED_SAWIT_WOOD = BLOCKS.register("stripped_sawit_wood", () ->
            new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD)));

    public static final RegistrySupplier<Block> SAWIT_PLANKS = BLOCKS.register("sawit_planks", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 3.0f).sound(SoundType.WOOD)));

    public static final RegistrySupplier<Block> SAWIT_LEAVES_DUMMY = BLOCKS.register("sawit_leaves_dummy", () ->
            new Block(BlockBehaviour.Properties.of().noCollission().noOcclusion()));

    public static final RegistrySupplier<Block> SAWIT_LEAVES_FRUIT_DUMMY = BLOCKS.register("sawit_leaves_fruit_dummy", () ->
            new Block(BlockBehaviour.Properties.of().noCollission().noOcclusion()));

    public static final RegistrySupplier<Block> SAWIT_BAG = BLOCKS.register("sawit_bag", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(1.5f).sound(SoundType.WOOL)));

    public static final RegistrySupplier<Block> SAWIT_STAIRS = BLOCKS.register("sawit_stairs", () ->
            new net.minecraft.world.level.block.StairBlock(Blocks.OAK_PLANKS.defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));

    public static final RegistrySupplier<Block> SAWIT_SLAB = BLOCKS.register("sawit_slab", () ->
            new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));

    public static final RegistrySupplier<Block> SAWIT_FENCE = BLOCKS.register("sawit_fence", () ->
            new net.minecraft.world.level.block.FenceBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));

    public static final RegistrySupplier<Block> SAWIT_FENCE_GATE = BLOCKS.register("sawit_fence_gate", () ->
            new net.minecraft.world.level.block.FenceGateBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS), net.minecraft.world.level.block.state.properties.WoodType.OAK));

    public static final RegistrySupplier<Block> SAWIT_DOOR = BLOCKS.register("sawit_door", () ->
            new net.minecraft.world.level.block.DoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_DOOR), net.minecraft.world.level.block.state.properties.BlockSetType.OAK));

    public static final RegistrySupplier<Block> SAWIT_TRAPDOOR = BLOCKS.register("sawit_trapdoor", () ->
            new net.minecraft.world.level.block.TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_TRAPDOOR), net.minecraft.world.level.block.state.properties.BlockSetType.OAK));

    public static final RegistrySupplier<Block> SAWIT_BUTTON = BLOCKS.register("sawit_button", () ->
            new net.minecraft.world.level.block.ButtonBlock(BlockBehaviour.Properties.copy(Blocks.OAK_BUTTON), net.minecraft.world.level.block.state.properties.BlockSetType.OAK, 30, true));

    public static final RegistrySupplier<Block> SAWIT_PRESSURE_PLATE = BLOCKS.register("sawit_pressure_plate", () ->
            new net.minecraft.world.level.block.PressurePlateBlock(net.minecraft.world.level.block.PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.OAK_PRESSURE_PLATE), SAWIT_BLOCK_SET));

    public static final RegistrySupplier<Block> SAWIT_SIGN = BLOCKS.register("sawit_sign", () ->
            new com.sawit.kotaklegend.block.SawitStandingSignBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SIGN), SAWIT_WOOD_TYPE));

    public static final RegistrySupplier<Block> SAWIT_WALL_SIGN = BLOCKS.register("sawit_wall_sign", () ->
            new com.sawit.kotaklegend.block.SawitWallSignBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WALL_SIGN), SAWIT_WOOD_TYPE));

    public static final RegistrySupplier<Block> SAWIT_HANGING_SIGN = BLOCKS.register("sawit_hanging_sign", () ->
            new com.sawit.kotaklegend.block.SawitHangingSignBlock(BlockBehaviour.Properties.copy(Blocks.OAK_HANGING_SIGN), SAWIT_WOOD_TYPE));

    public static final RegistrySupplier<Block> SAWIT_WALL_HANGING_SIGN = BLOCKS.register("sawit_wall_hanging_sign", () ->
            new com.sawit.kotaklegend.block.SawitWallHangingSignBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WALL_HANGING_SIGN), SAWIT_WOOD_TYPE));


    public static void register() {
        BLOCKS.register();
    }
}
