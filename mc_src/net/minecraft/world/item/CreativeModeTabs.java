package net.minecraft.world.item;

import com.mojang.datafixers.util.Pair;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.InstrumentTags;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

public class CreativeModeTabs {
	private static final ResourceLocation INVENTORY_BACKGROUND = CreativeModeTab.createTextureLocation("inventory");
	private static final ResourceLocation SEARCH_BACKGROUND = CreativeModeTab.createTextureLocation("item_search");
	public static final ResourceKey<CreativeModeTab> BUILDING_BLOCKS = createKey("building_blocks");
	public static final ResourceKey<CreativeModeTab> COLORED_BLOCKS = createKey("colored_blocks");
	public static final ResourceKey<CreativeModeTab> NATURAL_BLOCKS = createKey("natural_blocks");
	public static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = createKey("functional_blocks");
	public static final ResourceKey<CreativeModeTab> REDSTONE_BLOCKS = createKey("redstone_blocks");
	public static final ResourceKey<CreativeModeTab> HOTBAR = createKey("hotbar");
	public static final ResourceKey<CreativeModeTab> SEARCH = createKey("search");
	public static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES = createKey("tools_and_utilities");
	public static final ResourceKey<CreativeModeTab> COMBAT = createKey("combat");
	public static final ResourceKey<CreativeModeTab> FOOD_AND_DRINKS = createKey("food_and_drinks");
	public static final ResourceKey<CreativeModeTab> INGREDIENTS = createKey("ingredients");
	public static final ResourceKey<CreativeModeTab> SPAWN_EGGS = createKey("spawn_eggs");
	public static final ResourceKey<CreativeModeTab> OP_BLOCKS = createKey("op_blocks");
	public static final ResourceKey<CreativeModeTab> INVENTORY = createKey("inventory");
	private static final Comparator<Holder<PaintingVariant>> PAINTING_COMPARATOR = Comparator.comparing(
		Holder::value, Comparator.comparingInt(PaintingVariant::area).thenComparing(PaintingVariant::width)
	);
	@Nullable
	private static CreativeModeTab.ItemDisplayParameters CACHED_PARAMETERS;

	private static ResourceKey<CreativeModeTab> createKey(String string) {
		return ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace(string));
	}

	public static CreativeModeTab bootstrap(Registry<CreativeModeTab> arg) {
		Registry.register(
			arg,
			BUILDING_BLOCKS,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
				.title(Component.translatable("itemGroup.buildingBlocks"))
				.icon(() -> new ItemStack(Blocks.BRICKS))
				.displayItems((CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
					arg2.accept(Items.OAK_LOG);
					arg2.accept(Items.OAK_WOOD);
					arg2.accept(Items.STRIPPED_OAK_LOG);
					arg2.accept(Items.STRIPPED_OAK_WOOD);
					arg2.accept(Items.OAK_PLANKS);
					arg2.accept(Items.OAK_STAIRS);
					arg2.accept(Items.OAK_SLAB);
					arg2.accept(Items.OAK_FENCE);
					arg2.accept(Items.OAK_FENCE_GATE);
					arg2.accept(Items.OAK_DOOR);
					arg2.accept(Items.OAK_TRAPDOOR);
					arg2.accept(Items.OAK_PRESSURE_PLATE);
					arg2.accept(Items.OAK_BUTTON);
					arg2.accept(Items.SPRUCE_LOG);
					arg2.accept(Items.SPRUCE_WOOD);
					arg2.accept(Items.STRIPPED_SPRUCE_LOG);
					arg2.accept(Items.STRIPPED_SPRUCE_WOOD);
					arg2.accept(Items.SPRUCE_PLANKS);
					arg2.accept(Items.SPRUCE_STAIRS);
					arg2.accept(Items.SPRUCE_SLAB);
					arg2.accept(Items.SPRUCE_FENCE);
					arg2.accept(Items.SPRUCE_FENCE_GATE);
					arg2.accept(Items.SPRUCE_DOOR);
					arg2.accept(Items.SPRUCE_TRAPDOOR);
					arg2.accept(Items.SPRUCE_PRESSURE_PLATE);
					arg2.accept(Items.SPRUCE_BUTTON);
					arg2.accept(Items.BIRCH_LOG);
					arg2.accept(Items.BIRCH_WOOD);
					arg2.accept(Items.STRIPPED_BIRCH_LOG);
					arg2.accept(Items.STRIPPED_BIRCH_WOOD);
					arg2.accept(Items.BIRCH_PLANKS);
					arg2.accept(Items.BIRCH_STAIRS);
					arg2.accept(Items.BIRCH_SLAB);
					arg2.accept(Items.BIRCH_FENCE);
					arg2.accept(Items.BIRCH_FENCE_GATE);
					arg2.accept(Items.BIRCH_DOOR);
					arg2.accept(Items.BIRCH_TRAPDOOR);
					arg2.accept(Items.BIRCH_PRESSURE_PLATE);
					arg2.accept(Items.BIRCH_BUTTON);
					arg2.accept(Items.JUNGLE_LOG);
					arg2.accept(Items.JUNGLE_WOOD);
					arg2.accept(Items.STRIPPED_JUNGLE_LOG);
					arg2.accept(Items.STRIPPED_JUNGLE_WOOD);
					arg2.accept(Items.JUNGLE_PLANKS);
					arg2.accept(Items.JUNGLE_STAIRS);
					arg2.accept(Items.JUNGLE_SLAB);
					arg2.accept(Items.JUNGLE_FENCE);
					arg2.accept(Items.JUNGLE_FENCE_GATE);
					arg2.accept(Items.JUNGLE_DOOR);
					arg2.accept(Items.JUNGLE_TRAPDOOR);
					arg2.accept(Items.JUNGLE_PRESSURE_PLATE);
					arg2.accept(Items.JUNGLE_BUTTON);
					arg2.accept(Items.ACACIA_LOG);
					arg2.accept(Items.ACACIA_WOOD);
					arg2.accept(Items.STRIPPED_ACACIA_LOG);
					arg2.accept(Items.STRIPPED_ACACIA_WOOD);
					arg2.accept(Items.ACACIA_PLANKS);
					arg2.accept(Items.ACACIA_STAIRS);
					arg2.accept(Items.ACACIA_SLAB);
					arg2.accept(Items.ACACIA_FENCE);
					arg2.accept(Items.ACACIA_FENCE_GATE);
					arg2.accept(Items.ACACIA_DOOR);
					arg2.accept(Items.ACACIA_TRAPDOOR);
					arg2.accept(Items.ACACIA_PRESSURE_PLATE);
					arg2.accept(Items.ACACIA_BUTTON);
					arg2.accept(Items.DARK_OAK_LOG);
					arg2.accept(Items.DARK_OAK_WOOD);
					arg2.accept(Items.STRIPPED_DARK_OAK_LOG);
					arg2.accept(Items.STRIPPED_DARK_OAK_WOOD);
					arg2.accept(Items.DARK_OAK_PLANKS);
					arg2.accept(Items.DARK_OAK_STAIRS);
					arg2.accept(Items.DARK_OAK_SLAB);
					arg2.accept(Items.DARK_OAK_FENCE);
					arg2.accept(Items.DARK_OAK_FENCE_GATE);
					arg2.accept(Items.DARK_OAK_DOOR);
					arg2.accept(Items.DARK_OAK_TRAPDOOR);
					arg2.accept(Items.DARK_OAK_PRESSURE_PLATE);
					arg2.accept(Items.DARK_OAK_BUTTON);
					arg2.accept(Items.MANGROVE_LOG);
					arg2.accept(Items.MANGROVE_WOOD);
					arg2.accept(Items.STRIPPED_MANGROVE_LOG);
					arg2.accept(Items.STRIPPED_MANGROVE_WOOD);
					arg2.accept(Items.MANGROVE_PLANKS);
					arg2.accept(Items.MANGROVE_STAIRS);
					arg2.accept(Items.MANGROVE_SLAB);
					arg2.accept(Items.MANGROVE_FENCE);
					arg2.accept(Items.MANGROVE_FENCE_GATE);
					arg2.accept(Items.MANGROVE_DOOR);
					arg2.accept(Items.MANGROVE_TRAPDOOR);
					arg2.accept(Items.MANGROVE_PRESSURE_PLATE);
					arg2.accept(Items.MANGROVE_BUTTON);
					arg2.accept(Items.CHERRY_LOG);
					arg2.accept(Items.CHERRY_WOOD);
					arg2.accept(Items.STRIPPED_CHERRY_LOG);
					arg2.accept(Items.STRIPPED_CHERRY_WOOD);
					arg2.accept(Items.CHERRY_PLANKS);
					arg2.accept(Items.CHERRY_STAIRS);
					arg2.accept(Items.CHERRY_SLAB);
					arg2.accept(Items.CHERRY_FENCE);
					arg2.accept(Items.CHERRY_FENCE_GATE);
					arg2.accept(Items.CHERRY_DOOR);
					arg2.accept(Items.CHERRY_TRAPDOOR);
					arg2.accept(Items.CHERRY_PRESSURE_PLATE);
					arg2.accept(Items.CHERRY_BUTTON);
					arg2.accept(Items.PALE_OAK_LOG);
					arg2.accept(Items.PALE_OAK_WOOD);
					arg2.accept(Items.STRIPPED_PALE_OAK_LOG);
					arg2.accept(Items.STRIPPED_PALE_OAK_WOOD);
					arg2.accept(Items.PALE_OAK_PLANKS);
					arg2.accept(Items.PALE_OAK_STAIRS);
					arg2.accept(Items.PALE_OAK_SLAB);
					arg2.accept(Items.PALE_OAK_FENCE);
					arg2.accept(Items.PALE_OAK_FENCE_GATE);
					arg2.accept(Items.PALE_OAK_DOOR);
					arg2.accept(Items.PALE_OAK_TRAPDOOR);
					arg2.accept(Items.PALE_OAK_PRESSURE_PLATE);
					arg2.accept(Items.PALE_OAK_BUTTON);
					arg2.accept(Items.BAMBOO_BLOCK);
					arg2.accept(Items.STRIPPED_BAMBOO_BLOCK);
					arg2.accept(Items.BAMBOO_PLANKS);
					arg2.accept(Items.BAMBOO_MOSAIC);
					arg2.accept(Items.BAMBOO_STAIRS);
					arg2.accept(Items.BAMBOO_MOSAIC_STAIRS);
					arg2.accept(Items.BAMBOO_SLAB);
					arg2.accept(Items.BAMBOO_MOSAIC_SLAB);
					arg2.accept(Items.BAMBOO_FENCE);
					arg2.accept(Items.BAMBOO_FENCE_GATE);
					arg2.accept(Items.BAMBOO_DOOR);
					arg2.accept(Items.BAMBOO_TRAPDOOR);
					arg2.accept(Items.BAMBOO_PRESSURE_PLATE);
					arg2.accept(Items.BAMBOO_BUTTON);
					arg2.accept(Items.CRIMSON_STEM);
					arg2.accept(Items.CRIMSON_HYPHAE);
					arg2.accept(Items.STRIPPED_CRIMSON_STEM);
					arg2.accept(Items.STRIPPED_CRIMSON_HYPHAE);
					arg2.accept(Items.CRIMSON_PLANKS);
					arg2.accept(Items.CRIMSON_STAIRS);
					arg2.accept(Items.CRIMSON_SLAB);
					arg2.accept(Items.CRIMSON_FENCE);
					arg2.accept(Items.CRIMSON_FENCE_GATE);
					arg2.accept(Items.CRIMSON_DOOR);
					arg2.accept(Items.CRIMSON_TRAPDOOR);
					arg2.accept(Items.CRIMSON_PRESSURE_PLATE);
					arg2.accept(Items.CRIMSON_BUTTON);
					arg2.accept(Items.WARPED_STEM);
					arg2.accept(Items.WARPED_HYPHAE);
					arg2.accept(Items.STRIPPED_WARPED_STEM);
					arg2.accept(Items.STRIPPED_WARPED_HYPHAE);
					arg2.accept(Items.WARPED_PLANKS);
					arg2.accept(Items.WARPED_STAIRS);
					arg2.accept(Items.WARPED_SLAB);
					arg2.accept(Items.WARPED_FENCE);
					arg2.accept(Items.WARPED_FENCE_GATE);
					arg2.accept(Items.WARPED_DOOR);
					arg2.accept(Items.WARPED_TRAPDOOR);
					arg2.accept(Items.WARPED_PRESSURE_PLATE);
					arg2.accept(Items.WARPED_BUTTON);
					arg2.accept(Items.STONE);
					arg2.accept(Items.STONE_STAIRS);
					arg2.accept(Items.STONE_SLAB);
					arg2.accept(Items.STONE_PRESSURE_PLATE);
					arg2.accept(Items.STONE_BUTTON);
					arg2.accept(Items.COBBLESTONE);
					arg2.accept(Items.COBBLESTONE_STAIRS);
					arg2.accept(Items.COBBLESTONE_SLAB);
					arg2.accept(Items.COBBLESTONE_WALL);
					arg2.accept(Items.MOSSY_COBBLESTONE);
					arg2.accept(Items.MOSSY_COBBLESTONE_STAIRS);
					arg2.accept(Items.MOSSY_COBBLESTONE_SLAB);
					arg2.accept(Items.MOSSY_COBBLESTONE_WALL);
					arg2.accept(Items.SMOOTH_STONE);
					arg2.accept(Items.SMOOTH_STONE_SLAB);
					arg2.accept(Items.STONE_BRICKS);
					arg2.accept(Items.CRACKED_STONE_BRICKS);
					arg2.accept(Items.STONE_BRICK_STAIRS);
					arg2.accept(Items.STONE_BRICK_SLAB);
					arg2.accept(Items.STONE_BRICK_WALL);
					arg2.accept(Items.CHISELED_STONE_BRICKS);
					arg2.accept(Items.MOSSY_STONE_BRICKS);
					arg2.accept(Items.MOSSY_STONE_BRICK_STAIRS);
					arg2.accept(Items.MOSSY_STONE_BRICK_SLAB);
					arg2.accept(Items.MOSSY_STONE_BRICK_WALL);
					arg2.accept(Items.GRANITE);
					arg2.accept(Items.GRANITE_STAIRS);
					arg2.accept(Items.GRANITE_SLAB);
					arg2.accept(Items.GRANITE_WALL);
					arg2.accept(Items.POLISHED_GRANITE);
					arg2.accept(Items.POLISHED_GRANITE_STAIRS);
					arg2.accept(Items.POLISHED_GRANITE_SLAB);
					arg2.accept(Items.DIORITE);
					arg2.accept(Items.DIORITE_STAIRS);
					arg2.accept(Items.DIORITE_SLAB);
					arg2.accept(Items.DIORITE_WALL);
					arg2.accept(Items.POLISHED_DIORITE);
					arg2.accept(Items.POLISHED_DIORITE_STAIRS);
					arg2.accept(Items.POLISHED_DIORITE_SLAB);
					arg2.accept(Items.ANDESITE);
					arg2.accept(Items.ANDESITE_STAIRS);
					arg2.accept(Items.ANDESITE_SLAB);
					arg2.accept(Items.ANDESITE_WALL);
					arg2.accept(Items.POLISHED_ANDESITE);
					arg2.accept(Items.POLISHED_ANDESITE_STAIRS);
					arg2.accept(Items.POLISHED_ANDESITE_SLAB);
					arg2.accept(Items.DEEPSLATE);
					arg2.accept(Items.COBBLED_DEEPSLATE);
					arg2.accept(Items.COBBLED_DEEPSLATE_STAIRS);
					arg2.accept(Items.COBBLED_DEEPSLATE_SLAB);
					arg2.accept(Items.COBBLED_DEEPSLATE_WALL);
					arg2.accept(Items.CHISELED_DEEPSLATE);
					arg2.accept(Items.POLISHED_DEEPSLATE);
					arg2.accept(Items.POLISHED_DEEPSLATE_STAIRS);
					arg2.accept(Items.POLISHED_DEEPSLATE_SLAB);
					arg2.accept(Items.POLISHED_DEEPSLATE_WALL);
					arg2.accept(Items.DEEPSLATE_BRICKS);
					arg2.accept(Items.CRACKED_DEEPSLATE_BRICKS);
					arg2.accept(Items.DEEPSLATE_BRICK_STAIRS);
					arg2.accept(Items.DEEPSLATE_BRICK_SLAB);
					arg2.accept(Items.DEEPSLATE_BRICK_WALL);
					arg2.accept(Items.DEEPSLATE_TILES);
					arg2.accept(Items.CRACKED_DEEPSLATE_TILES);
					arg2.accept(Items.DEEPSLATE_TILE_STAIRS);
					arg2.accept(Items.DEEPSLATE_TILE_SLAB);
					arg2.accept(Items.DEEPSLATE_TILE_WALL);
					arg2.accept(Items.REINFORCED_DEEPSLATE);
					arg2.accept(Items.TUFF);
					arg2.accept(Items.TUFF_STAIRS);
					arg2.accept(Items.TUFF_SLAB);
					arg2.accept(Items.TUFF_WALL);
					arg2.accept(Items.CHISELED_TUFF);
					arg2.accept(Items.POLISHED_TUFF);
					arg2.accept(Items.POLISHED_TUFF_STAIRS);
					arg2.accept(Items.POLISHED_TUFF_SLAB);
					arg2.accept(Items.POLISHED_TUFF_WALL);
					arg2.accept(Items.TUFF_BRICKS);
					arg2.accept(Items.TUFF_BRICK_STAIRS);
					arg2.accept(Items.TUFF_BRICK_SLAB);
					arg2.accept(Items.TUFF_BRICK_WALL);
					arg2.accept(Items.CHISELED_TUFF_BRICKS);
					arg2.accept(Items.BRICKS);
					arg2.accept(Items.BRICK_STAIRS);
					arg2.accept(Items.BRICK_SLAB);
					arg2.accept(Items.BRICK_WALL);
					arg2.accept(Items.PACKED_MUD);
					arg2.accept(Items.MUD_BRICKS);
					arg2.accept(Items.MUD_BRICK_STAIRS);
					arg2.accept(Items.MUD_BRICK_SLAB);
					arg2.accept(Items.MUD_BRICK_WALL);
					arg2.accept(Items.SANDSTONE);
					arg2.accept(Items.SANDSTONE_STAIRS);
					arg2.accept(Items.SANDSTONE_SLAB);
					arg2.accept(Items.SANDSTONE_WALL);
					arg2.accept(Items.CHISELED_SANDSTONE);
					arg2.accept(Items.SMOOTH_SANDSTONE);
					arg2.accept(Items.SMOOTH_SANDSTONE_STAIRS);
					arg2.accept(Items.SMOOTH_SANDSTONE_SLAB);
					arg2.accept(Items.CUT_SANDSTONE);
					arg2.accept(Items.CUT_STANDSTONE_SLAB);
					arg2.accept(Items.RED_SANDSTONE);
					arg2.accept(Items.RED_SANDSTONE_STAIRS);
					arg2.accept(Items.RED_SANDSTONE_SLAB);
					arg2.accept(Items.RED_SANDSTONE_WALL);
					arg2.accept(Items.CHISELED_RED_SANDSTONE);
					arg2.accept(Items.SMOOTH_RED_SANDSTONE);
					arg2.accept(Items.SMOOTH_RED_SANDSTONE_STAIRS);
					arg2.accept(Items.SMOOTH_RED_SANDSTONE_SLAB);
					arg2.accept(Items.CUT_RED_SANDSTONE);
					arg2.accept(Items.CUT_RED_SANDSTONE_SLAB);
					arg2.accept(Items.SEA_LANTERN);
					arg2.accept(Items.PRISMARINE);
					arg2.accept(Items.PRISMARINE_STAIRS);
					arg2.accept(Items.PRISMARINE_SLAB);
					arg2.accept(Items.PRISMARINE_WALL);
					arg2.accept(Items.PRISMARINE_BRICKS);
					arg2.accept(Items.PRISMARINE_BRICK_STAIRS);
					arg2.accept(Items.PRISMARINE_BRICK_SLAB);
					arg2.accept(Items.DARK_PRISMARINE);
					arg2.accept(Items.DARK_PRISMARINE_STAIRS);
					arg2.accept(Items.DARK_PRISMARINE_SLAB);
					arg2.accept(Items.NETHERRACK);
					arg2.accept(Items.NETHER_BRICKS);
					arg2.accept(Items.CRACKED_NETHER_BRICKS);
					arg2.accept(Items.NETHER_BRICK_STAIRS);
					arg2.accept(Items.NETHER_BRICK_SLAB);
					arg2.accept(Items.NETHER_BRICK_WALL);
					arg2.accept(Items.NETHER_BRICK_FENCE);
					arg2.accept(Items.CHISELED_NETHER_BRICKS);
					arg2.accept(Items.RED_NETHER_BRICKS);
					arg2.accept(Items.RED_NETHER_BRICK_STAIRS);
					arg2.accept(Items.RED_NETHER_BRICK_SLAB);
					arg2.accept(Items.RED_NETHER_BRICK_WALL);
					arg2.accept(Items.BASALT);
					arg2.accept(Items.SMOOTH_BASALT);
					arg2.accept(Items.POLISHED_BASALT);
					arg2.accept(Items.BLACKSTONE);
					arg2.accept(Items.GILDED_BLACKSTONE);
					arg2.accept(Items.BLACKSTONE_STAIRS);
					arg2.accept(Items.BLACKSTONE_SLAB);
					arg2.accept(Items.BLACKSTONE_WALL);
					arg2.accept(Items.CHISELED_POLISHED_BLACKSTONE);
					arg2.accept(Items.POLISHED_BLACKSTONE);
					arg2.accept(Items.POLISHED_BLACKSTONE_STAIRS);
					arg2.accept(Items.POLISHED_BLACKSTONE_SLAB);
					arg2.accept(Items.POLISHED_BLACKSTONE_WALL);
					arg2.accept(Items.POLISHED_BLACKSTONE_PRESSURE_PLATE);
					arg2.accept(Items.POLISHED_BLACKSTONE_BUTTON);
					arg2.accept(Items.POLISHED_BLACKSTONE_BRICKS);
					arg2.accept(Items.CRACKED_POLISHED_BLACKSTONE_BRICKS);
					arg2.accept(Items.POLISHED_BLACKSTONE_BRICK_STAIRS);
					arg2.accept(Items.POLISHED_BLACKSTONE_BRICK_SLAB);
					arg2.accept(Items.POLISHED_BLACKSTONE_BRICK_WALL);
					arg2.accept(Items.END_STONE);
					arg2.accept(Items.END_STONE_BRICKS);
					arg2.accept(Items.END_STONE_BRICK_STAIRS);
					arg2.accept(Items.END_STONE_BRICK_SLAB);
					arg2.accept(Items.END_STONE_BRICK_WALL);
					arg2.accept(Items.PURPUR_BLOCK);
					arg2.accept(Items.PURPUR_PILLAR);
					arg2.accept(Items.PURPUR_STAIRS);
					arg2.accept(Items.PURPUR_SLAB);
					arg2.accept(Items.COAL_BLOCK);
					arg2.accept(Items.IRON_BLOCK);
					arg2.accept(Items.IRON_BARS);
					arg2.accept(Items.IRON_DOOR);
					arg2.accept(Items.IRON_TRAPDOOR);
					arg2.accept(Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
					arg2.accept(Items.CHAIN);
					arg2.accept(Items.GOLD_BLOCK);
					arg2.accept(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
					arg2.accept(Items.REDSTONE_BLOCK);
					arg2.accept(Items.EMERALD_BLOCK);
					arg2.accept(Items.LAPIS_BLOCK);
					arg2.accept(Items.DIAMOND_BLOCK);
					arg2.accept(Items.NETHERITE_BLOCK);
					arg2.accept(Items.QUARTZ_BLOCK);
					arg2.accept(Items.QUARTZ_STAIRS);
					arg2.accept(Items.QUARTZ_SLAB);
					arg2.accept(Items.CHISELED_QUARTZ_BLOCK);
					arg2.accept(Items.QUARTZ_BRICKS);
					arg2.accept(Items.QUARTZ_PILLAR);
					arg2.accept(Items.SMOOTH_QUARTZ);
					arg2.accept(Items.SMOOTH_QUARTZ_STAIRS);
					arg2.accept(Items.SMOOTH_QUARTZ_SLAB);
					arg2.accept(Items.AMETHYST_BLOCK);
					arg2.accept(Items.COPPER_BLOCK);
					arg2.accept(Items.CHISELED_COPPER);
					arg2.accept(Items.COPPER_GRATE);
					arg2.accept(Items.CUT_COPPER);
					arg2.accept(Items.CUT_COPPER_STAIRS);
					arg2.accept(Items.CUT_COPPER_SLAB);
					arg2.accept(Items.COPPER_DOOR);
					arg2.accept(Items.COPPER_TRAPDOOR);
					arg2.accept(Items.COPPER_BULB);
					arg2.accept(Items.EXPOSED_COPPER);
					arg2.accept(Items.EXPOSED_CHISELED_COPPER);
					arg2.accept(Items.EXPOSED_COPPER_GRATE);
					arg2.accept(Items.EXPOSED_CUT_COPPER);
					arg2.accept(Items.EXPOSED_CUT_COPPER_STAIRS);
					arg2.accept(Items.EXPOSED_CUT_COPPER_SLAB);
					arg2.accept(Items.EXPOSED_COPPER_DOOR);
					arg2.accept(Items.EXPOSED_COPPER_TRAPDOOR);
					arg2.accept(Items.EXPOSED_COPPER_BULB);
					arg2.accept(Items.WEATHERED_COPPER);
					arg2.accept(Items.WEATHERED_CHISELED_COPPER);
					arg2.accept(Items.WEATHERED_COPPER_GRATE);
					arg2.accept(Items.WEATHERED_CUT_COPPER);
					arg2.accept(Items.WEATHERED_CUT_COPPER_STAIRS);
					arg2.accept(Items.WEATHERED_CUT_COPPER_SLAB);
					arg2.accept(Items.WEATHERED_COPPER_DOOR);
					arg2.accept(Items.WEATHERED_COPPER_TRAPDOOR);
					arg2.accept(Items.WEATHERED_COPPER_BULB);
					arg2.accept(Items.OXIDIZED_COPPER);
					arg2.accept(Items.OXIDIZED_CHISELED_COPPER);
					arg2.accept(Items.OXIDIZED_COPPER_GRATE);
					arg2.accept(Items.OXIDIZED_CUT_COPPER);
					arg2.accept(Items.OXIDIZED_CUT_COPPER_STAIRS);
					arg2.accept(Items.OXIDIZED_CUT_COPPER_SLAB);
					arg2.accept(Items.OXIDIZED_COPPER_DOOR);
					arg2.accept(Items.OXIDIZED_COPPER_TRAPDOOR);
					arg2.accept(Items.OXIDIZED_COPPER_BULB);
					arg2.accept(Items.WAXED_COPPER_BLOCK);
					arg2.accept(Items.WAXED_CHISELED_COPPER);
					arg2.accept(Items.WAXED_COPPER_GRATE);
					arg2.accept(Items.WAXED_CUT_COPPER);
					arg2.accept(Items.WAXED_CUT_COPPER_STAIRS);
					arg2.accept(Items.WAXED_CUT_COPPER_SLAB);
					arg2.accept(Items.WAXED_COPPER_DOOR);
					arg2.accept(Items.WAXED_COPPER_TRAPDOOR);
					arg2.accept(Items.WAXED_COPPER_BULB);
					arg2.accept(Items.WAXED_EXPOSED_COPPER);
					arg2.accept(Items.WAXED_EXPOSED_CHISELED_COPPER);
					arg2.accept(Items.WAXED_EXPOSED_COPPER_GRATE);
					arg2.accept(Items.WAXED_EXPOSED_CUT_COPPER);
					arg2.accept(Items.WAXED_EXPOSED_CUT_COPPER_STAIRS);
					arg2.accept(Items.WAXED_EXPOSED_CUT_COPPER_SLAB);
					arg2.accept(Items.WAXED_EXPOSED_COPPER_DOOR);
					arg2.accept(Items.WAXED_EXPOSED_COPPER_TRAPDOOR);
					arg2.accept(Items.WAXED_EXPOSED_COPPER_BULB);
					arg2.accept(Items.WAXED_WEATHERED_COPPER);
					arg2.accept(Items.WAXED_WEATHERED_CHISELED_COPPER);
					arg2.accept(Items.WAXED_WEATHERED_COPPER_GRATE);
					arg2.accept(Items.WAXED_WEATHERED_CUT_COPPER);
					arg2.accept(Items.WAXED_WEATHERED_CUT_COPPER_STAIRS);
					arg2.accept(Items.WAXED_WEATHERED_CUT_COPPER_SLAB);
					arg2.accept(Items.WAXED_WEATHERED_COPPER_DOOR);
					arg2.accept(Items.WAXED_WEATHERED_COPPER_TRAPDOOR);
					arg2.accept(Items.WAXED_WEATHERED_COPPER_BULB);
					arg2.accept(Items.WAXED_OXIDIZED_COPPER);
					arg2.accept(Items.WAXED_OXIDIZED_CHISELED_COPPER);
					arg2.accept(Items.WAXED_OXIDIZED_COPPER_GRATE);
					arg2.accept(Items.WAXED_OXIDIZED_CUT_COPPER);
					arg2.accept(Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS);
					arg2.accept(Items.WAXED_OXIDIZED_CUT_COPPER_SLAB);
					arg2.accept(Items.WAXED_OXIDIZED_COPPER_DOOR);
					arg2.accept(Items.WAXED_OXIDIZED_COPPER_TRAPDOOR);
					arg2.accept(Items.WAXED_OXIDIZED_COPPER_BULB);
				}))
				.build()
		);
		Registry.register(
			arg,
			COLORED_BLOCKS,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
				.title(Component.translatable("itemGroup.coloredBlocks"))
				.icon(() -> new ItemStack(Blocks.CYAN_WOOL))
				.displayItems((CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
					arg2.accept(Items.WHITE_WOOL);
					arg2.accept(Items.LIGHT_GRAY_WOOL);
					arg2.accept(Items.GRAY_WOOL);
					arg2.accept(Items.BLACK_WOOL);
					arg2.accept(Items.BROWN_WOOL);
					arg2.accept(Items.RED_WOOL);
					arg2.accept(Items.ORANGE_WOOL);
					arg2.accept(Items.YELLOW_WOOL);
					arg2.accept(Items.LIME_WOOL);
					arg2.accept(Items.GREEN_WOOL);
					arg2.accept(Items.CYAN_WOOL);
					arg2.accept(Items.LIGHT_BLUE_WOOL);
					arg2.accept(Items.BLUE_WOOL);
					arg2.accept(Items.PURPLE_WOOL);
					arg2.accept(Items.MAGENTA_WOOL);
					arg2.accept(Items.PINK_WOOL);
					arg2.accept(Items.WHITE_CARPET);
					arg2.accept(Items.LIGHT_GRAY_CARPET);
					arg2.accept(Items.GRAY_CARPET);
					arg2.accept(Items.BLACK_CARPET);
					arg2.accept(Items.BROWN_CARPET);
					arg2.accept(Items.RED_CARPET);
					arg2.accept(Items.ORANGE_CARPET);
					arg2.accept(Items.YELLOW_CARPET);
					arg2.accept(Items.LIME_CARPET);
					arg2.accept(Items.GREEN_CARPET);
					arg2.accept(Items.CYAN_CARPET);
					arg2.accept(Items.LIGHT_BLUE_CARPET);
					arg2.accept(Items.BLUE_CARPET);
					arg2.accept(Items.PURPLE_CARPET);
					arg2.accept(Items.MAGENTA_CARPET);
					arg2.accept(Items.PINK_CARPET);
					arg2.accept(Items.TERRACOTTA);
					arg2.accept(Items.WHITE_TERRACOTTA);
					arg2.accept(Items.LIGHT_GRAY_TERRACOTTA);
					arg2.accept(Items.GRAY_TERRACOTTA);
					arg2.accept(Items.BLACK_TERRACOTTA);
					arg2.accept(Items.BROWN_TERRACOTTA);
					arg2.accept(Items.RED_TERRACOTTA);
					arg2.accept(Items.ORANGE_TERRACOTTA);
					arg2.accept(Items.YELLOW_TERRACOTTA);
					arg2.accept(Items.LIME_TERRACOTTA);
					arg2.accept(Items.GREEN_TERRACOTTA);
					arg2.accept(Items.CYAN_TERRACOTTA);
					arg2.accept(Items.LIGHT_BLUE_TERRACOTTA);
					arg2.accept(Items.BLUE_TERRACOTTA);
					arg2.accept(Items.PURPLE_TERRACOTTA);
					arg2.accept(Items.MAGENTA_TERRACOTTA);
					arg2.accept(Items.PINK_TERRACOTTA);
					arg2.accept(Items.WHITE_CONCRETE);
					arg2.accept(Items.LIGHT_GRAY_CONCRETE);
					arg2.accept(Items.GRAY_CONCRETE);
					arg2.accept(Items.BLACK_CONCRETE);
					arg2.accept(Items.BROWN_CONCRETE);
					arg2.accept(Items.RED_CONCRETE);
					arg2.accept(Items.ORANGE_CONCRETE);
					arg2.accept(Items.YELLOW_CONCRETE);
					arg2.accept(Items.LIME_CONCRETE);
					arg2.accept(Items.GREEN_CONCRETE);
					arg2.accept(Items.CYAN_CONCRETE);
					arg2.accept(Items.LIGHT_BLUE_CONCRETE);
					arg2.accept(Items.BLUE_CONCRETE);
					arg2.accept(Items.PURPLE_CONCRETE);
					arg2.accept(Items.MAGENTA_CONCRETE);
					arg2.accept(Items.PINK_CONCRETE);
					arg2.accept(Items.WHITE_CONCRETE_POWDER);
					arg2.accept(Items.LIGHT_GRAY_CONCRETE_POWDER);
					arg2.accept(Items.GRAY_CONCRETE_POWDER);
					arg2.accept(Items.BLACK_CONCRETE_POWDER);
					arg2.accept(Items.BROWN_CONCRETE_POWDER);
					arg2.accept(Items.RED_CONCRETE_POWDER);
					arg2.accept(Items.ORANGE_CONCRETE_POWDER);
					arg2.accept(Items.YELLOW_CONCRETE_POWDER);
					arg2.accept(Items.LIME_CONCRETE_POWDER);
					arg2.accept(Items.GREEN_CONCRETE_POWDER);
					arg2.accept(Items.CYAN_CONCRETE_POWDER);
					arg2.accept(Items.LIGHT_BLUE_CONCRETE_POWDER);
					arg2.accept(Items.BLUE_CONCRETE_POWDER);
					arg2.accept(Items.PURPLE_CONCRETE_POWDER);
					arg2.accept(Items.MAGENTA_CONCRETE_POWDER);
					arg2.accept(Items.PINK_CONCRETE_POWDER);
					arg2.accept(Items.WHITE_GLAZED_TERRACOTTA);
					arg2.accept(Items.LIGHT_GRAY_GLAZED_TERRACOTTA);
					arg2.accept(Items.GRAY_GLAZED_TERRACOTTA);
					arg2.accept(Items.BLACK_GLAZED_TERRACOTTA);
					arg2.accept(Items.BROWN_GLAZED_TERRACOTTA);
					arg2.accept(Items.RED_GLAZED_TERRACOTTA);
					arg2.accept(Items.ORANGE_GLAZED_TERRACOTTA);
					arg2.accept(Items.YELLOW_GLAZED_TERRACOTTA);
					arg2.accept(Items.LIME_GLAZED_TERRACOTTA);
					arg2.accept(Items.GREEN_GLAZED_TERRACOTTA);
					arg2.accept(Items.CYAN_GLAZED_TERRACOTTA);
					arg2.accept(Items.LIGHT_BLUE_GLAZED_TERRACOTTA);
					arg2.accept(Items.BLUE_GLAZED_TERRACOTTA);
					arg2.accept(Items.PURPLE_GLAZED_TERRACOTTA);
					arg2.accept(Items.MAGENTA_GLAZED_TERRACOTTA);
					arg2.accept(Items.PINK_GLAZED_TERRACOTTA);
					arg2.accept(Items.GLASS);
					arg2.accept(Items.TINTED_GLASS);
					arg2.accept(Items.WHITE_STAINED_GLASS);
					arg2.accept(Items.LIGHT_GRAY_STAINED_GLASS);
					arg2.accept(Items.GRAY_STAINED_GLASS);
					arg2.accept(Items.BLACK_STAINED_GLASS);
					arg2.accept(Items.BROWN_STAINED_GLASS);
					arg2.accept(Items.RED_STAINED_GLASS);
					arg2.accept(Items.ORANGE_STAINED_GLASS);
					arg2.accept(Items.YELLOW_STAINED_GLASS);
					arg2.accept(Items.LIME_STAINED_GLASS);
					arg2.accept(Items.GREEN_STAINED_GLASS);
					arg2.accept(Items.CYAN_STAINED_GLASS);
					arg2.accept(Items.LIGHT_BLUE_STAINED_GLASS);
					arg2.accept(Items.BLUE_STAINED_GLASS);
					arg2.accept(Items.PURPLE_STAINED_GLASS);
					arg2.accept(Items.MAGENTA_STAINED_GLASS);
					arg2.accept(Items.PINK_STAINED_GLASS);
					arg2.accept(Items.GLASS_PANE);
					arg2.accept(Items.WHITE_STAINED_GLASS_PANE);
					arg2.accept(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
					arg2.accept(Items.GRAY_STAINED_GLASS_PANE);
					arg2.accept(Items.BLACK_STAINED_GLASS_PANE);
					arg2.accept(Items.BROWN_STAINED_GLASS_PANE);
					arg2.accept(Items.RED_STAINED_GLASS_PANE);
					arg2.accept(Items.ORANGE_STAINED_GLASS_PANE);
					arg2.accept(Items.YELLOW_STAINED_GLASS_PANE);
					arg2.accept(Items.LIME_STAINED_GLASS_PANE);
					arg2.accept(Items.GREEN_STAINED_GLASS_PANE);
					arg2.accept(Items.CYAN_STAINED_GLASS_PANE);
					arg2.accept(Items.LIGHT_BLUE_STAINED_GLASS_PANE);
					arg2.accept(Items.BLUE_STAINED_GLASS_PANE);
					arg2.accept(Items.PURPLE_STAINED_GLASS_PANE);
					arg2.accept(Items.MAGENTA_STAINED_GLASS_PANE);
					arg2.accept(Items.PINK_STAINED_GLASS_PANE);
					arg2.accept(Items.SHULKER_BOX);
					arg2.accept(Items.WHITE_SHULKER_BOX);
					arg2.accept(Items.LIGHT_GRAY_SHULKER_BOX);
					arg2.accept(Items.GRAY_SHULKER_BOX);
					arg2.accept(Items.BLACK_SHULKER_BOX);
					arg2.accept(Items.BROWN_SHULKER_BOX);
					arg2.accept(Items.RED_SHULKER_BOX);
					arg2.accept(Items.ORANGE_SHULKER_BOX);
					arg2.accept(Items.YELLOW_SHULKER_BOX);
					arg2.accept(Items.LIME_SHULKER_BOX);
					arg2.accept(Items.GREEN_SHULKER_BOX);
					arg2.accept(Items.CYAN_SHULKER_BOX);
					arg2.accept(Items.LIGHT_BLUE_SHULKER_BOX);
					arg2.accept(Items.BLUE_SHULKER_BOX);
					arg2.accept(Items.PURPLE_SHULKER_BOX);
					arg2.accept(Items.MAGENTA_SHULKER_BOX);
					arg2.accept(Items.PINK_SHULKER_BOX);
					arg2.accept(Items.WHITE_BED);
					arg2.accept(Items.LIGHT_GRAY_BED);
					arg2.accept(Items.GRAY_BED);
					arg2.accept(Items.BLACK_BED);
					arg2.accept(Items.BROWN_BED);
					arg2.accept(Items.RED_BED);
					arg2.accept(Items.ORANGE_BED);
					arg2.accept(Items.YELLOW_BED);
					arg2.accept(Items.LIME_BED);
					arg2.accept(Items.GREEN_BED);
					arg2.accept(Items.CYAN_BED);
					arg2.accept(Items.LIGHT_BLUE_BED);
					arg2.accept(Items.BLUE_BED);
					arg2.accept(Items.PURPLE_BED);
					arg2.accept(Items.MAGENTA_BED);
					arg2.accept(Items.PINK_BED);
					arg2.accept(Items.CANDLE);
					arg2.accept(Items.WHITE_CANDLE);
					arg2.accept(Items.LIGHT_GRAY_CANDLE);
					arg2.accept(Items.GRAY_CANDLE);
					arg2.accept(Items.BLACK_CANDLE);
					arg2.accept(Items.BROWN_CANDLE);
					arg2.accept(Items.RED_CANDLE);
					arg2.accept(Items.ORANGE_CANDLE);
					arg2.accept(Items.YELLOW_CANDLE);
					arg2.accept(Items.LIME_CANDLE);
					arg2.accept(Items.GREEN_CANDLE);
					arg2.accept(Items.CYAN_CANDLE);
					arg2.accept(Items.LIGHT_BLUE_CANDLE);
					arg2.accept(Items.BLUE_CANDLE);
					arg2.accept(Items.PURPLE_CANDLE);
					arg2.accept(Items.MAGENTA_CANDLE);
					arg2.accept(Items.PINK_CANDLE);
					arg2.accept(Items.WHITE_BANNER);
					arg2.accept(Items.LIGHT_GRAY_BANNER);
					arg2.accept(Items.GRAY_BANNER);
					arg2.accept(Items.BLACK_BANNER);
					arg2.accept(Items.BROWN_BANNER);
					arg2.accept(Items.RED_BANNER);
					arg2.accept(Items.ORANGE_BANNER);
					arg2.accept(Items.YELLOW_BANNER);
					arg2.accept(Items.LIME_BANNER);
					arg2.accept(Items.GREEN_BANNER);
					arg2.accept(Items.CYAN_BANNER);
					arg2.accept(Items.LIGHT_BLUE_BANNER);
					arg2.accept(Items.BLUE_BANNER);
					arg2.accept(Items.PURPLE_BANNER);
					arg2.accept(Items.MAGENTA_BANNER);
					arg2.accept(Items.PINK_BANNER);
				}))
				.build()
		);
		Registry.register(
			arg,
			NATURAL_BLOCKS,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 2)
				.title(Component.translatable("itemGroup.natural"))
				.icon(() -> new ItemStack(Blocks.GRASS_BLOCK))
				.displayItems((CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
					arg2.accept(Items.GRASS_BLOCK);
					arg2.accept(Items.PODZOL);
					arg2.accept(Items.MYCELIUM);
					arg2.accept(Items.DIRT_PATH);
					arg2.accept(Items.DIRT);
					arg2.accept(Items.COARSE_DIRT);
					arg2.accept(Items.ROOTED_DIRT);
					arg2.accept(Items.FARMLAND);
					arg2.accept(Items.MUD);
					arg2.accept(Items.CLAY);
					arg2.accept(Items.GRAVEL);
					arg2.accept(Items.SAND);
					arg2.accept(Items.SANDSTONE);
					arg2.accept(Items.RED_SAND);
					arg2.accept(Items.RED_SANDSTONE);
					arg2.accept(Items.ICE);
					arg2.accept(Items.PACKED_ICE);
					arg2.accept(Items.BLUE_ICE);
					arg2.accept(Items.SNOW_BLOCK);
					arg2.accept(Items.SNOW);
					arg2.accept(Items.MOSS_BLOCK);
					arg2.accept(Items.MOSS_CARPET);
					arg2.accept(Items.PALE_MOSS_BLOCK);
					arg2.accept(Items.PALE_MOSS_CARPET);
					arg2.accept(Items.PALE_HANGING_MOSS);
					arg2.accept(Items.STONE);
					arg2.accept(Items.DEEPSLATE);
					arg2.accept(Items.GRANITE);
					arg2.accept(Items.DIORITE);
					arg2.accept(Items.ANDESITE);
					arg2.accept(Items.CALCITE);
					arg2.accept(Items.TUFF);
					arg2.accept(Items.DRIPSTONE_BLOCK);
					arg2.accept(Items.POINTED_DRIPSTONE);
					arg2.accept(Items.PRISMARINE);
					arg2.accept(Items.MAGMA_BLOCK);
					arg2.accept(Items.OBSIDIAN);
					arg2.accept(Items.CRYING_OBSIDIAN);
					arg2.accept(Items.NETHERRACK);
					arg2.accept(Items.CRIMSON_NYLIUM);
					arg2.accept(Items.WARPED_NYLIUM);
					arg2.accept(Items.SOUL_SAND);
					arg2.accept(Items.SOUL_SOIL);
					arg2.accept(Items.BONE_BLOCK);
					arg2.accept(Items.BLACKSTONE);
					arg2.accept(Items.BASALT);
					arg2.accept(Items.SMOOTH_BASALT);
					arg2.accept(Items.END_STONE);
					arg2.accept(Items.COAL_ORE);
					arg2.accept(Items.DEEPSLATE_COAL_ORE);
					arg2.accept(Items.IRON_ORE);
					arg2.accept(Items.DEEPSLATE_IRON_ORE);
					arg2.accept(Items.COPPER_ORE);
					arg2.accept(Items.DEEPSLATE_COPPER_ORE);
					arg2.accept(Items.GOLD_ORE);
					arg2.accept(Items.DEEPSLATE_GOLD_ORE);
					arg2.accept(Items.REDSTONE_ORE);
					arg2.accept(Items.DEEPSLATE_REDSTONE_ORE);
					arg2.accept(Items.EMERALD_ORE);
					arg2.accept(Items.DEEPSLATE_EMERALD_ORE);
					arg2.accept(Items.LAPIS_ORE);
					arg2.accept(Items.DEEPSLATE_LAPIS_ORE);
					arg2.accept(Items.DIAMOND_ORE);
					arg2.accept(Items.DEEPSLATE_DIAMOND_ORE);
					arg2.accept(Items.NETHER_GOLD_ORE);
					arg2.accept(Items.NETHER_QUARTZ_ORE);
					arg2.accept(Items.ANCIENT_DEBRIS);
					arg2.accept(Items.RAW_IRON_BLOCK);
					arg2.accept(Items.RAW_COPPER_BLOCK);
					arg2.accept(Items.RAW_GOLD_BLOCK);
					arg2.accept(Items.GLOWSTONE);
					arg2.accept(Items.AMETHYST_BLOCK);
					arg2.accept(Items.BUDDING_AMETHYST);
					arg2.accept(Items.SMALL_AMETHYST_BUD);
					arg2.accept(Items.MEDIUM_AMETHYST_BUD);
					arg2.accept(Items.LARGE_AMETHYST_BUD);
					arg2.accept(Items.AMETHYST_CLUSTER);
					arg2.accept(Items.OAK_LOG);
					arg2.accept(Items.SPRUCE_LOG);
					arg2.accept(Items.BIRCH_LOG);
					arg2.accept(Items.JUNGLE_LOG);
					arg2.accept(Items.ACACIA_LOG);
					arg2.accept(Items.DARK_OAK_LOG);
					arg2.accept(Items.MANGROVE_LOG);
					arg2.accept(Items.MANGROVE_ROOTS);
					arg2.accept(Items.MUDDY_MANGROVE_ROOTS);
					arg2.accept(Items.CHERRY_LOG);
					arg2.accept(Items.PALE_OAK_LOG);
					arg2.accept(Items.MUSHROOM_STEM);
					arg2.accept(Items.CRIMSON_STEM);
					arg2.accept(Items.WARPED_STEM);
					arg2.accept(Items.OAK_LEAVES);
					arg2.accept(Items.SPRUCE_LEAVES);
					arg2.accept(Items.BIRCH_LEAVES);
					arg2.accept(Items.JUNGLE_LEAVES);
					arg2.accept(Items.ACACIA_LEAVES);
					arg2.accept(Items.DARK_OAK_LEAVES);
					arg2.accept(Items.MANGROVE_LEAVES);
					arg2.accept(Items.CHERRY_LEAVES);
					arg2.accept(Items.PALE_OAK_LEAVES);
					arg2.accept(Items.AZALEA_LEAVES);
					arg2.accept(Items.FLOWERING_AZALEA_LEAVES);
					arg2.accept(Items.BROWN_MUSHROOM_BLOCK);
					arg2.accept(Items.RED_MUSHROOM_BLOCK);
					arg2.accept(Items.NETHER_WART_BLOCK);
					arg2.accept(Items.WARPED_WART_BLOCK);
					arg2.accept(Items.SHROOMLIGHT);
					arg2.accept(Items.OAK_SAPLING);
					arg2.accept(Items.SPRUCE_SAPLING);
					arg2.accept(Items.BIRCH_SAPLING);
					arg2.accept(Items.JUNGLE_SAPLING);
					arg2.accept(Items.ACACIA_SAPLING);
					arg2.accept(Items.DARK_OAK_SAPLING);
					arg2.accept(Items.MANGROVE_PROPAGULE);
					arg2.accept(Items.CHERRY_SAPLING);
					arg2.accept(Items.PALE_OAK_SAPLING);
					arg2.accept(Items.AZALEA);
					arg2.accept(Items.FLOWERING_AZALEA);
					arg2.accept(Items.BROWN_MUSHROOM);
					arg2.accept(Items.RED_MUSHROOM);
					arg2.accept(Items.CRIMSON_FUNGUS);
					arg2.accept(Items.WARPED_FUNGUS);
					arg2.accept(Items.SHORT_GRASS);
					arg2.accept(Items.FERN);
					arg2.accept(Items.DEAD_BUSH);
					arg2.accept(Items.DANDELION);
					arg2.accept(Items.POPPY);
					arg2.accept(Items.BLUE_ORCHID);
					arg2.accept(Items.ALLIUM);
					arg2.accept(Items.AZURE_BLUET);
					arg2.accept(Items.RED_TULIP);
					arg2.accept(Items.ORANGE_TULIP);
					arg2.accept(Items.WHITE_TULIP);
					arg2.accept(Items.PINK_TULIP);
					arg2.accept(Items.OXEYE_DAISY);
					arg2.accept(Items.CORNFLOWER);
					arg2.accept(Items.LILY_OF_THE_VALLEY);
					arg2.accept(Items.TORCHFLOWER);
					arg2.accept(Items.WITHER_ROSE);
					arg2.accept(Items.PINK_PETALS);
					arg2.accept(Items.SPORE_BLOSSOM);
					arg2.accept(Items.BAMBOO);
					arg2.accept(Items.SUGAR_CANE);
					arg2.accept(Items.CACTUS);
					arg2.accept(Items.CRIMSON_ROOTS);
					arg2.accept(Items.WARPED_ROOTS);
					arg2.accept(Items.NETHER_SPROUTS);
					arg2.accept(Items.WEEPING_VINES);
					arg2.accept(Items.TWISTING_VINES);
					arg2.accept(Items.VINE);
					arg2.accept(Items.TALL_GRASS);
					arg2.accept(Items.LARGE_FERN);
					arg2.accept(Items.SUNFLOWER);
					arg2.accept(Items.LILAC);
					arg2.accept(Items.ROSE_BUSH);
					arg2.accept(Items.PEONY);
					arg2.accept(Items.PITCHER_PLANT);
					arg2.accept(Items.BIG_DRIPLEAF);
					arg2.accept(Items.SMALL_DRIPLEAF);
					arg2.accept(Items.CHORUS_PLANT);
					arg2.accept(Items.CHORUS_FLOWER);
					arg2.accept(Items.GLOW_LICHEN);
					arg2.accept(Items.HANGING_ROOTS);
					arg2.accept(Items.FROGSPAWN);
					arg2.accept(Items.TURTLE_EGG);
					arg2.accept(Items.SNIFFER_EGG);
					arg2.accept(Items.WHEAT_SEEDS);
					arg2.accept(Items.COCOA_BEANS);
					arg2.accept(Items.PUMPKIN_SEEDS);
					arg2.accept(Items.MELON_SEEDS);
					arg2.accept(Items.BEETROOT_SEEDS);
					arg2.accept(Items.TORCHFLOWER_SEEDS);
					arg2.accept(Items.PITCHER_POD);
					arg2.accept(Items.GLOW_BERRIES);
					arg2.accept(Items.SWEET_BERRIES);
					arg2.accept(Items.NETHER_WART);
					arg2.accept(Items.LILY_PAD);
					arg2.accept(Items.SEAGRASS);
					arg2.accept(Items.SEA_PICKLE);
					arg2.accept(Items.KELP);
					arg2.accept(Items.DRIED_KELP_BLOCK);
					arg2.accept(Items.TUBE_CORAL_BLOCK);
					arg2.accept(Items.BRAIN_CORAL_BLOCK);
					arg2.accept(Items.BUBBLE_CORAL_BLOCK);
					arg2.accept(Items.FIRE_CORAL_BLOCK);
					arg2.accept(Items.HORN_CORAL_BLOCK);
					arg2.accept(Items.DEAD_TUBE_CORAL_BLOCK);
					arg2.accept(Items.DEAD_BRAIN_CORAL_BLOCK);
					arg2.accept(Items.DEAD_BUBBLE_CORAL_BLOCK);
					arg2.accept(Items.DEAD_FIRE_CORAL_BLOCK);
					arg2.accept(Items.DEAD_HORN_CORAL_BLOCK);
					arg2.accept(Items.TUBE_CORAL);
					arg2.accept(Items.BRAIN_CORAL);
					arg2.accept(Items.BUBBLE_CORAL);
					arg2.accept(Items.FIRE_CORAL);
					arg2.accept(Items.HORN_CORAL);
					arg2.accept(Items.DEAD_TUBE_CORAL);
					arg2.accept(Items.DEAD_BRAIN_CORAL);
					arg2.accept(Items.DEAD_BUBBLE_CORAL);
					arg2.accept(Items.DEAD_FIRE_CORAL);
					arg2.accept(Items.DEAD_HORN_CORAL);
					arg2.accept(Items.TUBE_CORAL_FAN);
					arg2.accept(Items.BRAIN_CORAL_FAN);
					arg2.accept(Items.BUBBLE_CORAL_FAN);
					arg2.accept(Items.FIRE_CORAL_FAN);
					arg2.accept(Items.HORN_CORAL_FAN);
					arg2.accept(Items.DEAD_TUBE_CORAL_FAN);
					arg2.accept(Items.DEAD_BRAIN_CORAL_FAN);
					arg2.accept(Items.DEAD_BUBBLE_CORAL_FAN);
					arg2.accept(Items.DEAD_FIRE_CORAL_FAN);
					arg2.accept(Items.DEAD_HORN_CORAL_FAN);
					arg2.accept(Items.SPONGE);
					arg2.accept(Items.WET_SPONGE);
					arg2.accept(Items.MELON);
					arg2.accept(Items.PUMPKIN);
					arg2.accept(Items.CARVED_PUMPKIN);
					arg2.accept(Items.JACK_O_LANTERN);
					arg2.accept(Items.HAY_BLOCK);
					arg2.accept(Items.BEE_NEST);
					arg2.accept(Items.HONEYCOMB_BLOCK);
					arg2.accept(Items.SLIME_BLOCK);
					arg2.accept(Items.HONEY_BLOCK);
					arg2.accept(Items.OCHRE_FROGLIGHT);
					arg2.accept(Items.VERDANT_FROGLIGHT);
					arg2.accept(Items.PEARLESCENT_FROGLIGHT);
					arg2.accept(Items.SCULK);
					arg2.accept(Items.SCULK_VEIN);
					arg2.accept(Items.SCULK_CATALYST);
					arg2.accept(Items.SCULK_SHRIEKER);
					arg2.accept(Items.SCULK_SENSOR);
					arg2.accept(Items.COBWEB);
					arg2.accept(Items.BEDROCK);
				}))
				.build()
		);
		Registry.register(
			arg,
			FUNCTIONAL_BLOCKS,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 3)
				.title(Component.translatable("itemGroup.functional"))
				.icon(() -> new ItemStack(Items.OAK_SIGN))
				.displayItems(
					(CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
						arg2.accept(Items.TORCH);
						arg2.accept(Items.SOUL_TORCH);
						arg2.accept(Items.REDSTONE_TORCH);
						arg2.accept(Items.LANTERN);
						arg2.accept(Items.SOUL_LANTERN);
						arg2.accept(Items.CHAIN);
						arg2.accept(Items.END_ROD);
						arg2.accept(Items.SEA_LANTERN);
						arg2.accept(Items.REDSTONE_LAMP);
						arg2.accept(Items.COPPER_BULB);
						arg2.accept(Items.EXPOSED_COPPER_BULB);
						arg2.accept(Items.WEATHERED_COPPER_BULB);
						arg2.accept(Items.OXIDIZED_COPPER_BULB);
						arg2.accept(Items.WAXED_COPPER_BULB);
						arg2.accept(Items.WAXED_EXPOSED_COPPER_BULB);
						arg2.accept(Items.WAXED_WEATHERED_COPPER_BULB);
						arg2.accept(Items.WAXED_OXIDIZED_COPPER_BULB);
						arg2.accept(Items.GLOWSTONE);
						arg2.accept(Items.SHROOMLIGHT);
						arg2.accept(Items.OCHRE_FROGLIGHT);
						arg2.accept(Items.VERDANT_FROGLIGHT);
						arg2.accept(Items.PEARLESCENT_FROGLIGHT);
						arg2.accept(Items.CRYING_OBSIDIAN);
						arg2.accept(Items.GLOW_LICHEN);
						arg2.accept(Items.MAGMA_BLOCK);
						arg2.accept(Items.CRAFTING_TABLE);
						arg2.accept(Items.STONECUTTER);
						arg2.accept(Items.CARTOGRAPHY_TABLE);
						arg2.accept(Items.FLETCHING_TABLE);
						arg2.accept(Items.SMITHING_TABLE);
						arg2.accept(Items.GRINDSTONE);
						arg2.accept(Items.LOOM);
						arg2.accept(Items.FURNACE);
						arg2.accept(Items.SMOKER);
						arg2.accept(Items.BLAST_FURNACE);
						arg2.accept(Items.CAMPFIRE);
						arg2.accept(Items.SOUL_CAMPFIRE);
						arg2.accept(Items.ANVIL);
						arg2.accept(Items.CHIPPED_ANVIL);
						arg2.accept(Items.DAMAGED_ANVIL);
						arg2.accept(Items.COMPOSTER);
						arg2.accept(Items.NOTE_BLOCK);
						arg2.accept(Items.JUKEBOX);
						arg2.accept(Items.ENCHANTING_TABLE);
						arg2.accept(Items.END_CRYSTAL);
						arg2.accept(Items.BREWING_STAND);
						arg2.accept(Items.CAULDRON);
						arg2.accept(Items.BELL);
						arg2.accept(Items.BEACON);
						arg2.accept(Items.CONDUIT);
						arg2.accept(Items.LODESTONE);
						arg2.accept(Items.LADDER);
						arg2.accept(Items.SCAFFOLDING);
						arg2.accept(Items.BEE_NEST);
						arg2.accept(Items.BEEHIVE);
						arg2.accept(Items.SUSPICIOUS_SAND);
						arg2.accept(Items.SUSPICIOUS_GRAVEL);
						arg2.accept(Items.LIGHTNING_ROD);
						arg2.accept(Items.FLOWER_POT);
						arg2.accept(Items.DECORATED_POT);
						arg2.accept(Items.ARMOR_STAND);
						arg2.accept(Items.ITEM_FRAME);
						arg2.accept(Items.GLOW_ITEM_FRAME);
						arg2.accept(Items.PAINTING);
						argx.holders()
							.lookup(Registries.PAINTING_VARIANT)
							.ifPresent(
								arg3 -> generatePresetPaintings(
									arg2, argx.holders(), arg3, argxxx -> argxxx.is(PaintingVariantTags.PLACEABLE), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
								)
							);
						arg2.accept(Items.BOOKSHELF);
						arg2.accept(Items.CHISELED_BOOKSHELF);
						arg2.accept(Items.LECTERN);
						arg2.accept(Items.TINTED_GLASS);
						arg2.accept(Items.OAK_SIGN);
						arg2.accept(Items.OAK_HANGING_SIGN);
						arg2.accept(Items.SPRUCE_SIGN);
						arg2.accept(Items.SPRUCE_HANGING_SIGN);
						arg2.accept(Items.BIRCH_SIGN);
						arg2.accept(Items.BIRCH_HANGING_SIGN);
						arg2.accept(Items.JUNGLE_SIGN);
						arg2.accept(Items.JUNGLE_HANGING_SIGN);
						arg2.accept(Items.ACACIA_SIGN);
						arg2.accept(Items.ACACIA_HANGING_SIGN);
						arg2.accept(Items.DARK_OAK_SIGN);
						arg2.accept(Items.DARK_OAK_HANGING_SIGN);
						arg2.accept(Items.MANGROVE_SIGN);
						arg2.accept(Items.MANGROVE_HANGING_SIGN);
						arg2.accept(Items.CHERRY_SIGN);
						arg2.accept(Items.CHERRY_HANGING_SIGN);
						arg2.accept(Items.PALE_OAK_SIGN);
						arg2.accept(Items.PALE_OAK_HANGING_SIGN);
						arg2.accept(Items.BAMBOO_SIGN);
						arg2.accept(Items.BAMBOO_HANGING_SIGN);
						arg2.accept(Items.CRIMSON_SIGN);
						arg2.accept(Items.CRIMSON_HANGING_SIGN);
						arg2.accept(Items.WARPED_SIGN);
						arg2.accept(Items.WARPED_HANGING_SIGN);
						arg2.accept(Items.CHEST);
						arg2.accept(Items.BARREL);
						arg2.accept(Items.ENDER_CHEST);
						arg2.accept(Items.SHULKER_BOX);
						arg2.accept(Items.WHITE_SHULKER_BOX);
						arg2.accept(Items.LIGHT_GRAY_SHULKER_BOX);
						arg2.accept(Items.GRAY_SHULKER_BOX);
						arg2.accept(Items.BLACK_SHULKER_BOX);
						arg2.accept(Items.BROWN_SHULKER_BOX);
						arg2.accept(Items.RED_SHULKER_BOX);
						arg2.accept(Items.ORANGE_SHULKER_BOX);
						arg2.accept(Items.YELLOW_SHULKER_BOX);
						arg2.accept(Items.LIME_SHULKER_BOX);
						arg2.accept(Items.GREEN_SHULKER_BOX);
						arg2.accept(Items.CYAN_SHULKER_BOX);
						arg2.accept(Items.LIGHT_BLUE_SHULKER_BOX);
						arg2.accept(Items.BLUE_SHULKER_BOX);
						arg2.accept(Items.PURPLE_SHULKER_BOX);
						arg2.accept(Items.MAGENTA_SHULKER_BOX);
						arg2.accept(Items.PINK_SHULKER_BOX);
						arg2.accept(Items.RESPAWN_ANCHOR);
						arg2.accept(Items.WHITE_BED);
						arg2.accept(Items.LIGHT_GRAY_BED);
						arg2.accept(Items.GRAY_BED);
						arg2.accept(Items.BLACK_BED);
						arg2.accept(Items.BROWN_BED);
						arg2.accept(Items.RED_BED);
						arg2.accept(Items.ORANGE_BED);
						arg2.accept(Items.YELLOW_BED);
						arg2.accept(Items.LIME_BED);
						arg2.accept(Items.GREEN_BED);
						arg2.accept(Items.CYAN_BED);
						arg2.accept(Items.LIGHT_BLUE_BED);
						arg2.accept(Items.BLUE_BED);
						arg2.accept(Items.PURPLE_BED);
						arg2.accept(Items.MAGENTA_BED);
						arg2.accept(Items.PINK_BED);
						arg2.accept(Items.CANDLE);
						arg2.accept(Items.WHITE_CANDLE);
						arg2.accept(Items.LIGHT_GRAY_CANDLE);
						arg2.accept(Items.GRAY_CANDLE);
						arg2.accept(Items.BLACK_CANDLE);
						arg2.accept(Items.BROWN_CANDLE);
						arg2.accept(Items.RED_CANDLE);
						arg2.accept(Items.ORANGE_CANDLE);
						arg2.accept(Items.YELLOW_CANDLE);
						arg2.accept(Items.LIME_CANDLE);
						arg2.accept(Items.GREEN_CANDLE);
						arg2.accept(Items.CYAN_CANDLE);
						arg2.accept(Items.LIGHT_BLUE_CANDLE);
						arg2.accept(Items.BLUE_CANDLE);
						arg2.accept(Items.PURPLE_CANDLE);
						arg2.accept(Items.MAGENTA_CANDLE);
						arg2.accept(Items.PINK_CANDLE);
						arg2.accept(Items.WHITE_BANNER);
						arg2.accept(Items.LIGHT_GRAY_BANNER);
						arg2.accept(Items.GRAY_BANNER);
						arg2.accept(Items.BLACK_BANNER);
						arg2.accept(Items.BROWN_BANNER);
						arg2.accept(Items.RED_BANNER);
						arg2.accept(Items.ORANGE_BANNER);
						arg2.accept(Items.YELLOW_BANNER);
						arg2.accept(Items.LIME_BANNER);
						arg2.accept(Items.GREEN_BANNER);
						arg2.accept(Items.CYAN_BANNER);
						arg2.accept(Items.LIGHT_BLUE_BANNER);
						arg2.accept(Items.BLUE_BANNER);
						arg2.accept(Items.PURPLE_BANNER);
						arg2.accept(Items.MAGENTA_BANNER);
						arg2.accept(Items.PINK_BANNER);
						arg2.accept(Raid.getOminousBannerInstance(argx.holders().lookupOrThrow(Registries.BANNER_PATTERN)));
						arg2.accept(Items.SKELETON_SKULL);
						arg2.accept(Items.WITHER_SKELETON_SKULL);
						arg2.accept(Items.PLAYER_HEAD);
						arg2.accept(Items.ZOMBIE_HEAD);
						arg2.accept(Items.CREEPER_HEAD);
						arg2.accept(Items.PIGLIN_HEAD);
						arg2.accept(Items.DRAGON_HEAD);
						arg2.accept(Items.DRAGON_EGG);
						arg2.accept(Items.END_PORTAL_FRAME);
						arg2.accept(Items.ENDER_EYE);
						arg2.accept(Items.VAULT);
						arg2.accept(Items.INFESTED_STONE);
						arg2.accept(Items.INFESTED_COBBLESTONE);
						arg2.accept(Items.INFESTED_STONE_BRICKS);
						arg2.accept(Items.INFESTED_MOSSY_STONE_BRICKS);
						arg2.accept(Items.INFESTED_CRACKED_STONE_BRICKS);
						arg2.accept(Items.INFESTED_CHISELED_STONE_BRICKS);
						arg2.accept(Items.INFESTED_DEEPSLATE);
					})
				)
				.build()
		);
		Registry.register(
			arg,
			REDSTONE_BLOCKS,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 4)
				.title(Component.translatable("itemGroup.redstone"))
				.icon(() -> new ItemStack(Items.REDSTONE))
				.displayItems((CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
					arg2.accept(Items.REDSTONE);
					arg2.accept(Items.REDSTONE_TORCH);
					arg2.accept(Items.REDSTONE_BLOCK);
					arg2.accept(Items.REPEATER);
					arg2.accept(Items.COMPARATOR);
					arg2.accept(Items.TARGET);
					arg2.accept(Items.WAXED_COPPER_BULB);
					arg2.accept(Items.WAXED_EXPOSED_COPPER_BULB);
					arg2.accept(Items.WAXED_WEATHERED_COPPER_BULB);
					arg2.accept(Items.WAXED_OXIDIZED_COPPER_BULB);
					arg2.accept(Items.LEVER);
					arg2.accept(Items.OAK_BUTTON);
					arg2.accept(Items.STONE_BUTTON);
					arg2.accept(Items.OAK_PRESSURE_PLATE);
					arg2.accept(Items.STONE_PRESSURE_PLATE);
					arg2.accept(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
					arg2.accept(Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
					arg2.accept(Items.SCULK_SENSOR);
					arg2.accept(Items.CALIBRATED_SCULK_SENSOR);
					arg2.accept(Items.SCULK_SHRIEKER);
					arg2.accept(Items.AMETHYST_BLOCK);
					arg2.accept(Items.WHITE_WOOL);
					arg2.accept(Items.TRIPWIRE_HOOK);
					arg2.accept(Items.STRING);
					arg2.accept(Items.LECTERN);
					arg2.accept(Items.DAYLIGHT_DETECTOR);
					arg2.accept(Items.LIGHTNING_ROD);
					arg2.accept(Items.PISTON);
					arg2.accept(Items.STICKY_PISTON);
					arg2.accept(Items.SLIME_BLOCK);
					arg2.accept(Items.HONEY_BLOCK);
					arg2.accept(Items.DISPENSER);
					arg2.accept(Items.DROPPER);
					arg2.accept(Items.CRAFTER);
					arg2.accept(Items.HOPPER);
					arg2.accept(Items.CHEST);
					arg2.accept(Items.BARREL);
					arg2.accept(Items.CHISELED_BOOKSHELF);
					arg2.accept(Items.FURNACE);
					arg2.accept(Items.TRAPPED_CHEST);
					arg2.accept(Items.JUKEBOX);
					arg2.accept(Items.DECORATED_POT);
					arg2.accept(Items.OBSERVER);
					arg2.accept(Items.NOTE_BLOCK);
					arg2.accept(Items.COMPOSTER);
					arg2.accept(Items.CAULDRON);
					arg2.accept(Items.RAIL);
					arg2.accept(Items.POWERED_RAIL);
					arg2.accept(Items.DETECTOR_RAIL);
					arg2.accept(Items.ACTIVATOR_RAIL);
					arg2.accept(Items.MINECART);
					arg2.accept(Items.HOPPER_MINECART);
					arg2.accept(Items.CHEST_MINECART);
					arg2.accept(Items.FURNACE_MINECART);
					arg2.accept(Items.TNT_MINECART);
					arg2.accept(Items.OAK_CHEST_BOAT);
					arg2.accept(Items.BAMBOO_CHEST_RAFT);
					arg2.accept(Items.OAK_DOOR);
					arg2.accept(Items.IRON_DOOR);
					arg2.accept(Items.OAK_FENCE_GATE);
					arg2.accept(Items.OAK_TRAPDOOR);
					arg2.accept(Items.IRON_TRAPDOOR);
					arg2.accept(Items.TNT);
					arg2.accept(Items.REDSTONE_LAMP);
					arg2.accept(Items.BELL);
					arg2.accept(Items.BIG_DRIPLEAF);
					arg2.accept(Items.ARMOR_STAND);
					arg2.accept(Items.REDSTONE_ORE);
				}))
				.build()
		);
		Registry.register(
			arg,
			HOTBAR,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 5)
				.title(Component.translatable("itemGroup.hotbar"))
				.icon(() -> new ItemStack(Blocks.BOOKSHELF))
				.alignedRight()
				.type(CreativeModeTab.Type.HOTBAR)
				.build()
		);
		Registry.register(
			arg,
			SEARCH,
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 6)
				.title(Component.translatable("itemGroup.search"))
				.icon(() -> new ItemStack(Items.COMPASS))
				.displayItems((CreativeModeTab.DisplayItemsGenerator)((arg2, arg3) -> {
					Set<ItemStack> set = ItemStackLinkedSet.createTypeAndComponentsSet();

					for (CreativeModeTab creativeModeTab : arg) {
						if (creativeModeTab.getType() != CreativeModeTab.Type.SEARCH) {
							set.addAll(creativeModeTab.getSearchTabDisplayItems());
						}
					}

					arg3.acceptAll(set);
				}))
				.backgroundTexture(SEARCH_BACKGROUND)
				.alignedRight()
				.type(CreativeModeTab.Type.SEARCH)
				.build()
		);
		Registry.register(
			arg,
			TOOLS_AND_UTILITIES,
			CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 0)
				.title(Component.translatable("itemGroup.tools"))
				.icon(() -> new ItemStack(Items.DIAMOND_PICKAXE))
				.displayItems(
					(CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
						arg2.accept(Items.WOODEN_SHOVEL);
						arg2.accept(Items.WOODEN_PICKAXE);
						arg2.accept(Items.WOODEN_AXE);
						arg2.accept(Items.WOODEN_HOE);
						arg2.accept(Items.STONE_SHOVEL);
						arg2.accept(Items.STONE_PICKAXE);
						arg2.accept(Items.STONE_AXE);
						arg2.accept(Items.STONE_HOE);
						arg2.accept(Items.IRON_SHOVEL);
						arg2.accept(Items.IRON_PICKAXE);
						arg2.accept(Items.IRON_AXE);
						arg2.accept(Items.IRON_HOE);
						arg2.accept(Items.GOLDEN_SHOVEL);
						arg2.accept(Items.GOLDEN_PICKAXE);
						arg2.accept(Items.GOLDEN_AXE);
						arg2.accept(Items.GOLDEN_HOE);
						arg2.accept(Items.DIAMOND_SHOVEL);
						arg2.accept(Items.DIAMOND_PICKAXE);
						arg2.accept(Items.DIAMOND_AXE);
						arg2.accept(Items.DIAMOND_HOE);
						arg2.accept(Items.NETHERITE_SHOVEL);
						arg2.accept(Items.NETHERITE_PICKAXE);
						arg2.accept(Items.NETHERITE_AXE);
						arg2.accept(Items.NETHERITE_HOE);
						arg2.accept(Items.BUCKET);
						arg2.accept(Items.WATER_BUCKET);
						arg2.accept(Items.COD_BUCKET);
						arg2.accept(Items.SALMON_BUCKET);
						arg2.accept(Items.TROPICAL_FISH_BUCKET);
						arg2.accept(Items.PUFFERFISH_BUCKET);
						arg2.accept(Items.AXOLOTL_BUCKET);
						arg2.accept(Items.TADPOLE_BUCKET);
						arg2.accept(Items.LAVA_BUCKET);
						arg2.accept(Items.POWDER_SNOW_BUCKET);
						arg2.accept(Items.MILK_BUCKET);
						arg2.accept(Items.FISHING_ROD);
						arg2.accept(Items.FLINT_AND_STEEL);
						arg2.accept(Items.FIRE_CHARGE);
						arg2.accept(Items.BONE_MEAL);
						arg2.accept(Items.SHEARS);
						arg2.accept(Items.BRUSH);
						arg2.accept(Items.NAME_TAG);
						arg2.accept(Items.LEAD);
						arg2.accept(Items.BUNDLE);
						arg2.accept(Items.WHITE_BUNDLE);
						arg2.accept(Items.LIGHT_GRAY_BUNDLE);
						arg2.accept(Items.GRAY_BUNDLE);
						arg2.accept(Items.BLACK_BUNDLE);
						arg2.accept(Items.BROWN_BUNDLE);
						arg2.accept(Items.RED_BUNDLE);
						arg2.accept(Items.ORANGE_BUNDLE);
						arg2.accept(Items.YELLOW_BUNDLE);
						arg2.accept(Items.LIME_BUNDLE);
						arg2.accept(Items.GREEN_BUNDLE);
						arg2.accept(Items.CYAN_BUNDLE);
						arg2.accept(Items.LIGHT_BLUE_BUNDLE);
						arg2.accept(Items.BLUE_BUNDLE);
						arg2.accept(Items.PURPLE_BUNDLE);
						arg2.accept(Items.MAGENTA_BUNDLE);
						arg2.accept(Items.PINK_BUNDLE);
						arg2.accept(Items.COMPASS);
						arg2.accept(Items.RECOVERY_COMPASS);
						arg2.accept(Items.CLOCK);
						arg2.accept(Items.SPYGLASS);
						arg2.accept(Items.MAP);
						arg2.accept(Items.WRITABLE_BOOK);
						arg2.accept(Items.WIND_CHARGE);
						arg2.accept(Items.ENDER_PEARL);
						arg2.accept(Items.ENDER_EYE);
						arg2.accept(Items.ELYTRA);
						generateFireworksAllDurations(arg2, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
						arg2.accept(Items.SADDLE);
						arg2.accept(Items.CARROT_ON_A_STICK);
						arg2.accept(Items.WARPED_FUNGUS_ON_A_STICK);
						arg2.accept(Items.OAK_BOAT);
						arg2.accept(Items.OAK_CHEST_BOAT);
						arg2.accept(Items.SPRUCE_BOAT);
						arg2.accept(Items.SPRUCE_CHEST_BOAT);
						arg2.accept(Items.BIRCH_BOAT);
						arg2.accept(Items.BIRCH_CHEST_BOAT);
						arg2.accept(Items.JUNGLE_BOAT);
						arg2.accept(Items.JUNGLE_CHEST_BOAT);
						arg2.accept(Items.ACACIA_BOAT);
						arg2.accept(Items.ACACIA_CHEST_BOAT);
						arg2.accept(Items.DARK_OAK_BOAT);
						arg2.accept(Items.DARK_OAK_CHEST_BOAT);
						arg2.accept(Items.MANGROVE_BOAT);
						arg2.accept(Items.MANGROVE_CHEST_BOAT);
						arg2.accept(Items.CHERRY_BOAT);
						arg2.accept(Items.CHERRY_CHEST_BOAT);
						arg2.accept(Items.PALE_OAK_BOAT);
						arg2.accept(Items.PALE_OAK_CHEST_BOAT);
						arg2.accept(Items.BAMBOO_RAFT);
						arg2.accept(Items.BAMBOO_CHEST_RAFT);
						arg2.accept(Items.RAIL);
						arg2.accept(Items.POWERED_RAIL);
						arg2.accept(Items.DETECTOR_RAIL);
						arg2.accept(Items.ACTIVATOR_RAIL);
						arg2.accept(Items.MINECART);
						arg2.accept(Items.HOPPER_MINECART);
						arg2.accept(Items.CHEST_MINECART);
						arg2.accept(Items.FURNACE_MINECART);
						arg2.accept(Items.TNT_MINECART);
						argx.holders()
							.lookup(Registries.INSTRUMENT)
							.ifPresent(
								arg2x -> generateInstrumentTypes(arg2, arg2x, Items.GOAT_HORN, InstrumentTags.GOAT_HORNS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)
							);
						arg2.accept(Items.MUSIC_DISC_13);
						arg2.accept(Items.MUSIC_DISC_CAT);
						arg2.accept(Items.MUSIC_DISC_BLOCKS);
						arg2.accept(Items.MUSIC_DISC_CHIRP);
						arg2.accept(Items.MUSIC_DISC_FAR);
						arg2.accept(Items.MUSIC_DISC_MALL);
						arg2.accept(Items.MUSIC_DISC_MELLOHI);
						arg2.accept(Items.MUSIC_DISC_STAL);
						arg2.accept(Items.MUSIC_DISC_STRAD);
						arg2.accept(Items.MUSIC_DISC_WARD);
						arg2.accept(Items.MUSIC_DISC_11);
						arg2.accept(Items.MUSIC_DISC_CREATOR_MUSIC_BOX);
						arg2.accept(Items.MUSIC_DISC_WAIT);
						arg2.accept(Items.MUSIC_DISC_CREATOR);
						arg2.accept(Items.MUSIC_DISC_PRECIPICE);
						arg2.accept(Items.MUSIC_DISC_OTHERSIDE);
						arg2.accept(Items.MUSIC_DISC_RELIC);
						arg2.accept(Items.MUSIC_DISC_5);
						arg2.accept(Items.MUSIC_DISC_PIGSTEP);
					})
				)
				.build()
		);
		Registry.register(
			arg,
			COMBAT,
			CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 1)
				.title(Component.translatable("itemGroup.combat"))
				.icon(() -> new ItemStack(Items.NETHERITE_SWORD))
				.displayItems(
					(CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
						arg2.accept(Items.WOODEN_SWORD);
						arg2.accept(Items.STONE_SWORD);
						arg2.accept(Items.IRON_SWORD);
						arg2.accept(Items.GOLDEN_SWORD);
						arg2.accept(Items.DIAMOND_SWORD);
						arg2.accept(Items.NETHERITE_SWORD);
						arg2.accept(Items.WOODEN_AXE);
						arg2.accept(Items.STONE_AXE);
						arg2.accept(Items.IRON_AXE);
						arg2.accept(Items.GOLDEN_AXE);
						arg2.accept(Items.DIAMOND_AXE);
						arg2.accept(Items.NETHERITE_AXE);
						arg2.accept(Items.TRIDENT);
						arg2.accept(Items.MACE);
						arg2.accept(Items.SHIELD);
						arg2.accept(Items.LEATHER_HELMET);
						arg2.accept(Items.LEATHER_CHESTPLATE);
						arg2.accept(Items.LEATHER_LEGGINGS);
						arg2.accept(Items.LEATHER_BOOTS);
						arg2.accept(Items.CHAINMAIL_HELMET);
						arg2.accept(Items.CHAINMAIL_CHESTPLATE);
						arg2.accept(Items.CHAINMAIL_LEGGINGS);
						arg2.accept(Items.CHAINMAIL_BOOTS);
						arg2.accept(Items.IRON_HELMET);
						arg2.accept(Items.IRON_CHESTPLATE);
						arg2.accept(Items.IRON_LEGGINGS);
						arg2.accept(Items.IRON_BOOTS);
						arg2.accept(Items.GOLDEN_HELMET);
						arg2.accept(Items.GOLDEN_CHESTPLATE);
						arg2.accept(Items.GOLDEN_LEGGINGS);
						arg2.accept(Items.GOLDEN_BOOTS);
						arg2.accept(Items.DIAMOND_HELMET);
						arg2.accept(Items.DIAMOND_CHESTPLATE);
						arg2.accept(Items.DIAMOND_LEGGINGS);
						arg2.accept(Items.DIAMOND_BOOTS);
						arg2.accept(Items.NETHERITE_HELMET);
						arg2.accept(Items.NETHERITE_CHESTPLATE);
						arg2.accept(Items.NETHERITE_LEGGINGS);
						arg2.accept(Items.NETHERITE_BOOTS);
						arg2.accept(Items.TURTLE_HELMET);
						arg2.accept(Items.LEATHER_HORSE_ARMOR);
						arg2.accept(Items.IRON_HORSE_ARMOR);
						arg2.accept(Items.GOLDEN_HORSE_ARMOR);
						arg2.accept(Items.DIAMOND_HORSE_ARMOR);
						arg2.accept(Items.WOLF_ARMOR);
						arg2.accept(Items.TOTEM_OF_UNDYING);
						arg2.accept(Items.TNT);
						arg2.accept(Items.END_CRYSTAL);
						arg2.accept(Items.SNOWBALL);
						arg2.accept(Items.EGG);
						arg2.accept(Items.WIND_CHARGE);
						arg2.accept(Items.BOW);
						arg2.accept(Items.CROSSBOW);
						generateFireworksAllDurations(arg2, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
						arg2.accept(Items.ARROW);
						arg2.accept(Items.SPECTRAL_ARROW);
						argx.holders()
							.lookup(Registries.POTION)
							.ifPresent(
								arg3 -> generatePotionEffectTypes(arg2, arg3, Items.TIPPED_ARROW, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, argx.enabledFeatures())
							);
					})
				)
				.build()
		);
		Registry.register(
			arg,
			FOOD_AND_DRINKS,
			CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 2)
				.title(Component.translatable("itemGroup.foodAndDrink"))
				.icon(() -> new ItemStack(Items.GOLDEN_APPLE))
				.displayItems((CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
					arg2.accept(Items.APPLE);
					arg2.accept(Items.GOLDEN_APPLE);
					arg2.accept(Items.ENCHANTED_GOLDEN_APPLE);
					arg2.accept(Items.MELON_SLICE);
					arg2.accept(Items.SWEET_BERRIES);
					arg2.accept(Items.GLOW_BERRIES);
					arg2.accept(Items.CHORUS_FRUIT);
					arg2.accept(Items.CARROT);
					arg2.accept(Items.GOLDEN_CARROT);
					arg2.accept(Items.POTATO);
					arg2.accept(Items.BAKED_POTATO);
					arg2.accept(Items.POISONOUS_POTATO);
					arg2.accept(Items.BEETROOT);
					arg2.accept(Items.DRIED_KELP);
					arg2.accept(Items.BEEF);
					arg2.accept(Items.COOKED_BEEF);
					arg2.accept(Items.PORKCHOP);
					arg2.accept(Items.COOKED_PORKCHOP);
					arg2.accept(Items.MUTTON);
					arg2.accept(Items.COOKED_MUTTON);
					arg2.accept(Items.CHICKEN);
					arg2.accept(Items.COOKED_CHICKEN);
					arg2.accept(Items.RABBIT);
					arg2.accept(Items.COOKED_RABBIT);
					arg2.accept(Items.COD);
					arg2.accept(Items.COOKED_COD);
					arg2.accept(Items.SALMON);
					arg2.accept(Items.COOKED_SALMON);
					arg2.accept(Items.TROPICAL_FISH);
					arg2.accept(Items.PUFFERFISH);
					arg2.accept(Items.BREAD);
					arg2.accept(Items.COOKIE);
					arg2.accept(Items.CAKE);
					arg2.accept(Items.PUMPKIN_PIE);
					arg2.accept(Items.ROTTEN_FLESH);
					arg2.accept(Items.SPIDER_EYE);
					arg2.accept(Items.MUSHROOM_STEW);
					arg2.accept(Items.BEETROOT_SOUP);
					arg2.accept(Items.RABBIT_STEW);
					generateSuspiciousStews(arg2, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
					arg2.accept(Items.MILK_BUCKET);
					arg2.accept(Items.HONEY_BOTTLE);
					generateOminousBottles(arg2, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
					argx.holders().lookup(Registries.POTION).ifPresent(arg3 -> {
						generatePotionEffectTypes(arg2, arg3, Items.POTION, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, argx.enabledFeatures());
						generatePotionEffectTypes(arg2, arg3, Items.SPLASH_POTION, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, argx.enabledFeatures());
						generatePotionEffectTypes(arg2, arg3, Items.LINGERING_POTION, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, argx.enabledFeatures());
					});
				}))
				.build()
		);
		Registry.register(
			arg,
			INGREDIENTS,
			CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 3)
				.title(Component.translatable("itemGroup.ingredients"))
				.icon(() -> new ItemStack(Items.IRON_INGOT))
				.displayItems((CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
					arg2.accept(Items.COAL);
					arg2.accept(Items.CHARCOAL);
					arg2.accept(Items.RAW_IRON);
					arg2.accept(Items.RAW_COPPER);
					arg2.accept(Items.RAW_GOLD);
					arg2.accept(Items.EMERALD);
					arg2.accept(Items.LAPIS_LAZULI);
					arg2.accept(Items.DIAMOND);
					arg2.accept(Items.ANCIENT_DEBRIS);
					arg2.accept(Items.QUARTZ);
					arg2.accept(Items.AMETHYST_SHARD);
					arg2.accept(Items.IRON_NUGGET);
					arg2.accept(Items.GOLD_NUGGET);
					arg2.accept(Items.IRON_INGOT);
					arg2.accept(Items.COPPER_INGOT);
					arg2.accept(Items.GOLD_INGOT);
					arg2.accept(Items.NETHERITE_SCRAP);
					arg2.accept(Items.NETHERITE_INGOT);
					arg2.accept(Items.STICK);
					arg2.accept(Items.FLINT);
					arg2.accept(Items.WHEAT);
					arg2.accept(Items.BONE);
					arg2.accept(Items.BONE_MEAL);
					arg2.accept(Items.STRING);
					arg2.accept(Items.FEATHER);
					arg2.accept(Items.SNOWBALL);
					arg2.accept(Items.EGG);
					arg2.accept(Items.LEATHER);
					arg2.accept(Items.RABBIT_HIDE);
					arg2.accept(Items.HONEYCOMB);
					arg2.accept(Items.INK_SAC);
					arg2.accept(Items.GLOW_INK_SAC);
					arg2.accept(Items.TURTLE_SCUTE);
					arg2.accept(Items.ARMADILLO_SCUTE);
					arg2.accept(Items.SLIME_BALL);
					arg2.accept(Items.CLAY_BALL);
					arg2.accept(Items.PRISMARINE_SHARD);
					arg2.accept(Items.PRISMARINE_CRYSTALS);
					arg2.accept(Items.NAUTILUS_SHELL);
					arg2.accept(Items.HEART_OF_THE_SEA);
					arg2.accept(Items.FIRE_CHARGE);
					arg2.accept(Items.BLAZE_ROD);
					arg2.accept(Items.BREEZE_ROD);
					arg2.accept(Items.HEAVY_CORE);
					arg2.accept(Items.NETHER_STAR);
					arg2.accept(Items.ENDER_PEARL);
					arg2.accept(Items.ENDER_EYE);
					arg2.accept(Items.SHULKER_SHELL);
					arg2.accept(Items.POPPED_CHORUS_FRUIT);
					arg2.accept(Items.ECHO_SHARD);
					arg2.accept(Items.DISC_FRAGMENT_5);
					arg2.accept(Items.WHITE_DYE);
					arg2.accept(Items.LIGHT_GRAY_DYE);
					arg2.accept(Items.GRAY_DYE);
					arg2.accept(Items.BLACK_DYE);
					arg2.accept(Items.BROWN_DYE);
					arg2.accept(Items.RED_DYE);
					arg2.accept(Items.ORANGE_DYE);
					arg2.accept(Items.YELLOW_DYE);
					arg2.accept(Items.LIME_DYE);
					arg2.accept(Items.GREEN_DYE);
					arg2.accept(Items.CYAN_DYE);
					arg2.accept(Items.LIGHT_BLUE_DYE);
					arg2.accept(Items.BLUE_DYE);
					arg2.accept(Items.PURPLE_DYE);
					arg2.accept(Items.MAGENTA_DYE);
					arg2.accept(Items.PINK_DYE);
					arg2.accept(Items.BOWL);
					arg2.accept(Items.BRICK);
					arg2.accept(Items.NETHER_BRICK);
					arg2.accept(Items.PAPER);
					arg2.accept(Items.BOOK);
					arg2.accept(Items.FIREWORK_STAR);
					arg2.accept(Items.GLASS_BOTTLE);
					arg2.accept(Items.NETHER_WART);
					arg2.accept(Items.REDSTONE);
					arg2.accept(Items.GLOWSTONE_DUST);
					arg2.accept(Items.GUNPOWDER);
					arg2.accept(Items.DRAGON_BREATH);
					arg2.accept(Items.FERMENTED_SPIDER_EYE);
					arg2.accept(Items.BLAZE_POWDER);
					arg2.accept(Items.SUGAR);
					arg2.accept(Items.RABBIT_FOOT);
					arg2.accept(Items.GLISTERING_MELON_SLICE);
					arg2.accept(Items.SPIDER_EYE);
					arg2.accept(Items.PUFFERFISH);
					arg2.accept(Items.MAGMA_CREAM);
					arg2.accept(Items.GOLDEN_CARROT);
					arg2.accept(Items.GHAST_TEAR);
					arg2.accept(Items.TURTLE_HELMET);
					arg2.accept(Items.PHANTOM_MEMBRANE);
					arg2.accept(Items.FIELD_MASONED_BANNER_PATTERN);
					arg2.accept(Items.BORDURE_INDENTED_BANNER_PATTERN);
					arg2.accept(Items.FLOWER_BANNER_PATTERN);
					arg2.accept(Items.CREEPER_BANNER_PATTERN);
					arg2.accept(Items.SKULL_BANNER_PATTERN);
					arg2.accept(Items.MOJANG_BANNER_PATTERN);
					arg2.accept(Items.GLOBE_BANNER_PATTERN);
					arg2.accept(Items.PIGLIN_BANNER_PATTERN);
					arg2.accept(Items.FLOW_BANNER_PATTERN);
					arg2.accept(Items.GUSTER_BANNER_PATTERN);
					arg2.accept(Items.ANGLER_POTTERY_SHERD);
					arg2.accept(Items.ARCHER_POTTERY_SHERD);
					arg2.accept(Items.ARMS_UP_POTTERY_SHERD);
					arg2.accept(Items.BLADE_POTTERY_SHERD);
					arg2.accept(Items.BREWER_POTTERY_SHERD);
					arg2.accept(Items.BURN_POTTERY_SHERD);
					arg2.accept(Items.DANGER_POTTERY_SHERD);
					arg2.accept(Items.FLOW_POTTERY_SHERD);
					arg2.accept(Items.EXPLORER_POTTERY_SHERD);
					arg2.accept(Items.FRIEND_POTTERY_SHERD);
					arg2.accept(Items.GUSTER_POTTERY_SHERD);
					arg2.accept(Items.HEART_POTTERY_SHERD);
					arg2.accept(Items.HEARTBREAK_POTTERY_SHERD);
					arg2.accept(Items.HOWL_POTTERY_SHERD);
					arg2.accept(Items.MINER_POTTERY_SHERD);
					arg2.accept(Items.MOURNER_POTTERY_SHERD);
					arg2.accept(Items.PLENTY_POTTERY_SHERD);
					arg2.accept(Items.PRIZE_POTTERY_SHERD);
					arg2.accept(Items.SCRAPE_POTTERY_SHERD);
					arg2.accept(Items.SHEAF_POTTERY_SHERD);
					arg2.accept(Items.SHELTER_POTTERY_SHERD);
					arg2.accept(Items.SKULL_POTTERY_SHERD);
					arg2.accept(Items.SNORT_POTTERY_SHERD);
					arg2.accept(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
					arg2.accept(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
					arg2.accept(Items.EXPERIENCE_BOTTLE);
					arg2.accept(Items.TRIAL_KEY);
					arg2.accept(Items.OMINOUS_TRIAL_KEY);
					argx.holders().lookup(Registries.ENCHANTMENT).ifPresent(arg2x -> {
						generateEnchantmentBookTypesOnlyMaxLevel(arg2, arg2x, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
						generateEnchantmentBookTypesAllLevels(arg2, arg2x, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
					});
				}))
				.build()
		);
		Registry.register(
			arg,
			SPAWN_EGGS,
			CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 4)
				.title(Component.translatable("itemGroup.spawnEggs"))
				.icon(() -> new ItemStack(Items.PIG_SPAWN_EGG))
				.displayItems((CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
					arg2.accept(Items.SPAWNER);
					arg2.accept(Items.TRIAL_SPAWNER);
					arg2.accept(Items.CREAKING_HEART);
					arg2.accept(Items.ALLAY_SPAWN_EGG);
					arg2.accept(Items.ARMADILLO_SPAWN_EGG);
					arg2.accept(Items.AXOLOTL_SPAWN_EGG);
					arg2.accept(Items.BAT_SPAWN_EGG);
					arg2.accept(Items.BEE_SPAWN_EGG);
					arg2.accept(Items.BLAZE_SPAWN_EGG);
					arg2.accept(Items.BOGGED_SPAWN_EGG);
					arg2.accept(Items.BREEZE_SPAWN_EGG);
					arg2.accept(Items.CAMEL_SPAWN_EGG);
					arg2.accept(Items.CAT_SPAWN_EGG);
					arg2.accept(Items.CAVE_SPIDER_SPAWN_EGG);
					arg2.accept(Items.CHICKEN_SPAWN_EGG);
					arg2.accept(Items.COD_SPAWN_EGG);
					arg2.accept(Items.COW_SPAWN_EGG);
					arg2.accept(Items.CREAKING_SPAWN_EGG);
					arg2.accept(Items.CREEPER_SPAWN_EGG);
					arg2.accept(Items.DOLPHIN_SPAWN_EGG);
					arg2.accept(Items.DONKEY_SPAWN_EGG);
					arg2.accept(Items.DROWNED_SPAWN_EGG);
					arg2.accept(Items.ELDER_GUARDIAN_SPAWN_EGG);
					arg2.accept(Items.ENDERMAN_SPAWN_EGG);
					arg2.accept(Items.ENDERMITE_SPAWN_EGG);
					arg2.accept(Items.EVOKER_SPAWN_EGG);
					arg2.accept(Items.FOX_SPAWN_EGG);
					arg2.accept(Items.FROG_SPAWN_EGG);
					arg2.accept(Items.GHAST_SPAWN_EGG);
					arg2.accept(Items.GLOW_SQUID_SPAWN_EGG);
					arg2.accept(Items.GOAT_SPAWN_EGG);
					arg2.accept(Items.GUARDIAN_SPAWN_EGG);
					arg2.accept(Items.HOGLIN_SPAWN_EGG);
					arg2.accept(Items.HORSE_SPAWN_EGG);
					arg2.accept(Items.HUSK_SPAWN_EGG);
					arg2.accept(Items.IRON_GOLEM_SPAWN_EGG);
					arg2.accept(Items.LLAMA_SPAWN_EGG);
					arg2.accept(Items.MAGMA_CUBE_SPAWN_EGG);
					arg2.accept(Items.MOOSHROOM_SPAWN_EGG);
					arg2.accept(Items.MULE_SPAWN_EGG);
					arg2.accept(Items.OCELOT_SPAWN_EGG);
					arg2.accept(Items.PANDA_SPAWN_EGG);
					arg2.accept(Items.PARROT_SPAWN_EGG);
					arg2.accept(Items.PHANTOM_SPAWN_EGG);
					arg2.accept(Items.PIG_SPAWN_EGG);
					arg2.accept(Items.PIGLIN_SPAWN_EGG);
					arg2.accept(Items.PIGLIN_BRUTE_SPAWN_EGG);
					arg2.accept(Items.PILLAGER_SPAWN_EGG);
					arg2.accept(Items.POLAR_BEAR_SPAWN_EGG);
					arg2.accept(Items.PUFFERFISH_SPAWN_EGG);
					arg2.accept(Items.RABBIT_SPAWN_EGG);
					arg2.accept(Items.RAVAGER_SPAWN_EGG);
					arg2.accept(Items.SALMON_SPAWN_EGG);
					arg2.accept(Items.SHEEP_SPAWN_EGG);
					arg2.accept(Items.SHULKER_SPAWN_EGG);
					arg2.accept(Items.SILVERFISH_SPAWN_EGG);
					arg2.accept(Items.SKELETON_SPAWN_EGG);
					arg2.accept(Items.SKELETON_HORSE_SPAWN_EGG);
					arg2.accept(Items.SLIME_SPAWN_EGG);
					arg2.accept(Items.SNIFFER_SPAWN_EGG);
					arg2.accept(Items.SNOW_GOLEM_SPAWN_EGG);
					arg2.accept(Items.SPIDER_SPAWN_EGG);
					arg2.accept(Items.SQUID_SPAWN_EGG);
					arg2.accept(Items.STRAY_SPAWN_EGG);
					arg2.accept(Items.STRIDER_SPAWN_EGG);
					arg2.accept(Items.TADPOLE_SPAWN_EGG);
					arg2.accept(Items.TRADER_LLAMA_SPAWN_EGG);
					arg2.accept(Items.TROPICAL_FISH_SPAWN_EGG);
					arg2.accept(Items.TURTLE_SPAWN_EGG);
					arg2.accept(Items.VEX_SPAWN_EGG);
					arg2.accept(Items.VILLAGER_SPAWN_EGG);
					arg2.accept(Items.VINDICATOR_SPAWN_EGG);
					arg2.accept(Items.WANDERING_TRADER_SPAWN_EGG);
					arg2.accept(Items.WARDEN_SPAWN_EGG);
					arg2.accept(Items.WITCH_SPAWN_EGG);
					arg2.accept(Items.WITHER_SKELETON_SPAWN_EGG);
					arg2.accept(Items.WOLF_SPAWN_EGG);
					arg2.accept(Items.ZOGLIN_SPAWN_EGG);
					arg2.accept(Items.ZOMBIE_SPAWN_EGG);
					arg2.accept(Items.ZOMBIE_HORSE_SPAWN_EGG);
					arg2.accept(Items.ZOMBIE_VILLAGER_SPAWN_EGG);
					arg2.accept(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG);
				}))
				.build()
		);
		Registry.register(
			arg,
			OP_BLOCKS,
			CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 5)
				.title(Component.translatable("itemGroup.op"))
				.icon(() -> new ItemStack(Items.COMMAND_BLOCK))
				.alignedRight()
				.displayItems(
					(CreativeModeTab.DisplayItemsGenerator)((argx, arg2) -> {
						if (argx.hasPermissions()) {
							arg2.accept(Items.COMMAND_BLOCK);
							arg2.accept(Items.CHAIN_COMMAND_BLOCK);
							arg2.accept(Items.REPEATING_COMMAND_BLOCK);
							arg2.accept(Items.COMMAND_BLOCK_MINECART);
							arg2.accept(Items.JIGSAW);
							arg2.accept(Items.STRUCTURE_BLOCK);
							arg2.accept(Items.STRUCTURE_VOID);
							arg2.accept(Items.BARRIER);
							arg2.accept(Items.DEBUG_STICK);

							for (int i = 15; i >= 0; i--) {
								arg2.accept(LightBlock.setLightOnStack(new ItemStack(Items.LIGHT), i));
							}

							argx.holders()
								.lookup(Registries.PAINTING_VARIANT)
								.ifPresent(
									arg3 -> generatePresetPaintings(
										arg2, argx.holders(), arg3, argxxx -> !argxxx.is(PaintingVariantTags.PLACEABLE), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
									)
								);
						}
					})
				)
				.build()
		);
		return Registry.register(
			arg,
			INVENTORY,
			CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 6)
				.title(Component.translatable("itemGroup.inventory"))
				.icon(() -> new ItemStack(Blocks.CHEST))
				.backgroundTexture(INVENTORY_BACKGROUND)
				.hideTitle()
				.alignedRight()
				.type(CreativeModeTab.Type.INVENTORY)
				.noScrollBar()
				.build()
		);
	}

	public static void validate() {
		Map<Pair<CreativeModeTab.Row, Integer>, String> map = new HashMap();

		for (ResourceKey<CreativeModeTab> resourceKey : BuiltInRegistries.CREATIVE_MODE_TAB.registryKeySet()) {
			CreativeModeTab creativeModeTab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(resourceKey);
			String string = creativeModeTab.getDisplayName().getString();
			String string2 = (String)map.put(Pair.of(creativeModeTab.row(), creativeModeTab.column()), string);
			if (string2 != null) {
				throw new IllegalArgumentException("Duplicate position: " + string + " vs. " + string2);
			}
		}
	}

	public static CreativeModeTab getDefaultTab() {
		return BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(BUILDING_BLOCKS);
	}

	private static void generatePotionEffectTypes(
		CreativeModeTab.Output arg, HolderLookup<Potion> arg2, Item arg3, CreativeModeTab.TabVisibility arg4, FeatureFlagSet arg5
	) {
		arg2.listElements()
			.filter(arg2x -> ((Potion)arg2x.value()).isEnabled(arg5))
			.map(arg2x -> PotionContents.createItemStack(arg3, arg2x))
			.forEach(arg3x -> arg.accept(arg3x, arg4));
	}

	private static void generateEnchantmentBookTypesOnlyMaxLevel(CreativeModeTab.Output arg, HolderLookup<Enchantment> arg2, CreativeModeTab.TabVisibility arg3) {
		arg2.listElements()
			.map(argx -> EnchantmentHelper.createBook(new EnchantmentInstance(argx, ((Enchantment)argx.value()).getMaxLevel())))
			.forEach(arg3x -> arg.accept(arg3x, arg3));
	}

	private static void generateEnchantmentBookTypesAllLevels(CreativeModeTab.Output arg, HolderLookup<Enchantment> arg2, CreativeModeTab.TabVisibility arg3) {
		arg2.listElements()
			.flatMap(
				argx -> IntStream.rangeClosed(((Enchantment)argx.value()).getMinLevel(), ((Enchantment)argx.value()).getMaxLevel())
					.mapToObj(i -> EnchantmentHelper.createBook(new EnchantmentInstance(argx, i)))
			)
			.forEach(arg3x -> arg.accept(arg3x, arg3));
	}

	private static void generateInstrumentTypes(
		CreativeModeTab.Output arg, HolderLookup<Instrument> arg2, Item arg3, TagKey<Instrument> arg4, CreativeModeTab.TabVisibility arg5
	) {
		arg2.get(arg4).ifPresent(arg4x -> arg4x.stream().map(arg2xx -> InstrumentItem.create(arg3, arg2xx)).forEach(arg3xx -> arg.accept(arg3xx, arg5)));
	}

	private static void generateSuspiciousStews(CreativeModeTab.Output arg, CreativeModeTab.TabVisibility arg2) {
		List<SuspiciousEffectHolder> list = SuspiciousEffectHolder.getAllEffectHolders();
		Set<ItemStack> set = ItemStackLinkedSet.createTypeAndComponentsSet();

		for (SuspiciousEffectHolder suspiciousEffectHolder : list) {
			ItemStack itemStack = new ItemStack(Items.SUSPICIOUS_STEW);
			itemStack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, suspiciousEffectHolder.getSuspiciousEffects());
			set.add(itemStack);
		}

		arg.acceptAll(set, arg2);
	}

	private static void generateOminousBottles(CreativeModeTab.Output arg, CreativeModeTab.TabVisibility arg2) {
		for (int i = 0; i <= 4; i++) {
			ItemStack itemStack = new ItemStack(Items.OMINOUS_BOTTLE);
			itemStack.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(i));
			arg.accept(itemStack, arg2);
		}
	}

	private static void generateFireworksAllDurations(CreativeModeTab.Output arg, CreativeModeTab.TabVisibility arg2) {
		for (byte b : FireworkRocketItem.CRAFTABLE_DURATIONS) {
			ItemStack itemStack = new ItemStack(Items.FIREWORK_ROCKET);
			itemStack.set(DataComponents.FIREWORKS, new Fireworks(b, List.of()));
			arg.accept(itemStack, arg2);
		}
	}

	private static void generatePresetPaintings(
		CreativeModeTab.Output arg,
		HolderLookup.Provider arg2,
		HolderLookup.RegistryLookup<PaintingVariant> arg3,
		Predicate<Holder<PaintingVariant>> predicate,
		CreativeModeTab.TabVisibility arg4
	) {
		RegistryOps<Tag> registryOps = arg2.createSerializationContext(NbtOps.INSTANCE);
		arg3.listElements()
			.filter(predicate)
			.sorted(PAINTING_COMPARATOR)
			.forEach(
				arg4x -> {
					CustomData customData = CustomData.EMPTY
						.update(registryOps, Painting.VARIANT_MAP_CODEC, arg4x)
						.getOrThrow()
						.update(argxx -> argxx.putString("id", "minecraft:painting"));
					ItemStack itemStack = new ItemStack(Items.PAINTING);
					itemStack.set(DataComponents.ENTITY_DATA, customData);
					arg.accept(itemStack, arg4);
				}
			);
	}

	public static List<CreativeModeTab> tabs() {
		return streamAllTabs().filter(CreativeModeTab::shouldDisplay).toList();
	}

	public static List<CreativeModeTab> allTabs() {
		return streamAllTabs().toList();
	}

	private static Stream<CreativeModeTab> streamAllTabs() {
		return BuiltInRegistries.CREATIVE_MODE_TAB.stream();
	}

	public static CreativeModeTab searchTab() {
		return BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(SEARCH);
	}

	private static void buildAllTabContents(CreativeModeTab.ItemDisplayParameters arg) {
		streamAllTabs().filter(argx -> argx.getType() == CreativeModeTab.Type.CATEGORY).forEach(arg2 -> arg2.buildContents(arg));
		streamAllTabs().filter(argx -> argx.getType() != CreativeModeTab.Type.CATEGORY).forEach(arg2 -> arg2.buildContents(arg));
	}

	public static boolean tryRebuildTabContents(FeatureFlagSet arg, boolean bl, HolderLookup.Provider arg2) {
		if (CACHED_PARAMETERS != null && !CACHED_PARAMETERS.needsUpdate(arg, bl, arg2)) {
			return false;
		} else {
			CACHED_PARAMETERS = new CreativeModeTab.ItemDisplayParameters(arg, bl, arg2);
			buildAllTabContents(CACHED_PARAMETERS);
			return true;
		}
	}
}
