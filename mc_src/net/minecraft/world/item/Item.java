package net.minecraft.world.item;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.neoforged.neoforge.common.extensions.IItemPropertiesExtensions;
import net.neoforged.neoforge.internal.RegistrationEvents;
import net.neoforged.neoforge.registries.GameData;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

public class Item implements FeatureElement, ItemLike, IItemExtension {
	public static final Codec<Holder<Item>> CODEC = BuiltInRegistries.ITEM
		.holderByNameCodec()
		.validate(arg -> arg.is(Items.AIR.builtInRegistryHolder()) ? DataResult.error(() -> "Item must not be minecraft:air") : DataResult.success(arg));
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Map<Block, Item> BY_BLOCK = GameData.getBlockItemMap();
	public static final ResourceLocation BASE_ATTACK_DAMAGE_ID = ResourceLocation.withDefaultNamespace("base_attack_damage");
	public static final ResourceLocation BASE_ATTACK_SPEED_ID = ResourceLocation.withDefaultNamespace("base_attack_speed");
	public static final int DEFAULT_MAX_STACK_SIZE = 64;
	public static final int ABSOLUTE_MAX_STACK_SIZE = 99;
	public static final int MAX_BAR_WIDTH = 13;
	private final Holder.Reference<Item> builtInRegistryHolder = BuiltInRegistries.ITEM.createIntrusiveHolder(this);
	private DataComponentMap components;
	@Nullable
	private final Item craftingRemainingItem;
	protected final String descriptionId;
	private final FeatureFlagSet requiredFeatures;

	public static int getId(Item arg) {
		return arg == null ? 0 : BuiltInRegistries.ITEM.getId(arg);
	}

	public static Item byId(int i) {
		return BuiltInRegistries.ITEM.byId(i);
	}

	@Deprecated
	public static Item byBlock(Block arg) {
		return (Item)BY_BLOCK.getOrDefault(arg, Items.AIR);
	}

	public Item(Item.Properties arg) {
		this.descriptionId = arg.effectiveDescriptionId();
		this.components = arg.buildAndValidateComponents(Component.translatable(this.descriptionId), arg.effectiveModel());
		this.craftingRemainingItem = arg.craftingRemainingItem;
		this.requiredFeatures = arg.requiredFeatures;
		if (SharedConstants.IS_RUNNING_IN_IDE) {
		}
	}

	@Deprecated
	public Holder.Reference<Item> builtInRegistryHolder() {
		return this.builtInRegistryHolder;
	}

	public DataComponentMap components() {
		return this.components;
	}

	@Deprecated
	@Internal
	public void modifyDefaultComponentsFrom(DataComponentPatch patch) {
		if (!RegistrationEvents.canModifyComponents()) {
			throw new IllegalStateException("Default components cannot be modified now!");
		} else {
			DataComponentMap.Builder builder = DataComponentMap.builder().addAll(this.components);
			patch.entrySet().forEach(entry -> builder.set((DataComponentType)entry.getKey(), ((Optional)entry.getValue()).orElse(null)));
			this.components = Item.Properties.validateComponents(builder.build());
		}
	}

	public int getDefaultMaxStackSize() {
		return this.components.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
	}

	public void onUseTick(Level arg, LivingEntity arg2, ItemStack arg3, int i) {
	}

	@Deprecated
	public void onDestroyed(ItemEntity arg) {
	}

	public void verifyComponentsAfterLoad(ItemStack arg) {
	}

	public boolean canAttackBlock(BlockState arg, Level arg2, BlockPos arg3, Player arg4) {
		return true;
	}

	@Override
	public Item asItem() {
		return this;
	}

	public InteractionResult useOn(UseOnContext arg) {
		return InteractionResult.PASS;
	}

	public float getDestroySpeed(ItemStack arg, BlockState arg2) {
		Tool tool = arg.get(DataComponents.TOOL);
		return tool != null ? tool.getMiningSpeed(arg2) : 1.0F;
	}

	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemstack = arg2.getItemInHand(arg3);
		Consumable consumable = itemstack.get(DataComponents.CONSUMABLE);
		if (consumable != null) {
			return consumable.startConsuming(arg2, itemstack, arg3);
		} else {
			Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
			return (InteractionResult)(equippable != null && equippable.swappable() ? equippable.swapWithEquipmentSlot(itemstack, arg2) : InteractionResult.PASS);
		}
	}

	public ItemStack finishUsingItem(ItemStack arg, Level arg2, LivingEntity arg3) {
		Consumable consumable = arg.get(DataComponents.CONSUMABLE);
		return consumable != null ? consumable.onConsume(arg2, arg3, arg) : arg;
	}

	public boolean isBarVisible(ItemStack arg) {
		return arg.isDamaged();
	}

	public int getBarWidth(ItemStack arg) {
		return Math.round(13.0F - arg.getDamageValue() * 13.0F / this.getMaxDamage(arg));
	}

	public int getBarColor(ItemStack arg) {
		int i = arg.getMaxDamage();
		float stackMaxDamage = this.getMaxDamage(arg);
		float f = Math.max(0.0F, (stackMaxDamage - arg.getDamageValue()) / stackMaxDamage);
		return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
	}

	public boolean overrideStackedOnOther(ItemStack arg, Slot arg2, ClickAction arg3, Player arg4) {
		return false;
	}

	public boolean overrideOtherStackedOnMe(ItemStack arg, ItemStack arg2, Slot arg3, ClickAction arg4, Player arg5, SlotAccess arg6) {
		return false;
	}

	public float getAttackDamageBonus(Entity arg, float f, DamageSource arg2) {
		return 0.0F;
	}

	@Nullable
	public DamageSource getDamageSource(LivingEntity arg) {
		return null;
	}

	public boolean hurtEnemy(ItemStack arg, LivingEntity arg2, LivingEntity arg3) {
		return false;
	}

	public void postHurtEnemy(ItemStack arg, LivingEntity arg2, LivingEntity arg3) {
	}

	public boolean mineBlock(ItemStack arg, Level arg2, BlockState arg3, BlockPos arg4, LivingEntity arg5) {
		Tool tool = arg.get(DataComponents.TOOL);
		if (tool == null) {
			return false;
		} else {
			if (!arg2.isClientSide && arg3.getDestroySpeed(arg2, arg4) != 0.0F && tool.damagePerBlock() > 0) {
				arg.hurtAndBreak(tool.damagePerBlock(), arg5, EquipmentSlot.MAINHAND);
			}

			return true;
		}
	}

	public boolean isCorrectToolForDrops(ItemStack arg, BlockState arg2) {
		Tool tool = arg.get(DataComponents.TOOL);
		return tool != null && tool.isCorrectForDrops(arg2);
	}

	public InteractionResult interactLivingEntity(ItemStack arg, Player arg2, LivingEntity arg3, InteractionHand arg4) {
		return InteractionResult.PASS;
	}

	public String toString() {
		return BuiltInRegistries.ITEM.wrapAsHolder(this).getRegisteredName();
	}

	@Deprecated
	public final ItemStack getCraftingRemainder() {
		return this.craftingRemainingItem == null ? ItemStack.EMPTY : new ItemStack(this.craftingRemainingItem);
	}

	public void inventoryTick(ItemStack arg, Level arg2, Entity arg3, int i, boolean bl) {
	}

	public void onCraftedBy(ItemStack arg, Level arg2, Player arg3) {
		this.onCraftedPostProcess(arg, arg2);
	}

	public void onCraftedPostProcess(ItemStack arg, Level arg2) {
	}

	public ItemUseAnimation getUseAnimation(ItemStack arg) {
		Consumable consumable = arg.get(DataComponents.CONSUMABLE);
		return consumable != null ? consumable.animation() : ItemUseAnimation.NONE;
	}

	public int getUseDuration(ItemStack arg, LivingEntity arg2) {
		Consumable consumable = arg.get(DataComponents.CONSUMABLE);
		return consumable != null ? consumable.consumeTicks() : 0;
	}

	public boolean releaseUsing(ItemStack arg, Level arg2, LivingEntity arg3, int i) {
		return false;
	}

	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
	}

	public Optional<TooltipComponent> getTooltipImage(ItemStack arg) {
		return Optional.empty();
	}

	@VisibleForTesting
	public final String getDescriptionId() {
		return this.descriptionId;
	}

	public final Component getName() {
		return this.components.getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
	}

	public Component getName(ItemStack arg) {
		return arg.getComponents().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
	}

	public boolean isFoil(ItemStack arg) {
		return arg.isEnchanted();
	}

	public static BlockHitResult getPlayerPOVHitResult(Level arg, Player arg2, ClipContext.Fluid arg3) {
		Vec3 vec3 = arg2.getEyePosition();
		Vec3 vec31 = vec3.add(arg2.calculateViewVector(arg2.getXRot(), arg2.getYRot()).scale(arg2.blockInteractionRange()));
		return arg.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, arg3, arg2));
	}

	public boolean useOnRelease(ItemStack arg) {
		return arg.getItem() == Items.CROSSBOW;
	}

	@Override
	public boolean isRepairable(ItemStack stack) {
		return stack.has(DataComponents.REPAIRABLE) && this.isDamageable(stack);
	}

	public ItemStack getDefaultInstance() {
		return new ItemStack(this);
	}

	public SoundEvent getBreakingSound() {
		return SoundEvents.ITEM_BREAK;
	}

	public boolean canFitInsideContainerItems() {
		return true;
	}

	@Override
	public FeatureFlagSet requiredFeatures() {
		return this.requiredFeatures;
	}

	public static class Properties implements IItemPropertiesExtensions {
		private static final DependantName<Item, String> BLOCK_DESCRIPTION_ID = arg -> Util.makeDescriptionId("block", arg.location());
		private static final DependantName<Item, String> ITEM_DESCRIPTION_ID = arg -> Util.makeDescriptionId("item", arg.location());
		private final DataComponentMap.Builder components = DataComponentMap.builder().addAll(DataComponents.COMMON_ITEM_COMPONENTS);
		@Nullable
		Item craftingRemainingItem;
		FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
		@Nullable
		private ResourceKey<Item> id;
		private DependantName<Item, String> descriptionId = ITEM_DESCRIPTION_ID;
		private DependantName<Item, ResourceLocation> model = ResourceKey::location;

		public Item.Properties food(FoodProperties arg) {
			return this.food(arg, Consumables.DEFAULT_FOOD);
		}

		public Item.Properties food(FoodProperties arg, Consumable arg2) {
			return this.component(DataComponents.FOOD, arg).component(DataComponents.CONSUMABLE, arg2);
		}

		public Item.Properties usingConvertsTo(Item arg) {
			return this.component(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStack(arg)));
		}

		public Item.Properties useCooldown(float f) {
			return this.component(DataComponents.USE_COOLDOWN, new UseCooldown(f));
		}

		public Item.Properties stacksTo(int i) {
			return this.component(DataComponents.MAX_STACK_SIZE, i);
		}

		public Item.Properties durability(int i) {
			this.component(DataComponents.MAX_DAMAGE, i);
			this.component(DataComponents.MAX_STACK_SIZE, 1);
			this.component(DataComponents.DAMAGE, 0);
			return this;
		}

		public Item.Properties craftRemainder(Item arg) {
			this.craftingRemainingItem = arg;
			return this;
		}

		public Item.Properties rarity(Rarity arg) {
			return this.component(DataComponents.RARITY, arg);
		}

		public Item.Properties fireResistant() {
			return this.component(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE));
		}

		public Item.Properties jukeboxPlayable(ResourceKey<JukeboxSong> arg) {
			return this.component(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(new EitherHolder<>(arg), true));
		}

		public Item.Properties enchantable(int i) {
			return this.component(DataComponents.ENCHANTABLE, new Enchantable(i));
		}

		public Item.Properties repairable(Item arg) {
			return this.component(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(arg.builtInRegistryHolder())));
		}

		public Item.Properties repairable(TagKey<Item> arg) {
			HolderGetter<Item> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ITEM);
			return this.component(DataComponents.REPAIRABLE, new Repairable(holdergetter.getOrThrow(arg)));
		}

		public Item.Properties equippable(EquipmentSlot arg) {
			return this.component(DataComponents.EQUIPPABLE, Equippable.builder(arg).build());
		}

		public Item.Properties equippableUnswappable(EquipmentSlot arg) {
			return this.component(DataComponents.EQUIPPABLE, Equippable.builder(arg).setSwappable(false).build());
		}

		public Item.Properties requiredFeatures(FeatureFlag... args) {
			this.requiredFeatures = FeatureFlags.REGISTRY.subset(args);
			return this;
		}

		public Item.Properties setId(ResourceKey<Item> arg) {
			this.id = arg;
			return this;
		}

		public Item.Properties overrideDescription(String string) {
			this.descriptionId = DependantName.fixed(string);
			return this;
		}

		public Item.Properties useBlockDescriptionPrefix() {
			this.descriptionId = BLOCK_DESCRIPTION_ID;
			return this;
		}

		public Item.Properties useItemDescriptionPrefix() {
			this.descriptionId = ITEM_DESCRIPTION_ID;
			return this;
		}

		protected String effectiveDescriptionId() {
			return this.descriptionId.get((ResourceKey<Item>)Objects.requireNonNull(this.id, "Item id not set"));
		}

		public Item.Properties overrideModel(ResourceLocation arg) {
			this.model = DependantName.fixed(arg);
			return this;
		}

		public ResourceLocation effectiveModel() {
			return this.model.get((ResourceKey<Item>)Objects.requireNonNull(this.id, "Item id not set"));
		}

		public <T> Item.Properties component(DataComponentType<T> arg, T object) {
			CommonHooks.validateComponent(object);
			this.components.set(arg, object);
			return this;
		}

		public Item.Properties attributes(ItemAttributeModifiers arg) {
			return this.component(DataComponents.ATTRIBUTE_MODIFIERS, arg);
		}

		DataComponentMap buildAndValidateComponents(Component arg, ResourceLocation arg2) {
			DataComponentMap datacomponentmap = this.components.set(DataComponents.ITEM_NAME, arg).set(DataComponents.ITEM_MODEL, arg2).build();
			return validateComponents(datacomponentmap);
		}

		public static DataComponentMap validateComponents(DataComponentMap datacomponentmap) {
			if (datacomponentmap.has(DataComponents.DAMAGE) && datacomponentmap.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
				throw new IllegalStateException("Item cannot have both durability and be stackable");
			} else {
				return datacomponentmap;
			}
		}
	}

	public interface TooltipContext {
		Item.TooltipContext EMPTY = new Item.TooltipContext() {
			@Nullable
			@Override
			public HolderLookup.Provider registries() {
				return null;
			}

			@Override
			public float tickRate() {
				return 20.0F;
			}

			@Nullable
			@Override
			public MapItemSavedData mapData(MapId arg) {
				return null;
			}
		};

		@Nullable
		HolderLookup.Provider registries();

		float tickRate();

		@Nullable
		MapItemSavedData mapData(MapId arg);

		@Nullable
		default Level level() {
			return null;
		}

		static Item.TooltipContext of(@Nullable Level arg) {
			return arg == null ? EMPTY : new Item.TooltipContext() {
				@Override
				public HolderLookup.Provider registries() {
					return arg.registryAccess();
				}

				@Override
				public float tickRate() {
					return arg.tickRateManager().tickrate();
				}

				@Override
				public MapItemSavedData mapData(MapId arg) {
					return arg.getMapData(arg);
				}

				@Override
				public Level level() {
					return arg;
				}
			};
		}

		static Item.TooltipContext of(HolderLookup.Provider arg) {
			return new Item.TooltipContext() {
				@Override
				public HolderLookup.Provider registries() {
					return arg;
				}

				@Override
				public float tickRate() {
					return 20.0F;
				}

				@Nullable
				@Override
				public MapItemSavedData mapData(MapId arg) {
					return null;
				}
			};
		}
	}
}
