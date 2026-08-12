package net.minecraft.world.item.equipment.trim;

import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentModels;

public class TrimMaterials {
	public static final ResourceKey<TrimMaterial> QUARTZ = registryKey("quartz");
	public static final ResourceKey<TrimMaterial> IRON = registryKey("iron");
	public static final ResourceKey<TrimMaterial> NETHERITE = registryKey("netherite");
	public static final ResourceKey<TrimMaterial> REDSTONE = registryKey("redstone");
	public static final ResourceKey<TrimMaterial> COPPER = registryKey("copper");
	public static final ResourceKey<TrimMaterial> GOLD = registryKey("gold");
	public static final ResourceKey<TrimMaterial> EMERALD = registryKey("emerald");
	public static final ResourceKey<TrimMaterial> DIAMOND = registryKey("diamond");
	public static final ResourceKey<TrimMaterial> LAPIS = registryKey("lapis");
	public static final ResourceKey<TrimMaterial> AMETHYST = registryKey("amethyst");

	public static void bootstrap(BootstrapContext<TrimMaterial> arg) {
		register(arg, QUARTZ, Items.QUARTZ, Style.EMPTY.withColor(14931140), 0.1F);
		register(arg, IRON, Items.IRON_INGOT, Style.EMPTY.withColor(15527148), 0.2F, Map.of(EquipmentModels.IRON, "iron_darker"));
		register(arg, NETHERITE, Items.NETHERITE_INGOT, Style.EMPTY.withColor(6445145), 0.3F, Map.of(EquipmentModels.NETHERITE, "netherite_darker"));
		register(arg, REDSTONE, Items.REDSTONE, Style.EMPTY.withColor(9901575), 0.4F);
		register(arg, COPPER, Items.COPPER_INGOT, Style.EMPTY.withColor(11823181), 0.5F);
		register(arg, GOLD, Items.GOLD_INGOT, Style.EMPTY.withColor(14594349), 0.6F, Map.of(EquipmentModels.GOLD, "gold_darker"));
		register(arg, EMERALD, Items.EMERALD, Style.EMPTY.withColor(1155126), 0.7F);
		register(arg, DIAMOND, Items.DIAMOND, Style.EMPTY.withColor(7269586), 0.8F, Map.of(EquipmentModels.DIAMOND, "diamond_darker"));
		register(arg, LAPIS, Items.LAPIS_LAZULI, Style.EMPTY.withColor(4288151), 0.9F);
		register(arg, AMETHYST, Items.AMETHYST_SHARD, Style.EMPTY.withColor(10116294), 1.0F);
	}

	public static Optional<Holder.Reference<TrimMaterial>> getFromIngredient(HolderLookup.Provider arg, ItemStack arg2) {
		return arg.lookupOrThrow(Registries.TRIM_MATERIAL).listElements().filter(arg2x -> arg2.is(((TrimMaterial)arg2x.value()).ingredient())).findFirst();
	}

	private static void register(BootstrapContext<TrimMaterial> arg, ResourceKey<TrimMaterial> arg2, Item arg3, Style arg4, float f) {
		register(arg, arg2, arg3, arg4, f, Map.of());
	}

	private static void register(
		BootstrapContext<TrimMaterial> arg, ResourceKey<TrimMaterial> arg2, Item arg3, Style arg4, float f, Map<ResourceLocation, String> map
	) {
		TrimMaterial trimMaterial = TrimMaterial.create(
			arg2.location().getPath(), arg3, f, Component.translatable(Util.makeDescriptionId("trim_material", arg2.location())).withStyle(arg4), map
		);
		arg.register(arg2, trimMaterial);
	}

	private static ResourceKey<TrimMaterial> registryKey(String string) {
		return ResourceKey.create(Registries.TRIM_MATERIAL, ResourceLocation.withDefaultNamespace(string));
	}
}
