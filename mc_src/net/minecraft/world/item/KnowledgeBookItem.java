package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class KnowledgeBookItem extends Item {
	private static final Logger LOGGER = LogUtils.getLogger();

	public KnowledgeBookItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemStack = arg2.getItemInHand(arg3);
		List<ResourceKey<Recipe<?>>> list = itemStack.getOrDefault(DataComponents.RECIPES, List.of());
		itemStack.consume(1, arg2);
		if (list.isEmpty()) {
			return InteractionResult.FAIL;
		} else {
			if (!arg.isClientSide) {
				RecipeManager recipeManager = arg.getServer().getRecipeManager();
				List<RecipeHolder<?>> list2 = new ArrayList(list.size());

				for (ResourceKey<Recipe<?>> resourceKey : list) {
					Optional<RecipeHolder<?>> optional = recipeManager.byKey(resourceKey);
					if (!optional.isPresent()) {
						LOGGER.error("Invalid recipe: {}", resourceKey);
						return InteractionResult.FAIL;
					}

					list2.add((RecipeHolder)optional.get());
				}

				arg2.awardRecipes(list2);
				arg2.awardStat(Stats.ITEM_USED.get(this));
			}

			return InteractionResult.SUCCESS;
		}
	}
}
