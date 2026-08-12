package net.minecraft.world.item.equipment.trim;

import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TrimPatterns {
	public static final ResourceKey<TrimPattern> SENTRY = registryKey("sentry");
	public static final ResourceKey<TrimPattern> DUNE = registryKey("dune");
	public static final ResourceKey<TrimPattern> COAST = registryKey("coast");
	public static final ResourceKey<TrimPattern> WILD = registryKey("wild");
	public static final ResourceKey<TrimPattern> WARD = registryKey("ward");
	public static final ResourceKey<TrimPattern> EYE = registryKey("eye");
	public static final ResourceKey<TrimPattern> VEX = registryKey("vex");
	public static final ResourceKey<TrimPattern> TIDE = registryKey("tide");
	public static final ResourceKey<TrimPattern> SNOUT = registryKey("snout");
	public static final ResourceKey<TrimPattern> RIB = registryKey("rib");
	public static final ResourceKey<TrimPattern> SPIRE = registryKey("spire");
	public static final ResourceKey<TrimPattern> WAYFINDER = registryKey("wayfinder");
	public static final ResourceKey<TrimPattern> SHAPER = registryKey("shaper");
	public static final ResourceKey<TrimPattern> SILENCE = registryKey("silence");
	public static final ResourceKey<TrimPattern> RAISER = registryKey("raiser");
	public static final ResourceKey<TrimPattern> HOST = registryKey("host");
	public static final ResourceKey<TrimPattern> FLOW = registryKey("flow");
	public static final ResourceKey<TrimPattern> BOLT = registryKey("bolt");

	public static void bootstrap(BootstrapContext<TrimPattern> arg) {
		register(arg, Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, SENTRY);
		register(arg, Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, DUNE);
		register(arg, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, COAST);
		register(arg, Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, WILD);
		register(arg, Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, WARD);
		register(arg, Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, EYE);
		register(arg, Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, VEX);
		register(arg, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, TIDE);
		register(arg, Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, SNOUT);
		register(arg, Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, RIB);
		register(arg, Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, SPIRE);
		register(arg, Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, WAYFINDER);
		register(arg, Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, SHAPER);
		register(arg, Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, SILENCE);
		register(arg, Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, RAISER);
		register(arg, Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, HOST);
		register(arg, Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, FLOW);
		register(arg, Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, BOLT);
	}

	public static Optional<Holder.Reference<TrimPattern>> getFromTemplate(HolderLookup.Provider arg, ItemStack arg2) {
		return arg.lookupOrThrow(Registries.TRIM_PATTERN).listElements().filter(arg2x -> arg2.is(((TrimPattern)arg2x.value()).templateItem())).findFirst();
	}

	public static void register(BootstrapContext<TrimPattern> arg, Item arg2, ResourceKey<TrimPattern> arg3) {
		TrimPattern trimPattern = new TrimPattern(
			arg3.location(), BuiltInRegistries.ITEM.wrapAsHolder(arg2), Component.translatable(Util.makeDescriptionId("trim_pattern", arg3.location())), false
		);
		arg.register(arg3, trimPattern);
	}

	private static ResourceKey<TrimPattern> registryKey(String string) {
		return ResourceKey.create(Registries.TRIM_PATTERN, ResourceLocation.withDefaultNamespace(string));
	}
}
