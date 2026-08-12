package net.minecraft.world.item.crafting;

import com.mojang.datafixers.util.Pair;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public class RepairItemRecipe extends CustomRecipe {
	public RepairItemRecipe(CraftingBookCategory arg) {
		super(arg);
	}

	@Nullable
	private static Pair<ItemStack, ItemStack> getItemsToCombine(CraftingInput arg) {
		if (arg.ingredientCount() != 2) {
			return null;
		} else {
			ItemStack itemstack = null;

			for (int i = 0; i < arg.size(); i++) {
				ItemStack itemstack1 = arg.getItem(i);
				if (!itemstack1.isEmpty()) {
					if (itemstack != null) {
						return canCombine(itemstack, itemstack1) ? Pair.of(itemstack, itemstack1) : null;
					}

					itemstack = itemstack1;
				}
			}

			return null;
		}
	}

	private static boolean canCombine(ItemStack arg, ItemStack arg2) {
		return arg2.is(arg.getItem())
			&& arg.getCount() == 1
			&& arg2.getCount() == 1
			&& arg.has(DataComponents.MAX_DAMAGE)
			&& arg2.has(DataComponents.MAX_DAMAGE)
			&& arg.has(DataComponents.DAMAGE)
			&& arg2.has(DataComponents.DAMAGE)
			&& arg.isRepairable()
			&& arg2.isRepairable();
	}

	public boolean matches(CraftingInput arg, Level arg2) {
		return getItemsToCombine(arg) != null;
	}

	public ItemStack assemble(CraftingInput arg, HolderLookup.Provider arg2) {
		Pair<ItemStack, ItemStack> pair = getItemsToCombine(arg);
		if (pair == null) {
			return ItemStack.EMPTY;
		} else {
			ItemStack itemstack = pair.getFirst();
			ItemStack itemstack1 = pair.getSecond();
			int i = Math.max(itemstack.getMaxDamage(), itemstack1.getMaxDamage());
			int j = itemstack.getMaxDamage() - itemstack.getDamageValue();
			int k = itemstack1.getMaxDamage() - itemstack1.getDamageValue();
			int l = j + k + i * 5 / 100;
			ItemStack itemstack2 = new ItemStack(itemstack.getItem());
			itemstack2.set(DataComponents.MAX_DAMAGE, i);
			itemstack2.setDamageValue(Math.max(i - l, 0));
			ItemEnchantments itemenchantments = EnchantmentHelper.getEnchantmentsForCrafting(itemstack);
			ItemEnchantments itemenchantments1 = EnchantmentHelper.getEnchantmentsForCrafting(itemstack1);
			EnchantmentHelper.updateEnchantments(
				itemstack2, arg2x -> arg2.lookupOrThrow(Registries.ENCHANTMENT).listElements().filter(argxx -> argxx.is(EnchantmentTags.CURSE)).forEach(arg2xx -> {
					int i1 = Math.max(itemenchantments.getLevel(arg2xx), itemenchantments1.getLevel(arg2xx));
					if (i1 > 0) {
						arg2x.upgrade(arg2xx, i1);
					}
				})
			);
			return itemstack2;
		}
	}

	@Override
	public RecipeSerializer<RepairItemRecipe> getSerializer() {
		return RecipeSerializer.REPAIR_ITEM;
	}
}
