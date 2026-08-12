package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import org.slf4j.Logger;

public class RecipeManager extends SimplePreparableReloadListener<RecipeMap> implements RecipeAccess {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Map<ResourceKey<RecipePropertySet>, RecipeManager.IngredientExtractor> RECIPE_PROPERTY_SETS = Map.of(
		RecipePropertySet.SMITHING_ADDITION,
		(RecipeManager.IngredientExtractor)arg -> arg instanceof SmithingRecipe smithingrecipe ? smithingrecipe.additionIngredient() : Optional.empty(),
		RecipePropertySet.SMITHING_BASE,
		(RecipeManager.IngredientExtractor)arg -> arg instanceof SmithingRecipe smithingrecipe ? smithingrecipe.baseIngredient() : Optional.empty(),
		RecipePropertySet.SMITHING_TEMPLATE,
		(RecipeManager.IngredientExtractor)arg -> arg instanceof SmithingRecipe smithingrecipe ? smithingrecipe.templateIngredient() : Optional.empty(),
		RecipePropertySet.FURNACE_INPUT,
		forSingleInput(RecipeType.SMELTING),
		RecipePropertySet.BLAST_FURNACE_INPUT,
		forSingleInput(RecipeType.BLASTING),
		RecipePropertySet.SMOKER_INPUT,
		forSingleInput(RecipeType.SMOKING),
		RecipePropertySet.CAMPFIRE_INPUT,
		forSingleInput(RecipeType.CAMPFIRE_COOKING)
	);
	private final HolderLookup.Provider registries;
	private RecipeMap recipes = RecipeMap.EMPTY;
	private Map<ResourceKey<RecipePropertySet>, RecipePropertySet> propertySets = Map.of();
	private SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes = SelectableRecipe.SingleInputSet.empty();
	private List<RecipeManager.ServerDisplayInfo> allDisplays = List.of();
	private Map<ResourceKey<Recipe<?>>, List<RecipeManager.ServerDisplayInfo>> recipeToDisplay = Map.of();

	public RecipeManager(HolderLookup.Provider arg) {
		this.registries = arg;
	}

	protected RecipeMap prepare(ResourceManager arg, ProfilerFiller arg2) {
		SortedMap<ResourceLocation, Recipe<?>> sortedmap = new TreeMap();
		SimpleJsonResourceReloadListener.scanDirectory(
			arg,
			Registries.elementsDirPath(Registries.RECIPE),
			new ConditionalOps<>(this.registries.createSerializationContext(JsonOps.INSTANCE), this.getContext()),
			Recipe.CODEC,
			sortedmap
		);
		List<RecipeHolder<?>> list = new ArrayList(sortedmap.size());
		sortedmap.forEach((argx, arg2x) -> {
			ResourceKey<Recipe<?>> resourcekey = ResourceKey.create(Registries.RECIPE, argx);
			RecipeHolder<?> recipeholder = new RecipeHolder(resourcekey, arg2x);
			list.add(recipeholder);
		});
		return RecipeMap.create(list);
	}

	protected void apply(RecipeMap arg, ResourceManager arg2, ProfilerFiller arg3) {
		this.recipes = arg;
		LOGGER.info("Loaded {} recipes", arg.values().size());
	}

	public void finalizeRecipeLoading(FeatureFlagSet arg) {
		List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> list = new ArrayList();
		List<RecipeManager.IngredientCollector> list1 = RECIPE_PROPERTY_SETS.entrySet()
			.stream()
			.map(entry -> new RecipeManager.IngredientCollector((ResourceKey<RecipePropertySet>)entry.getKey(), (RecipeManager.IngredientExtractor)entry.getValue()))
			.toList();
		this.recipes
			.values()
			.forEach(
				arg2 -> {
					Recipe<?> recipe = arg2.value();
					if (!recipe.isSpecial() && recipe.placementInfo().isImpossibleToPlace()) {
						LOGGER.warn("Recipe {} can't be placed due to empty ingredients and will be ignored", arg2.id().location());
					} else {
						list1.forEach(argxx -> argxx.accept(recipe));
						if (recipe instanceof StonecutterRecipe stonecutterrecipe
							&& isIngredientEnabled(arg, stonecutterrecipe.input())
							&& stonecutterrecipe.resultDisplay().isEnabled(arg)) {
							list.add(new SelectableRecipe.SingleInputEntry(stonecutterrecipe.input(), new SelectableRecipe(stonecutterrecipe.resultDisplay(), Optional.of(arg2))));
						}
					}
				}
			);
		this.propertySets = (Map<ResourceKey<RecipePropertySet>, RecipePropertySet>)list1.stream()
			.collect(Collectors.toUnmodifiableMap(argx -> argx.key, arg2 -> arg2.asPropertySet(arg)));
		this.stonecutterRecipes = new SelectableRecipe.SingleInputSet<>(list);
		this.allDisplays = unpackRecipeInfo(this.recipes.values(), arg);
		this.recipeToDisplay = (Map<ResourceKey<Recipe<?>>, List<RecipeManager.ServerDisplayInfo>>)this.allDisplays
			.stream()
			.collect(Collectors.groupingBy(argx -> argx.parent.id(), IdentityHashMap::new, Collectors.toList()));
	}

	static List<Ingredient> filterDisabled(FeatureFlagSet arg, List<Ingredient> list) {
		list.removeIf(arg2 -> !isIngredientEnabled(arg, arg2));
		return list;
	}

	private static boolean isIngredientEnabled(FeatureFlagSet arg, Ingredient arg2) {
		return arg2.items().stream().allMatch(arg2x -> ((Item)arg2x.value()).isEnabled(arg));
	}

	public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
		RecipeType<T> arg, I arg2, Level arg3, @Nullable ResourceKey<Recipe<?>> arg4
	) {
		RecipeHolder<T> recipeholder = arg4 != null ? this.byKeyTyped(arg, arg4) : null;
		return this.getRecipeFor(arg, arg2, arg3, recipeholder);
	}

	public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
		RecipeType<T> arg, I arg2, Level arg3, @Nullable RecipeHolder<T> arg4
	) {
		return arg4 != null && arg4.value().matches(arg2, arg3) ? Optional.of(arg4) : this.getRecipeFor(arg, arg2, arg3);
	}

	public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> arg, I arg2, Level arg3) {
		return this.recipes.getRecipesFor(arg, arg2, arg3).findFirst();
	}

	public Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> arg) {
		return Optional.ofNullable(this.recipes.byKey(arg));
	}

	@Nullable
	private <T extends Recipe<?>> RecipeHolder<T> byKeyTyped(RecipeType<T> arg, ResourceKey<Recipe<?>> arg2) {
		RecipeHolder<?> recipeholder = this.recipes.byKey(arg2);
		return (RecipeHolder<T>)(recipeholder != null && recipeholder.value().getType().equals(arg) ? recipeholder : null);
	}

	public Map<ResourceKey<RecipePropertySet>, RecipePropertySet> getSynchronizedItemProperties() {
		return this.propertySets;
	}

	public SelectableRecipe.SingleInputSet<StonecutterRecipe> getSynchronizedStonecutterRecipes() {
		return this.stonecutterRecipes;
	}

	@Override
	public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> arg) {
		return (RecipePropertySet)this.propertySets.getOrDefault(arg, RecipePropertySet.EMPTY);
	}

	@Override
	public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
		return this.stonecutterRecipes;
	}

	public Collection<RecipeHolder<?>> getRecipes() {
		return this.recipes.values();
	}

	@Nullable
	public RecipeManager.ServerDisplayInfo getRecipeFromDisplay(RecipeDisplayId arg) {
		return (RecipeManager.ServerDisplayInfo)this.allDisplays.get(arg.index());
	}

	public void listDisplaysForRecipe(ResourceKey<Recipe<?>> arg, Consumer<RecipeDisplayEntry> consumer) {
		List<RecipeManager.ServerDisplayInfo> list = (List<RecipeManager.ServerDisplayInfo>)this.recipeToDisplay.get(arg);
		if (list != null) {
			list.forEach(argx -> consumer.accept(argx.display));
		}
	}

	@VisibleForTesting
	protected static RecipeHolder<?> fromJson(ResourceKey<Recipe<?>> arg, JsonObject jsonObject, HolderLookup.Provider arg2) {
		Recipe<?> recipe = Recipe.CODEC.parse(arg2.createSerializationContext(JsonOps.INSTANCE), jsonObject).getOrThrow(JsonParseException::new);
		return new RecipeHolder<>(arg, recipe);
	}

	public static <I extends RecipeInput, T extends Recipe<I>> RecipeManager.CachedCheck<I, T> createCheck(RecipeType<T> arg) {
		return new RecipeManager.CachedCheck<I, T>() {
			@Nullable
			private ResourceKey<Recipe<?>> lastRecipe;

			@Override
			public Optional<RecipeHolder<T>> getRecipeFor(I arg, ServerLevel arg2) {
				RecipeManager recipemanager = arg2.recipeAccess();
				Optional<RecipeHolder<T>> optional = recipemanager.getRecipeFor(arg, arg, arg2, this.lastRecipe);
				if (optional.isPresent()) {
					RecipeHolder<T> recipeholder = (RecipeHolder<T>)optional.get();
					this.lastRecipe = recipeholder.id();
					return Optional.of(recipeholder);
				} else {
					return Optional.empty();
				}
			}
		};
	}

	private static List<RecipeManager.ServerDisplayInfo> unpackRecipeInfo(Iterable<RecipeHolder<?>> iterable, FeatureFlagSet arg) {
		List<RecipeManager.ServerDisplayInfo> list = new ArrayList();
		Object2IntMap<String> object2intmap = new Object2IntOpenHashMap<>();

		for (RecipeHolder<?> recipeholder : iterable) {
			Recipe<?> recipe = recipeholder.value();
			OptionalInt optionalint;
			if (recipe.group().isEmpty()) {
				optionalint = OptionalInt.empty();
			} else {
				optionalint = OptionalInt.of(object2intmap.computeIfAbsent(recipe.group(), object -> object2intmap.size()));
			}

			Optional<List<Ingredient>> optional;
			if (recipe.isSpecial()) {
				optional = Optional.empty();
			} else {
				optional = Optional.of(recipe.placementInfo().ingredients());
			}

			for (RecipeDisplay recipedisplay : recipe.display()) {
				if (recipedisplay.isEnabled(arg)) {
					int i = list.size();
					RecipeDisplayId recipedisplayid = new RecipeDisplayId(i);
					RecipeDisplayEntry recipedisplayentry = new RecipeDisplayEntry(recipedisplayid, recipedisplay, optionalint, recipe.recipeBookCategory(), optional);
					list.add(new RecipeManager.ServerDisplayInfo(recipedisplayentry, recipeholder));
				}
			}
		}

		return list;
	}

	private static RecipeManager.IngredientExtractor forSingleInput(RecipeType<? extends SingleItemRecipe> arg) {
		return arg2 -> arg2.getType() == arg && arg2 instanceof SingleItemRecipe singleitemrecipe ? Optional.of(singleitemrecipe.input()) : Optional.empty();
	}

	public RecipeMap recipeMap() {
		return this.recipes;
	}

	public interface CachedCheck<I extends RecipeInput, T extends Recipe<I>> {
		Optional<RecipeHolder<T>> getRecipeFor(I arg, ServerLevel arg2);
	}

	public static class IngredientCollector implements Consumer<Recipe<?>> {
		final ResourceKey<RecipePropertySet> key;
		private final RecipeManager.IngredientExtractor extractor;
		private final List<Ingredient> ingredients = new ArrayList();

		protected IngredientCollector(ResourceKey<RecipePropertySet> arg, RecipeManager.IngredientExtractor arg2) {
			this.key = arg;
			this.extractor = arg2;
		}

		public void accept(Recipe<?> arg) {
			this.extractor.apply(arg).ifPresent(this.ingredients::add);
		}

		public RecipePropertySet asPropertySet(FeatureFlagSet arg) {
			return RecipePropertySet.create(RecipeManager.filterDisabled(arg, this.ingredients));
		}
	}

	@FunctionalInterface
	public interface IngredientExtractor {
		Optional<Ingredient> apply(Recipe<?> arg);
	}

	public record ServerDisplayInfo(RecipeDisplayEntry display, RecipeHolder<?> parent) {
	}
}
