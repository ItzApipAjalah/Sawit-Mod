package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
	protected static final int SLOT_INPUT = 0;
	protected static final int SLOT_FUEL = 1;
	protected static final int SLOT_RESULT = 2;
	public static final int DATA_LIT_TIME = 0;
	private static final int[] SLOTS_FOR_UP = new int[]{0};
	private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
	private static final int[] SLOTS_FOR_SIDES = new int[]{1};
	public static final int DATA_LIT_DURATION = 1;
	public static final int DATA_COOKING_PROGRESS = 2;
	public static final int DATA_COOKING_TOTAL_TIME = 3;
	public static final int NUM_DATA_VALUES = 4;
	public static final int BURN_TIME_STANDARD = 200;
	public static final int BURN_COOL_SPEED = 2;
	public static final int UNKNOWN_LIT_DURATION = 0;
	private final RecipeType<? extends AbstractCookingRecipe> recipeType;
	protected NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
	int litTime;
	int litDuration = 0;
	int cookingProgress;
	int cookingTotalTime;
	protected final ContainerData dataAccess = new ContainerData() {
		@Override
		public int get(int i) {
			switch (i) {
				case 0:
					if (AbstractFurnaceBlockEntity.this.litDuration > 32767) {
						return Mth.floor((double)AbstractFurnaceBlockEntity.this.litTime / AbstractFurnaceBlockEntity.this.litDuration * 32767.0);
					}

					return AbstractFurnaceBlockEntity.this.litTime;
				case 1:
					return Math.min(AbstractFurnaceBlockEntity.this.litDuration, 32767);
				case 2:
					return AbstractFurnaceBlockEntity.this.cookingProgress;
				case 3:
					return AbstractFurnaceBlockEntity.this.cookingTotalTime;
				default:
					return 0;
			}
		}

		@Override
		public void set(int i, int j) {
			switch (i) {
				case 0:
					AbstractFurnaceBlockEntity.this.litTime = j;
					break;
				case 1:
					AbstractFurnaceBlockEntity.this.litDuration = j;
					break;
				case 2:
					AbstractFurnaceBlockEntity.this.cookingProgress = j;
					break;
				case 3:
					AbstractFurnaceBlockEntity.this.cookingTotalTime = j;
			}
		}

		@Override
		public int getCount() {
			return 4;
		}
	};
	private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap<>();
	private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> quickCheck;

	protected AbstractFurnaceBlockEntity(BlockEntityType<?> arg, BlockPos arg2, BlockState arg3, RecipeType<? extends AbstractCookingRecipe> arg4) {
		super(arg, arg2, arg3);
		this.quickCheck = RecipeManager.createCheck(arg4);
		this.recipeType = arg4;
	}

	private boolean isLit() {
		return this.litTime > 0;
	}

	@Override
	protected void loadAdditional(CompoundTag arg, HolderLookup.Provider arg2) {
		super.loadAdditional(arg, arg2);
		this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(arg, this.items, arg2);
		this.litTime = arg.getInt("BurnTime");
		this.cookingProgress = arg.getInt("CookTime");
		this.cookingTotalTime = arg.getInt("CookTimeTotal");
		this.litDuration = 0;
		CompoundTag compoundtag = arg.getCompound("RecipesUsed");

		for (String s : compoundtag.getAllKeys()) {
			this.recipesUsed.put(ResourceKey.create(Registries.RECIPE, ResourceLocation.parse(s)), compoundtag.getInt(s));
		}
	}

	@Override
	protected void saveAdditional(CompoundTag arg, HolderLookup.Provider arg2) {
		super.saveAdditional(arg, arg2);
		arg.putInt("BurnTime", this.litTime);
		arg.putInt("CookTime", this.cookingProgress);
		arg.putInt("CookTimeTotal", this.cookingTotalTime);
		ContainerHelper.saveAllItems(arg, this.items, arg2);
		CompoundTag compoundtag = new CompoundTag();
		this.recipesUsed.forEach((argx, integer) -> compoundtag.putInt(argx.location().toString(), integer));
		arg.put("RecipesUsed", compoundtag);
	}

	public static void serverTick(ServerLevel arg, BlockPos arg2, BlockState arg3, AbstractFurnaceBlockEntity arg4) {
		boolean flag = arg4.isLit();
		boolean flag1 = false;
		if (arg4.isLit()) {
			arg4.litTime--;
		}

		ItemStack itemstack = arg4.items.get(1);
		ItemStack itemstack1 = arg4.items.get(0);
		boolean flag2 = !itemstack1.isEmpty();
		boolean flag3 = !itemstack.isEmpty();
		if (arg4.litDuration == 0) {
			arg4.litDuration = arg4.getBurnDuration(arg.fuelValues(), itemstack);
		}

		if (arg4.isLit() || flag3 && flag2) {
			SingleRecipeInput singlerecipeinput = new SingleRecipeInput(itemstack1);
			RecipeHolder<? extends AbstractCookingRecipe> recipeholder;
			if (flag2) {
				recipeholder = (RecipeHolder<? extends AbstractCookingRecipe>)arg4.quickCheck.getRecipeFor(singlerecipeinput, arg).orElse(null);
			} else {
				recipeholder = null;
			}

			int i = arg4.getMaxStackSize();
			if (!arg4.isLit() && canBurn(arg.registryAccess(), recipeholder, singlerecipeinput, arg4.items, i)) {
				arg4.litTime = arg4.getBurnDuration(arg.fuelValues(), itemstack);
				arg4.litDuration = arg4.litTime;
				if (arg4.isLit()) {
					flag1 = true;
					ItemStack remainder = itemstack.getCraftingRemainder();
					if (!remainder.isEmpty()) {
						arg4.items.set(1, remainder);
					} else if (flag3) {
						Item item = itemstack.getItem();
						itemstack.shrink(1);
						if (itemstack.isEmpty()) {
							arg4.items.set(1, item.getCraftingRemainder());
						}
					}
				}
			}

			if (arg4.isLit() && canBurn(arg.registryAccess(), recipeholder, singlerecipeinput, arg4.items, i)) {
				arg4.cookingProgress++;
				if (arg4.cookingProgress == arg4.cookingTotalTime) {
					arg4.cookingProgress = 0;
					arg4.cookingTotalTime = getTotalCookTime(arg, arg4);
					if (burn(arg.registryAccess(), recipeholder, singlerecipeinput, arg4.items, i)) {
						arg4.setRecipeUsed(recipeholder);
					}

					flag1 = true;
				}
			} else {
				arg4.cookingProgress = 0;
			}
		} else if (!arg4.isLit() && arg4.cookingProgress > 0) {
			arg4.cookingProgress = Mth.clamp(arg4.cookingProgress - 2, 0, arg4.cookingTotalTime);
		}

		if (flag != arg4.isLit()) {
			flag1 = true;
			arg3 = arg3.setValue(AbstractFurnaceBlock.LIT, arg4.isLit());
			arg.setBlock(arg2, arg3, 3);
		}

		if (flag1) {
			setChanged(arg, arg2, arg3);
		}
	}

	private static boolean canBurn(
		RegistryAccess arg, @Nullable RecipeHolder<? extends AbstractCookingRecipe> arg2, SingleRecipeInput arg3, NonNullList<ItemStack> arg4, int i
	) {
		if (!((ItemStack)arg4.get(0)).isEmpty() && arg2 != null) {
			ItemStack itemstack = ((AbstractCookingRecipe)arg2.value()).assemble(arg3, arg);
			if (itemstack.isEmpty()) {
				return false;
			} else {
				ItemStack itemstack1 = (ItemStack)arg4.get(2);
				if (itemstack1.isEmpty()) {
					return true;
				} else if (!ItemStack.isSameItemSameComponents(itemstack1, itemstack)) {
					return false;
				} else {
					return itemstack1.getCount() + itemstack.getCount() <= i && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize()
						? true
						: itemstack1.getCount() + itemstack.getCount() <= itemstack.getMaxStackSize();
				}
			}
		} else {
			return false;
		}
	}

	private static boolean burn(
		RegistryAccess arg, @Nullable RecipeHolder<? extends AbstractCookingRecipe> arg2, SingleRecipeInput arg3, NonNullList<ItemStack> arg4, int i
	) {
		if (arg2 != null && canBurn(arg, arg2, arg3, arg4, i)) {
			ItemStack itemstack = (ItemStack)arg4.get(0);
			ItemStack itemstack1 = ((AbstractCookingRecipe)arg2.value()).assemble(arg3, arg);
			ItemStack itemstack2 = (ItemStack)arg4.get(2);
			if (itemstack2.isEmpty()) {
				arg4.set(2, itemstack1.copy());
			} else if (ItemStack.isSameItemSameComponents(itemstack2, itemstack1)) {
				itemstack2.grow(itemstack1.getCount());
			}

			if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !((ItemStack)arg4.get(1)).isEmpty() && ((ItemStack)arg4.get(1)).is(Items.BUCKET)) {
				arg4.set(1, new ItemStack(Items.WATER_BUCKET));
			}

			itemstack.shrink(1);
			return true;
		} else {
			return false;
		}
	}

	protected int getBurnDuration(FuelValues arg, ItemStack arg2) {
		return arg2.getBurnTime(this.recipeType, arg);
	}

	private static int getTotalCookTime(ServerLevel arg, AbstractFurnaceBlockEntity arg2) {
		SingleRecipeInput singlerecipeinput = new SingleRecipeInput(arg2.getItem(0));
		return (Integer)arg2.quickCheck.getRecipeFor(singlerecipeinput, arg).map(argx -> ((AbstractCookingRecipe)argx.value()).cookingTime()).orElse(200);
	}

	@Override
	public int[] getSlotsForFace(Direction arg) {
		if (arg == Direction.DOWN) {
			return SLOTS_FOR_DOWN;
		} else {
			return arg == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
		}
	}

	@Override
	public boolean canPlaceItemThroughFace(int i, ItemStack arg, @Nullable Direction arg2) {
		return this.canPlaceItem(i, arg);
	}

	@Override
	public boolean canTakeItemThroughFace(int i, ItemStack arg, Direction arg2) {
		return arg2 == Direction.DOWN && i == 1 ? arg.is(Items.WATER_BUCKET) || arg.is(Items.BUCKET) : true;
	}

	@Override
	public int getContainerSize() {
		return this.items.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> arg) {
		this.items = arg;
	}

	@Override
	public void setItem(int i, ItemStack arg) {
		ItemStack itemstack = this.items.get(i);
		boolean flag = !arg.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, arg);
		this.items.set(i, arg);
		arg.limitSize(this.getMaxStackSize(arg));
		if (i == 0 && !flag && this.level instanceof ServerLevel serverlevel) {
			this.cookingTotalTime = getTotalCookTime(serverlevel, this);
			this.cookingProgress = 0;
			this.setChanged();
		}
	}

	@Override
	public boolean canPlaceItem(int i, ItemStack arg) {
		if (i == 2) {
			return false;
		} else if (i != 1) {
			return true;
		} else {
			ItemStack itemstack = this.items.get(1);
			return arg.getBurnTime(this.recipeType, this.level.fuelValues()) > 0 || arg.is(Items.BUCKET) && !itemstack.is(Items.BUCKET);
		}
	}

	@Override
	public void setRecipeUsed(@Nullable RecipeHolder<?> arg) {
		if (arg != null) {
			ResourceKey<Recipe<?>> resourcekey = arg.id();
			this.recipesUsed.addTo(resourcekey, 1);
		}
	}

	@Nullable
	@Override
	public RecipeHolder<?> getRecipeUsed() {
		return null;
	}

	@Override
	public void awardUsedRecipes(Player arg, List<ItemStack> list) {
	}

	public void awardUsedRecipesAndPopExperience(ServerPlayer arg) {
		List<RecipeHolder<?>> list = this.getRecipesToAwardAndPopExperience(arg.serverLevel(), arg.position());
		arg.awardRecipes(list);

		for (RecipeHolder<?> recipeholder : list) {
			if (recipeholder != null) {
				arg.triggerRecipeCrafted(recipeholder, this.items);
			}
		}

		this.recipesUsed.clear();
	}

	public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel arg, Vec3 arg2) {
		List<RecipeHolder<?>> list = Lists.<RecipeHolder<?>>newArrayList();

		for (Entry<ResourceKey<Recipe<?>>> entry : this.recipesUsed.reference2IntEntrySet()) {
			arg.recipeAccess().byKey((ResourceKey<Recipe<?>>)entry.getKey()).ifPresent(arg3 -> {
				list.add(arg3);
				createExperience(arg, arg2, entry.getIntValue(), ((AbstractCookingRecipe)arg3.value()).experience());
			});
		}

		return list;
	}

	private static void createExperience(ServerLevel arg, Vec3 arg2, int j, float g) {
		int i = Mth.floor(j * g);
		float f = Mth.frac(j * g);
		if (f != 0.0F && Math.random() < f) {
			i++;
		}

		ExperienceOrb.award(arg, arg2, i);
	}

	@Override
	public void fillStackedContents(StackedItemContents arg) {
		for (ItemStack itemstack : this.items) {
			arg.accountStack(itemstack);
		}
	}
}
