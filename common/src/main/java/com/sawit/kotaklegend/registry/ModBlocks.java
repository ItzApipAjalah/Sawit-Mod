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

public class ModBlocks {
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

    public static void register() {
        BLOCKS.register();
    }
}
