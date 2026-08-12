package net.minecraft.world.item.alchemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.brewing.BrewingRecipeRegistry;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

public class PotionBrewing {
	public static final int BREWING_TIME_SECONDS = 20;
	public static final PotionBrewing EMPTY = new PotionBrewing(List.of(), List.of(), List.of());
	private final List<Ingredient> containers;
	private final List<PotionBrewing.Mix<Potion>> potionMixes;
	private final List<PotionBrewing.Mix<Item>> containerMixes;
	private final BrewingRecipeRegistry registry;

	PotionBrewing(List<Ingredient> list, List<PotionBrewing.Mix<Potion>> list2, List<PotionBrewing.Mix<Item>> list3) {
		this(list, list2, list3, List.of());
	}

	PotionBrewing(List<Ingredient> list, List<PotionBrewing.Mix<Potion>> list2, List<PotionBrewing.Mix<Item>> list3, List<IBrewingRecipe> recipes) {
		this.containers = list;
		this.potionMixes = list2;
		this.containerMixes = list3;
		this.registry = new BrewingRecipeRegistry(recipes);
	}

	public boolean isIngredient(ItemStack arg) {
		return this.registry.isValidIngredient(arg) || this.isContainerIngredient(arg) || this.isPotionIngredient(arg);
	}

	public boolean isInput(ItemStack stack) {
		return this.registry.isValidInput(stack) || this.isContainer(stack);
	}

	public List<IBrewingRecipe> getRecipes() {
		return this.registry.recipes();
	}

	private boolean isContainer(ItemStack arg) {
		for (Ingredient ingredient : this.containers) {
			if (ingredient.test(arg)) {
				return true;
			}
		}

		return false;
	}

	public boolean isContainerIngredient(ItemStack arg) {
		for (PotionBrewing.Mix<Item> mix : this.containerMixes) {
			if (mix.ingredient.test(arg)) {
				return true;
			}
		}

		return false;
	}

	public boolean isPotionIngredient(ItemStack arg) {
		for (PotionBrewing.Mix<Potion> mix : this.potionMixes) {
			if (mix.ingredient.test(arg)) {
				return true;
			}
		}

		return false;
	}

	public boolean isBrewablePotion(Holder<Potion> arg) {
		for (PotionBrewing.Mix<Potion> mix : this.potionMixes) {
			if (mix.to.is(arg)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasMix(ItemStack arg, ItemStack arg2) {
		if (this.registry.hasOutput(arg, arg2)) {
			return true;
		} else {
			return !this.isContainer(arg) ? false : this.hasContainerMix(arg, arg2) || this.hasPotionMix(arg, arg2);
		}
	}

	public boolean hasContainerMix(ItemStack arg, ItemStack arg2) {
		for (PotionBrewing.Mix<Item> mix : this.containerMixes) {
			if (arg.is(mix.from) && mix.ingredient.test(arg2)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasPotionMix(ItemStack arg, ItemStack arg2) {
		Optional<Holder<Potion>> optional = arg.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
		if (optional.isEmpty()) {
			return false;
		} else {
			for (PotionBrewing.Mix<Potion> mix : this.potionMixes) {
				if (mix.from.is((Holder<Potion>)optional.get()) && mix.ingredient.test(arg2)) {
					return true;
				}
			}

			return false;
		}
	}

	public ItemStack mix(ItemStack arg, ItemStack arg2) {
		if (arg2.isEmpty()) {
			return arg2;
		} else {
			ItemStack customMix = this.registry.getOutput(arg2, arg);
			if (!customMix.isEmpty()) {
				return customMix;
			} else {
				Optional<Holder<Potion>> optional = arg2.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
				if (optional.isEmpty()) {
					return arg2;
				} else {
					for (PotionBrewing.Mix<Item> mix : this.containerMixes) {
						if (arg2.is(mix.from) && mix.ingredient.test(arg)) {
							return PotionContents.createItemStack(mix.to.value(), (Holder<Potion>)optional.get());
						}
					}

					for (PotionBrewing.Mix<Potion> mix1 : this.potionMixes) {
						if (mix1.from.is((Holder<Potion>)optional.get()) && mix1.ingredient.test(arg)) {
							return PotionContents.createItemStack(arg2.getItem(), mix1.to);
						}
					}

					return arg2;
				}
			}
		}
	}

	@Deprecated
	public static PotionBrewing bootstrap(FeatureFlagSet arg) {
		return bootstrap(arg, RegistryAccess.EMPTY);
	}

	public static PotionBrewing bootstrap(FeatureFlagSet arg, RegistryAccess registryAccess) {
		PotionBrewing.Builder potionbrewing$builder = new PotionBrewing.Builder(arg);
		addVanillaMixes(potionbrewing$builder);
		NeoForge.EVENT_BUS.post(new RegisterBrewingRecipesEvent(potionbrewing$builder, registryAccess));
		return potionbrewing$builder.build();
	}

	public static void addVanillaMixes(PotionBrewing.Builder arg) {
		arg.addContainer(Items.POTION);
		arg.addContainer(Items.SPLASH_POTION);
		arg.addContainer(Items.LINGERING_POTION);
		arg.addContainerRecipe(Items.POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
		arg.addContainerRecipe(Items.SPLASH_POTION, Items.DRAGON_BREATH, Items.LINGERING_POTION);
		arg.addMix(Potions.WATER, Items.GLOWSTONE_DUST, Potions.THICK);
		arg.addMix(Potions.WATER, Items.REDSTONE, Potions.MUNDANE);
		arg.addMix(Potions.WATER, Items.NETHER_WART, Potions.AWKWARD);
		arg.addStartMix(Items.BREEZE_ROD, Potions.WIND_CHARGED);
		arg.addStartMix(Items.SLIME_BLOCK, Potions.OOZING);
		arg.addStartMix(Items.STONE, Potions.INFESTED);
		arg.addStartMix(Items.COBWEB, Potions.WEAVING);
		arg.addMix(Potions.AWKWARD, Items.GOLDEN_CARROT, Potions.NIGHT_VISION);
		arg.addMix(Potions.NIGHT_VISION, Items.REDSTONE, Potions.LONG_NIGHT_VISION);
		arg.addMix(Potions.NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.INVISIBILITY);
		arg.addMix(Potions.LONG_NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.LONG_INVISIBILITY);
		arg.addMix(Potions.INVISIBILITY, Items.REDSTONE, Potions.LONG_INVISIBILITY);
		arg.addStartMix(Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE);
		arg.addMix(Potions.FIRE_RESISTANCE, Items.REDSTONE, Potions.LONG_FIRE_RESISTANCE);
		arg.addStartMix(Items.RABBIT_FOOT, Potions.LEAPING);
		arg.addMix(Potions.LEAPING, Items.REDSTONE, Potions.LONG_LEAPING);
		arg.addMix(Potions.LEAPING, Items.GLOWSTONE_DUST, Potions.STRONG_LEAPING);
		arg.addMix(Potions.LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
		arg.addMix(Potions.LONG_LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
		arg.addMix(Potions.SLOWNESS, Items.REDSTONE, Potions.LONG_SLOWNESS);
		arg.addMix(Potions.SLOWNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SLOWNESS);
		arg.addMix(Potions.AWKWARD, Items.TURTLE_HELMET, Potions.TURTLE_MASTER);
		arg.addMix(Potions.TURTLE_MASTER, Items.REDSTONE, Potions.LONG_TURTLE_MASTER);
		arg.addMix(Potions.TURTLE_MASTER, Items.GLOWSTONE_DUST, Potions.STRONG_TURTLE_MASTER);
		arg.addMix(Potions.SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
		arg.addMix(Potions.LONG_SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
		arg.addStartMix(Items.SUGAR, Potions.SWIFTNESS);
		arg.addMix(Potions.SWIFTNESS, Items.REDSTONE, Potions.LONG_SWIFTNESS);
		arg.addMix(Potions.SWIFTNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SWIFTNESS);
		arg.addMix(Potions.AWKWARD, Items.PUFFERFISH, Potions.WATER_BREATHING);
		arg.addMix(Potions.WATER_BREATHING, Items.REDSTONE, Potions.LONG_WATER_BREATHING);
		arg.addStartMix(Items.GLISTERING_MELON_SLICE, Potions.HEALING);
		arg.addMix(Potions.HEALING, Items.GLOWSTONE_DUST, Potions.STRONG_HEALING);
		arg.addMix(Potions.HEALING, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
		arg.addMix(Potions.STRONG_HEALING, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
		arg.addMix(Potions.HARMING, Items.GLOWSTONE_DUST, Potions.STRONG_HARMING);
		arg.addMix(Potions.POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
		arg.addMix(Potions.LONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
		arg.addMix(Potions.STRONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
		arg.addStartMix(Items.SPIDER_EYE, Potions.POISON);
		arg.addMix(Potions.POISON, Items.REDSTONE, Potions.LONG_POISON);
		arg.addMix(Potions.POISON, Items.GLOWSTONE_DUST, Potions.STRONG_POISON);
		arg.addStartMix(Items.GHAST_TEAR, Potions.REGENERATION);
		arg.addMix(Potions.REGENERATION, Items.REDSTONE, Potions.LONG_REGENERATION);
		arg.addMix(Potions.REGENERATION, Items.GLOWSTONE_DUST, Potions.STRONG_REGENERATION);
		arg.addStartMix(Items.BLAZE_POWDER, Potions.STRENGTH);
		arg.addMix(Potions.STRENGTH, Items.REDSTONE, Potions.LONG_STRENGTH);
		arg.addMix(Potions.STRENGTH, Items.GLOWSTONE_DUST, Potions.STRONG_STRENGTH);
		arg.addMix(Potions.WATER, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
		arg.addMix(Potions.WEAKNESS, Items.REDSTONE, Potions.LONG_WEAKNESS);
		arg.addMix(Potions.AWKWARD, Items.PHANTOM_MEMBRANE, Potions.SLOW_FALLING);
		arg.addMix(Potions.SLOW_FALLING, Items.REDSTONE, Potions.LONG_SLOW_FALLING);
	}

	public static class Builder {
		private final List<Ingredient> containers = new ArrayList();
		private final List<PotionBrewing.Mix<Potion>> potionMixes = new ArrayList();
		private final List<PotionBrewing.Mix<Item>> containerMixes = new ArrayList();
		private final List<IBrewingRecipe> recipes = new ArrayList();
		private final FeatureFlagSet enabledFeatures;

		public Builder(FeatureFlagSet arg) {
			this.enabledFeatures = arg;
		}

		private static void expectPotion(Item arg) {
			if (!(arg instanceof PotionItem)) {
				throw new IllegalArgumentException("Expected a potion, got: " + BuiltInRegistries.ITEM.getKey(arg));
			}
		}

		public void addContainerRecipe(Item arg, Item arg2, Item arg3) {
			if (arg.isEnabled(this.enabledFeatures) && arg2.isEnabled(this.enabledFeatures) && arg3.isEnabled(this.enabledFeatures)) {
				expectPotion(arg);
				expectPotion(arg3);
				this.containerMixes.add(new PotionBrewing.Mix<>(arg.builtInRegistryHolder(), Ingredient.of(arg2), arg3.builtInRegistryHolder()));
			}
		}

		public void addContainer(Item arg) {
			if (arg.isEnabled(this.enabledFeatures)) {
				expectPotion(arg);
				this.containers.add(Ingredient.of(arg));
			}
		}

		public void addMix(Holder<Potion> arg, Item arg2, Holder<Potion> arg3) {
			if (((Potion)arg.value()).isEnabled(this.enabledFeatures) && arg2.isEnabled(this.enabledFeatures) && ((Potion)arg3.value()).isEnabled(this.enabledFeatures)) {
				this.potionMixes.add(new PotionBrewing.Mix(arg, Ingredient.of(arg2), arg3));
			}
		}

		public void addStartMix(Item arg, Holder<Potion> arg2) {
			if (((Potion)arg2.value()).isEnabled(this.enabledFeatures)) {
				this.addMix(Potions.WATER, arg, Potions.MUNDANE);
				this.addMix(Potions.AWKWARD, arg, arg2);
			}
		}

		public void addRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
			this.addRecipe(new BrewingRecipe(input, ingredient, output));
		}

		public void addRecipe(IBrewingRecipe recipe) {
			this.recipes.add(recipe);
		}

		public PotionBrewing build() {
			return new PotionBrewing(List.copyOf(this.containers), List.copyOf(this.potionMixes), List.copyOf(this.containerMixes), List.copyOf(this.recipes));
		}
	}

	record Mix<T>(Holder<T> from, Ingredient ingredient, Holder<T> to) {
	}
}
