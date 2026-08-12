package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.NullOps;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.common.util.AttributeUtil;
import net.neoforged.neoforge.common.util.DataComponentUtil;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public final class ItemStack implements DataComponentHolder, IItemStackExtension, MutableDataComponentHolder {
	public static final Codec<ItemStack> CODEC = Codec.lazyInitialized(
		() -> RecordCodecBuilder.create(
			instance -> instance.group(
					Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
					ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
					DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(arg -> arg.components.asPatch())
				)
				.apply(instance, ItemStack::new)
		)
	);
	public static final Codec<ItemStack> SINGLE_ITEM_CODEC = Codec.lazyInitialized(
		() -> RecordCodecBuilder.create(
			instance -> instance.group(
					Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
					DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(arg -> arg.components.asPatch())
				)
				.apply(instance, (arg, arg2) -> new ItemStack(arg, 1, arg2))
		)
	);
	public static final Codec<ItemStack> STRICT_CODEC = CODEC.validate(ItemStack::validateStrict);
	public static final Codec<ItemStack> STRICT_SINGLE_ITEM_CODEC = SINGLE_ITEM_CODEC.validate(ItemStack::validateStrict);
	public static final Codec<ItemStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
		.xmap(optional -> (ItemStack)optional.orElse(ItemStack.EMPTY), arg -> arg.isEmpty() ? Optional.empty() : Optional.of(arg));
	public static final Codec<ItemStack> SIMPLE_ITEM_CODEC = Item.CODEC.xmap(ItemStack::new, ItemStack::getItemHolder);
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
		private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Item>> ITEM_STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ITEM);

		public ItemStack decode(RegistryFriendlyByteBuf arg) {
			int i = arg.readVarInt();
			if (i <= 0) {
				return ItemStack.EMPTY;
			} else {
				Holder<Item> holder = ITEM_STREAM_CODEC.decode(arg);
				DataComponentPatch datacomponentpatch = DataComponentPatch.STREAM_CODEC.decode(arg);
				return new ItemStack(holder, i, datacomponentpatch);
			}
		}

		public void encode(RegistryFriendlyByteBuf arg, ItemStack arg2) {
			if (arg2.isEmpty()) {
				arg.writeVarInt(0);
			} else {
				arg.writeVarInt(arg2.getCount());
				ITEM_STREAM_CODEC.encode(arg, arg2.getItemHolder());
				DataComponentPatch.STREAM_CODEC.encode(arg, arg2.components.asPatch());
			}
		}
	};
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
		public ItemStack decode(RegistryFriendlyByteBuf arg) {
			ItemStack itemstack = ItemStack.OPTIONAL_STREAM_CODEC.decode(arg);
			if (itemstack.isEmpty()) {
				throw new DecoderException("Empty ItemStack not allowed");
			} else {
				return itemstack;
			}
		}

		public void encode(RegistryFriendlyByteBuf arg, ItemStack arg2) {
			if (arg2.isEmpty()) {
				throw new EncoderException("Empty ItemStack not allowed");
			} else {
				ItemStack.OPTIONAL_STREAM_CODEC.encode(arg, arg2);
			}
		}
	};
	public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> OPTIONAL_LIST_STREAM_CODEC = OPTIONAL_STREAM_CODEC.apply(
		ByteBufCodecs.collection(NonNullList::createWithCapacity)
	);
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final ItemStack EMPTY = new ItemStack((Void)null);
	private static final Component DISABLED_ITEM_TOOLTIP = Component.translatable("item.disabled").withStyle(ChatFormatting.RED);
	private int count;
	private int popTime;
	@Deprecated
	@Nullable
	private final Item item;
	final PatchedDataComponentMap components;
	@Nullable
	private Entity entityRepresentation;

	private static DataResult<ItemStack> validateStrict(ItemStack arg) {
		DataResult<Unit> dataresult = validateComponents(arg.getComponents());
		if (dataresult.isError()) {
			return dataresult.map(arg2 -> arg);
		} else {
			return arg.getCount() > arg.getMaxStackSize()
				? DataResult.error(() -> "Item stack with stack size of " + arg.getCount() + " was larger than maximum: " + arg.getMaxStackSize())
				: DataResult.success(arg);
		}
	}

	public static StreamCodec<RegistryFriendlyByteBuf, ItemStack> validatedStreamCodec(StreamCodec<RegistryFriendlyByteBuf, ItemStack> arg) {
		return new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
			public ItemStack decode(RegistryFriendlyByteBuf arg) {
				ItemStack itemstack = (ItemStack)arg.decode(arg);
				if (!itemstack.isEmpty()) {
					RegistryOps<Unit> registryops = arg.registryAccess().createSerializationContext(NullOps.INSTANCE);
					ItemStack.CODEC.encodeStart(registryops, itemstack).getOrThrow(DecoderException::new);
				}

				return itemstack;
			}

			public void encode(RegistryFriendlyByteBuf arg, ItemStack arg2) {
				arg.encode(arg, arg2);
			}
		};
	}

	public Optional<TooltipComponent> getTooltipImage() {
		return this.getItem().getTooltipImage(this);
	}

	@Override
	public DataComponentMap getComponents() {
		return (DataComponentMap)(!this.isEmpty() ? this.components : DataComponentMap.EMPTY);
	}

	public void clearComponents() {
		this.components.clearPatch();
	}

	public DataComponentMap getPrototype() {
		return !this.isEmpty() ? this.getItem().components() : DataComponentMap.EMPTY;
	}

	public DataComponentPatch getComponentsPatch() {
		return !this.isEmpty() ? this.components.asPatch() : DataComponentPatch.EMPTY;
	}

	public boolean isComponentsPatchEmpty() {
		return !this.isEmpty() ? this.components.isPatchEmpty() : true;
	}

	public ItemStack(ItemLike arg) {
		this(arg, 1);
	}

	public ItemStack(Holder<Item> arg) {
		this((ItemLike)arg.value(), 1);
	}

	public ItemStack(Holder<Item> arg, int i, DataComponentPatch arg2) {
		this((ItemLike)arg.value(), i, PatchedDataComponentMap.fromPatch(((Item)arg.value()).components(), arg2));
	}

	public ItemStack(Holder<Item> arg, int i) {
		this((ItemLike)arg.value(), i);
	}

	public ItemStack(ItemLike arg, int i) {
		this(arg, i, new PatchedDataComponentMap(arg.asItem().components()));
	}

	private ItemStack(ItemLike arg, int i, PatchedDataComponentMap arg2) {
		this.item = arg.asItem();
		this.count = i;
		this.components = arg2;
		this.getItem().verifyComponentsAfterLoad(this);
	}

	private ItemStack(@Nullable Void void_) {
		this.item = null;
		this.components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
	}

	public static DataResult<Unit> validateComponents(DataComponentMap arg) {
		if (arg.has(DataComponents.MAX_DAMAGE) && arg.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
			return DataResult.error(() -> "Item cannot be both damageable and stackable");
		} else {
			ItemContainerContents itemcontainercontents = arg.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

			for (ItemStack itemstack : itemcontainercontents.nonEmptyItems()) {
				int i = itemstack.getCount();
				int j = itemstack.getMaxStackSize();
				if (i > j) {
					return DataResult.error(() -> "Item stack with count of " + i + " was larger than maximum: " + j);
				}
			}

			return DataResult.success(Unit.INSTANCE);
		}
	}

	public static Optional<ItemStack> parse(HolderLookup.Provider arg, Tag arg2) {
		return CODEC.parse(arg.createSerializationContext(NbtOps.INSTANCE), arg2).resultOrPartial(string -> LOGGER.error("Tried to load invalid item: '{}'", string));
	}

	public static ItemStack parseOptional(HolderLookup.Provider arg, CompoundTag arg2) {
		return arg2.isEmpty() ? EMPTY : (ItemStack)parse(arg, arg2).orElse(EMPTY);
	}

	public boolean isEmpty() {
		return this == EMPTY || this.item == Items.AIR || this.count <= 0;
	}

	public boolean isItemEnabled(FeatureFlagSet arg) {
		return this.isEmpty() || this.getItem().isEnabled(arg);
	}

	public ItemStack split(int j) {
		int i = Math.min(j, this.getCount());
		ItemStack itemstack = this.copyWithCount(i);
		this.shrink(i);
		return itemstack;
	}

	public ItemStack copyAndClear() {
		if (this.isEmpty()) {
			return EMPTY;
		} else {
			ItemStack itemstack = this.copy();
			this.setCount(0);
			return itemstack;
		}
	}

	public Item getItem() {
		return this.isEmpty() ? Items.AIR : this.item;
	}

	public Holder<Item> getItemHolder() {
		return this.getItem().builtInRegistryHolder();
	}

	public boolean is(TagKey<Item> arg) {
		return this.getItem().builtInRegistryHolder().is(arg);
	}

	public boolean is(Item arg) {
		return this.getItem() == arg;
	}

	public boolean is(Predicate<Holder<Item>> predicate) {
		return predicate.test(this.getItem().builtInRegistryHolder());
	}

	public boolean is(Holder<Item> arg) {
		return this.is((Item)arg.value());
	}

	public boolean is(HolderSet<Item> arg) {
		return arg.contains(this.getItemHolder());
	}

	public Stream<TagKey<Item>> getTags() {
		return this.getItem().builtInRegistryHolder().tags();
	}

	public InteractionResult useOn(UseOnContext arg) {
		UseItemOnBlockEvent e = NeoForge.EVENT_BUS.post(new UseItemOnBlockEvent(arg, UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK));
		if (e.isCanceled()) {
			return e.getCancellationResult();
		} else {
			return !arg.getLevel().isClientSide ? CommonHooks.onPlaceItemIntoWorld(arg) : this.onItemUse(arg, c -> this.getItem().useOn(arg));
		}
	}

	@Override
	public InteractionResult onItemUseFirst(UseOnContext arg) {
		UseItemOnBlockEvent e = NeoForge.EVENT_BUS.post(new UseItemOnBlockEvent(arg, UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK));
		return e.isCanceled() ? e.getCancellationResult() : this.onItemUse(arg, c -> this.getItem().onItemUseFirst(this, arg));
	}

	private InteractionResult onItemUse(UseOnContext arg, Function<UseOnContext, InteractionResult> callback) {
		Player player = arg.getPlayer();
		BlockPos blockpos = arg.getClickedPos();
		if (player != null && !player.getAbilities().mayBuild && !this.canPlaceOnBlockInAdventureMode(new BlockInWorld(arg.getLevel(), blockpos, false))) {
			return InteractionResult.PASS;
		} else {
			Item item = this.getItem();
			InteractionResult interactionresult = (InteractionResult)callback.apply(arg);
			if (player != null && interactionresult instanceof InteractionResult.Success interactionresult$success && interactionresult$success.wasItemInteraction()) {
				player.awardStat(Stats.ITEM_USED.get(item));
			}

			return interactionresult;
		}
	}

	public float getDestroySpeed(BlockState arg) {
		return this.getItem().getDestroySpeed(this, arg);
	}

	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemstack = this.copy();
		boolean flag = this.getUseDuration(arg2) <= 0;
		InteractionResult interactionresult = this.getItem().use(arg, arg2, arg3);
		return (InteractionResult)(flag && interactionresult instanceof InteractionResult.Success interactionresult$success
			? interactionresult$success.heldItemTransformedTo(
				interactionresult$success.heldItemTransformedTo() == null
					? this.applyAfterUseComponentSideEffects(arg2, itemstack)
					: interactionresult$success.heldItemTransformedTo().applyAfterUseComponentSideEffects(arg2, itemstack)
			)
			: interactionresult);
	}

	public ItemStack finishUsingItem(Level arg, LivingEntity arg2) {
		ItemStack itemstack = this.copy();
		ItemStack itemstack1 = this.getItem().finishUsingItem(this, arg, arg2);
		return itemstack1.applyAfterUseComponentSideEffects(arg2, itemstack);
	}

	private ItemStack applyAfterUseComponentSideEffects(LivingEntity arg, ItemStack arg2) {
		UseRemainder useremainder = arg2.get(DataComponents.USE_REMAINDER);
		UseCooldown usecooldown = arg2.get(DataComponents.USE_COOLDOWN);
		int i = arg2.getCount();
		ItemStack itemstack = this;
		if (useremainder != null) {
			itemstack = useremainder.convertIntoRemainder(this, i, arg.hasInfiniteMaterials(), arg::handleExtraItemsCreatedOnUse);
		}

		if (usecooldown != null) {
			usecooldown.apply(arg2, arg);
		}

		return itemstack;
	}

	public Tag save(HolderLookup.Provider arg, Tag arg2) {
		if (this.isEmpty()) {
			throw new IllegalStateException("Cannot encode empty ItemStack");
		} else {
			return DataComponentUtil.wrapEncodingExceptions(this, CODEC, arg, arg2);
		}
	}

	public Tag save(HolderLookup.Provider arg) {
		if (this.isEmpty()) {
			throw new IllegalStateException("Cannot encode empty ItemStack");
		} else {
			return DataComponentUtil.wrapEncodingExceptions(this, CODEC, arg);
		}
	}

	public Tag saveOptional(HolderLookup.Provider arg) {
		return (Tag)(this.isEmpty() ? new CompoundTag() : this.save(arg, new CompoundTag()));
	}

	public int getMaxStackSize() {
		return this.getItem().getMaxStackSize(this);
	}

	public boolean isStackable() {
		return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
	}

	public boolean isDamageableItem() {
		return this.has(DataComponents.MAX_DAMAGE) && !this.has(DataComponents.UNBREAKABLE) && this.has(DataComponents.DAMAGE);
	}

	public boolean isDamaged() {
		return this.isDamageableItem() && this.getItem().isDamaged(this);
	}

	public int getDamageValue() {
		return this.getItem().getDamage(this);
	}

	public void setDamageValue(int i) {
		this.getItem().setDamage(this, i);
	}

	public int getMaxDamage() {
		return this.getItem().getMaxDamage(this);
	}

	public boolean isBroken() {
		return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage();
	}

	public boolean nextDamageWillBreak() {
		return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage() - 1;
	}

	public void hurtAndBreak(int i, ServerLevel arg, @Nullable ServerPlayer arg2, Consumer<Item> consumer) {
		this.hurtAndBreak(i, arg, (LivingEntity)arg2, consumer);
	}

	public void hurtAndBreak(int j, ServerLevel arg, @Nullable LivingEntity arg2, Consumer<Item> consumer) {
		j = this.getItem().damageItem(this, j, arg2, consumer);
		int i = this.processDurabilityChange(j, arg, arg2);
		if (i != 0) {
			this.applyDamage(this.getDamageValue() + i, arg2, consumer);
		}
	}

	private int processDurabilityChange(int i, ServerLevel arg, @Nullable ServerPlayer arg2) {
		return this.processDurabilityChange(i, arg, (LivingEntity)arg2);
	}

	private int processDurabilityChange(int i, ServerLevel arg, @Nullable LivingEntity arg2) {
		if (!this.isDamageableItem()) {
			return 0;
		} else if (arg2 != null && arg2.hasInfiniteMaterials()) {
			return 0;
		} else {
			return i > 0 ? EnchantmentHelper.processDurabilityChange(arg, this, i) : i;
		}
	}

	private void applyDamage(int i, @Nullable ServerPlayer arg, Consumer<Item> consumer) {
		this.applyDamage(i, (LivingEntity)arg, consumer);
	}

	private void applyDamage(int i, @Nullable LivingEntity arg, Consumer<Item> consumer) {
		if (arg instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(serverPlayer, this, i);
		}

		this.setDamageValue(i);
		if (this.isBroken()) {
			Item item = this.getItem();
			this.shrink(1);
			consumer.accept(item);
		}
	}

	public void hurtWithoutBreaking(int k, Player arg) {
		if (arg instanceof ServerPlayer serverplayer) {
			int i = this.processDurabilityChange(k, serverplayer.serverLevel(), serverplayer);
			if (i == 0) {
				return;
			}

			int j = Math.min(this.getDamageValue() + i, this.getMaxDamage() - 1);
			this.applyDamage(j, serverplayer, argx -> {});
		}
	}

	public void hurtAndBreak(int i, LivingEntity arg, EquipmentSlot arg2) {
		if (arg.level() instanceof ServerLevel serverlevel) {
			this.hurtAndBreak(i, serverlevel, arg, arg3 -> arg.onEquippedItemBroken(arg3, arg2));
		}
	}

	public ItemStack hurtAndConvertOnBreak(int i, ItemLike arg, LivingEntity arg2, EquipmentSlot arg3) {
		this.hurtAndBreak(i, arg2, arg3);
		if (this.isEmpty()) {
			ItemStack itemstack = this.transmuteCopyIgnoreEmpty(arg, 1);
			if (itemstack.isDamageableItem()) {
				itemstack.setDamageValue(0);
			}

			return itemstack;
		} else {
			return this;
		}
	}

	public boolean isBarVisible() {
		return this.getItem().isBarVisible(this);
	}

	public int getBarWidth() {
		return this.getItem().getBarWidth(this);
	}

	public int getBarColor() {
		return this.getItem().getBarColor(this);
	}

	public boolean overrideStackedOnOther(Slot arg, ClickAction arg2, Player arg3) {
		return this.getItem().overrideStackedOnOther(this, arg, arg2, arg3);
	}

	public boolean overrideOtherStackedOnMe(ItemStack arg, Slot arg2, ClickAction arg3, Player arg4, SlotAccess arg5) {
		return this.getItem().overrideOtherStackedOnMe(this, arg, arg2, arg3, arg4, arg5);
	}

	public boolean hurtEnemy(LivingEntity arg, LivingEntity arg2) {
		Item item = this.getItem();
		if (item.hurtEnemy(this, arg, arg2)) {
			if (arg2 instanceof Player player) {
				player.awardStat(Stats.ITEM_USED.get(item));
			}

			return true;
		} else {
			return false;
		}
	}

	public void postHurtEnemy(LivingEntity arg, LivingEntity arg2) {
		this.getItem().postHurtEnemy(this, arg, arg2);
	}

	public void mineBlock(Level arg, BlockState arg2, BlockPos arg3, Player arg4) {
		Item item = this.getItem();
		if (item.mineBlock(this, arg, arg2, arg3, arg4)) {
			arg4.awardStat(Stats.ITEM_USED.get(item));
		}
	}

	public boolean isCorrectToolForDrops(BlockState arg) {
		return this.getItem().isCorrectToolForDrops(this, arg);
	}

	public InteractionResult interactLivingEntity(Player arg, LivingEntity arg2, InteractionHand arg3) {
		return this.getItem().interactLivingEntity(this, arg, arg2, arg3);
	}

	public ItemStack copy() {
		if (this.isEmpty()) {
			return EMPTY;
		} else {
			ItemStack itemstack = new ItemStack(this.getItem(), this.count, this.components.copy());
			itemstack.setPopTime(this.getPopTime());
			return itemstack;
		}
	}

	public ItemStack copyWithCount(int i) {
		if (this.isEmpty()) {
			return EMPTY;
		} else {
			ItemStack itemstack = this.copy();
			itemstack.setCount(i);
			return itemstack;
		}
	}

	public ItemStack transmuteCopy(ItemLike arg) {
		return this.transmuteCopy(arg, this.getCount());
	}

	public ItemStack transmuteCopy(ItemLike arg, int i) {
		return this.isEmpty() ? EMPTY : this.transmuteCopyIgnoreEmpty(arg, i);
	}

	private ItemStack transmuteCopyIgnoreEmpty(ItemLike arg, int i) {
		return new ItemStack(arg.asItem().builtInRegistryHolder(), i, this.components.asPatch());
	}

	public static boolean matches(ItemStack arg, ItemStack arg2) {
		if (arg == arg2) {
			return true;
		} else {
			return arg.getCount() != arg2.getCount() ? false : isSameItemSameComponents(arg, arg2);
		}
	}

	@Deprecated
	public static boolean listMatches(List<ItemStack> list, List<ItemStack> list2) {
		if (list.size() != list2.size()) {
			return false;
		} else {
			for (int i = 0; i < list.size(); i++) {
				if (!matches((ItemStack)list.get(i), (ItemStack)list2.get(i))) {
					return false;
				}
			}

			return true;
		}
	}

	public static boolean isSameItem(ItemStack arg, ItemStack arg2) {
		return arg.is(arg2.getItem());
	}

	public static boolean isSameItemSameComponents(ItemStack arg, ItemStack arg2) {
		if (!arg.is(arg2.getItem())) {
			return false;
		} else {
			return arg.isEmpty() && arg2.isEmpty() ? true : Objects.equals(arg.components, arg2.components);
		}
	}

	public static MapCodec<ItemStack> lenientOptionalFieldOf(String string) {
		return CODEC.lenientOptionalFieldOf(string).xmap(optional -> (ItemStack)optional.orElse(EMPTY), arg -> arg.isEmpty() ? Optional.empty() : Optional.of(arg));
	}

	public static int hashItemAndComponents(@Nullable ItemStack arg) {
		if (arg != null) {
			int i = 31 + arg.getItem().hashCode();
			return 31 * i + arg.getComponents().hashCode();
		} else {
			return 0;
		}
	}

	@Deprecated
	public static int hashStackList(List<ItemStack> list) {
		int i = 0;

		for (ItemStack itemstack : list) {
			i = i * 31 + hashItemAndComponents(itemstack);
		}

		return i;
	}

	public String toString() {
		return this.getCount() + " " + this.getItem();
	}

	public void inventoryTick(Level arg, Entity arg2, int i, boolean bl) {
		if (this.popTime > 0) {
			this.popTime--;
		}

		if (this.getItem() != null) {
			this.getItem().inventoryTick(this, arg, arg2, i, bl);
		}
	}

	public void onCraftedBy(Level arg, Player arg2, int i) {
		arg2.awardStat(Stats.ITEM_CRAFTED.get(this.getItem()), i);
		this.getItem().onCraftedBy(this, arg, arg2);
	}

	public void onCraftedBySystem(Level arg) {
		this.getItem().onCraftedPostProcess(this, arg);
	}

	public int getUseDuration(LivingEntity arg) {
		return this.getItem().getUseDuration(this, arg);
	}

	public ItemUseAnimation getUseAnimation() {
		return this.getItem().getUseAnimation(this);
	}

	public void releaseUsing(Level arg, LivingEntity arg2, int i) {
		ItemStack itemstack = this.copy();
		if (this.getItem().releaseUsing(this, arg, arg2, i)) {
			ItemStack itemstack1 = this.applyAfterUseComponentSideEffects(arg2, itemstack);
			if (itemstack1 != this) {
				arg2.setItemInHand(arg2.getUsedItemHand(), itemstack1);
			}
		}
	}

	public boolean useOnRelease() {
		return this.getItem().useOnRelease(this);
	}

	@Nullable
	@Override
	public <T> T set(DataComponentType<? super T> arg, @Nullable T object) {
		return this.components.set(arg, (T)object);
	}

	@Nullable
	@Override
	public <T, U> T update(DataComponentType<T> arg, T object, U object2, BiFunction<T, U, T> biFunction) {
		return this.set(arg, (T)biFunction.apply(this.getOrDefault(arg, object), object2));
	}

	@Nullable
	@Override
	public <T> T update(DataComponentType<T> arg, T object, UnaryOperator<T> unaryOperator) {
		T t = this.getOrDefault(arg, (T)object);
		return this.set(arg, (T)unaryOperator.apply(t));
	}

	@Nullable
	@Override
	public <T> T remove(DataComponentType<? extends T> arg) {
		return this.components.remove(arg);
	}

	public void applyComponentsAndValidate(DataComponentPatch arg) {
		DataComponentPatch datacomponentpatch = this.components.asPatch();
		this.components.applyPatch(arg);
		Optional<Error<ItemStack>> optional = validateStrict(this).error();
		if (optional.isPresent()) {
			LOGGER.error("Failed to apply component patch '{}' to item: '{}'", arg, ((Error)optional.get()).message());
			this.components.restorePatch(datacomponentpatch);
		} else {
			this.getItem().verifyComponentsAfterLoad(this);
		}
	}

	@Override
	public void applyComponents(DataComponentPatch arg) {
		this.components.applyPatch(arg);
		this.getItem().verifyComponentsAfterLoad(this);
	}

	@Override
	public void applyComponents(DataComponentMap arg) {
		this.components.setAll(arg);
		this.getItem().verifyComponentsAfterLoad(this);
	}

	public Component getHoverName() {
		Component component = this.get(DataComponents.CUSTOM_NAME);
		if (component != null) {
			return component;
		} else {
			WrittenBookContent writtenbookcontent = this.get(DataComponents.WRITTEN_BOOK_CONTENT);
			if (writtenbookcontent != null) {
				String s = writtenbookcontent.title().raw();
				if (!StringUtil.isBlank(s)) {
					return Component.literal(s);
				}
			}

			return this.getItemName();
		}
	}

	public Component getItemName() {
		return this.getItem().getName(this);
	}

	public Component getStyledHoverName() {
		MutableComponent mutablecomponent = Component.empty().append(this.getHoverName()).withStyle(this.getRarity().getStyleModifier());
		if (this.has(DataComponents.CUSTOM_NAME)) {
			mutablecomponent.withStyle(ChatFormatting.ITALIC);
		}

		return mutablecomponent;
	}

	@Override
	public <T extends TooltipProvider> void addToTooltip(DataComponentType<T> arg, Item.TooltipContext arg2, Consumer<Component> consumer, TooltipFlag arg3) {
		T t = (T)this.get(arg);
		if (t != null) {
			t.addToTooltip(arg2, consumer, arg3);
		}
	}

	public List<Component> getTooltipLines(Item.TooltipContext arg, @Nullable Player arg2, TooltipFlag arg3) {
		if (!arg3.isCreative() && this.has(DataComponents.HIDE_TOOLTIP)) {
			return List.of();
		} else {
			List<Component> list = Lists.<Component>newArrayList();
			list.add(this.getStyledHoverName());
			if (!arg3.isAdvanced() && !this.has(DataComponents.CUSTOM_NAME)) {
				MapId mapid = this.get(DataComponents.MAP_ID);
				if (mapid != null) {
					list.add(MapItem.getTooltipForId(mapid));
				}
			}

			Consumer<Component> consumer = list::add;
			if (!this.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)) {
				this.getItem().appendHoverText(this, arg, list, arg3);
			}

			this.addToTooltip(DataComponents.JUKEBOX_PLAYABLE, arg, consumer, arg3);
			this.addToTooltip(DataComponents.TRIM, arg, consumer, arg3);
			this.addToTooltip(DataComponents.STORED_ENCHANTMENTS, arg, consumer, arg3);
			this.addToTooltip(DataComponents.ENCHANTMENTS, arg, consumer, arg3);
			this.addToTooltip(DataComponents.DYED_COLOR, arg, consumer, arg3);
			this.addToTooltip(DataComponents.LORE, arg, consumer, arg3);
			AttributeUtil.addAttributeTooltips(this, consumer, AttributeTooltipContext.of(arg2, arg, arg3));
			this.addToTooltip(DataComponents.UNBREAKABLE, arg, consumer, arg3);
			this.addToTooltip(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, arg, consumer, arg3);
			this.addToTooltip(DataComponents.SUSPICIOUS_STEW_EFFECTS, arg, consumer, arg3);
			AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_BREAK);
			if (adventuremodepredicate != null && adventuremodepredicate.showInTooltip()) {
				consumer.accept(CommonComponents.EMPTY);
				consumer.accept(AdventureModePredicate.CAN_BREAK_HEADER);
				adventuremodepredicate.addToTooltip(consumer);
			}

			AdventureModePredicate adventuremodepredicate1 = this.get(DataComponents.CAN_PLACE_ON);
			if (adventuremodepredicate1 != null && adventuremodepredicate1.showInTooltip()) {
				consumer.accept(CommonComponents.EMPTY);
				consumer.accept(AdventureModePredicate.CAN_PLACE_HEADER);
				adventuremodepredicate1.addToTooltip(consumer);
			}

			if (arg3.isAdvanced()) {
				if (this.isDamaged()) {
					list.add(Component.translatable("item.durability", this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()));
				}

				list.add(Component.literal(BuiltInRegistries.ITEM.getKey(this.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
				int i = this.components.size();
				if (i > 0) {
					list.add(Component.translatable("item.components", i).withStyle(ChatFormatting.DARK_GRAY));
				}
			}

			if (arg2 != null && !this.getItem().isEnabled(arg2.level().enabledFeatures())) {
				list.add(DISABLED_ITEM_TOOLTIP);
			}

			EventHooks.onItemTooltip(this, arg2, list, arg3, arg);
			return list;
		}
	}

	@Deprecated
	private void addAttributeTooltips(Consumer<Component> consumer, @Nullable Player arg) {
		ItemAttributeModifiers itemattributemodifiers = this.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		if (itemattributemodifiers.showInTooltip()) {
			for (EquipmentSlotGroup equipmentslotgroup : EquipmentSlotGroup.values()) {
				MutableBoolean mutableboolean = new MutableBoolean(true);
				this.forEachModifier(equipmentslotgroup, (arg2, arg3) -> {
					if (mutableboolean.isTrue()) {
						consumer.accept(CommonComponents.EMPTY);
						consumer.accept(Component.translatable("item.modifiers." + equipmentslotgroup.getSerializedName()).withStyle(ChatFormatting.GRAY));
						mutableboolean.setFalse();
					}

					this.addModifierTooltip(consumer, arg, arg2, arg3);
				});
			}
		}
	}

	private void addModifierTooltip(Consumer<Component> consumer, @Nullable Player arg, Holder<Attribute> arg2, AttributeModifier arg3) {
		double d0 = arg3.amount();
		boolean flag = false;
		if (arg != null) {
			if (arg3.is(Item.BASE_ATTACK_DAMAGE_ID)) {
				d0 += arg.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
				flag = true;
			} else if (arg3.is(Item.BASE_ATTACK_SPEED_ID)) {
				d0 += arg.getAttributeBaseValue(Attributes.ATTACK_SPEED);
				flag = true;
			}
		}

		double d1;
		if (arg3.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE || arg3.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
			d1 = d0 * 100.0;
		} else if (arg2.is(Attributes.KNOCKBACK_RESISTANCE)) {
			d1 = d0 * 10.0;
		} else {
			d1 = d0;
		}

		if (flag) {
			consumer.accept(
				CommonComponents.space()
					.append(
						Component.translatable(
							"attribute.modifier.equals." + arg3.operation().id(),
							ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d1),
							Component.translatable(((Attribute)arg2.value()).getDescriptionId())
						)
					)
					.withStyle(ChatFormatting.DARK_GREEN)
			);
		} else if (d0 > 0.0) {
			consumer.accept(
				Component.translatable(
						"attribute.modifier.plus." + arg3.operation().id(),
						ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d1),
						Component.translatable(((Attribute)arg2.value()).getDescriptionId())
					)
					.withStyle(((Attribute)arg2.value()).getStyle(true))
			);
		} else if (d0 < 0.0) {
			consumer.accept(
				Component.translatable(
						"attribute.modifier.take." + arg3.operation().id(),
						ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(-d1),
						Component.translatable(((Attribute)arg2.value()).getDescriptionId())
					)
					.withStyle(((Attribute)arg2.value()).getStyle(false))
			);
		}
	}

	public boolean hasFoil() {
		Boolean obool = this.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
		return obool != null ? obool : this.getItem().isFoil(this);
	}

	public Rarity getRarity() {
		Rarity rarity = this.getOrDefault(DataComponents.RARITY, Rarity.COMMON);
		if (!this.isEnchanted()) {
			return rarity;
		} else {
			return switch (rarity) {
				case COMMON, UNCOMMON -> Rarity.RARE;
				case RARE -> Rarity.EPIC;
				default -> rarity;
			};
		}
	}

	public boolean isEnchantable() {
		if (!this.has(DataComponents.ENCHANTABLE)) {
			return false;
		} else {
			ItemEnchantments itemenchantments = this.get(DataComponents.ENCHANTMENTS);
			return itemenchantments != null && itemenchantments.isEmpty();
		}
	}

	public void enchant(Holder<Enchantment> arg, int i) {
		EnchantmentHelper.updateEnchantments(this, arg2 -> arg2.upgrade(arg, i));
	}

	public boolean isEnchanted() {
		return !this.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
	}

	public ItemEnchantments getTagEnchantments() {
		return this.getEnchantments();
	}

	@Deprecated
	public ItemEnchantments getEnchantments() {
		return this.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
	}

	public boolean isFramed() {
		return this.entityRepresentation instanceof ItemFrame;
	}

	public void setEntityRepresentation(@Nullable Entity arg) {
		if (!this.isEmpty()) {
			this.entityRepresentation = arg;
		}
	}

	@Nullable
	public ItemFrame getFrame() {
		return this.entityRepresentation instanceof ItemFrame ? (ItemFrame)this.getEntityRepresentation() : null;
	}

	@Nullable
	public Entity getEntityRepresentation() {
		return !this.isEmpty() ? this.entityRepresentation : null;
	}

	public void forEachModifier(EquipmentSlotGroup arg, BiConsumer<Holder<Attribute>, AttributeModifier> biConsumer) {
		this.getAttributeModifiers().forEach(arg, biConsumer);
		EnchantmentHelper.forEachModifier(this, arg, biConsumer);
	}

	public void forEachModifier(EquipmentSlot arg, BiConsumer<Holder<Attribute>, AttributeModifier> biConsumer) {
		this.getAttributeModifiers().forEach(arg, biConsumer);
		EnchantmentHelper.forEachModifier(this, arg, biConsumer);
	}

	public Component getDisplayName() {
		MutableComponent mutablecomponent = Component.empty().append(this.getHoverName());
		if (this.has(DataComponents.CUSTOM_NAME)) {
			mutablecomponent.withStyle(ChatFormatting.ITALIC);
		}

		MutableComponent mutablecomponent1 = ComponentUtils.wrapInSquareBrackets(mutablecomponent);
		if (!this.isEmpty()) {
			mutablecomponent1.withStyle(this.getRarity().getStyleModifier())
				.withStyle(arg -> arg.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(this))));
		}

		return mutablecomponent1;
	}

	public boolean canPlaceOnBlockInAdventureMode(BlockInWorld arg) {
		AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_PLACE_ON);
		return adventuremodepredicate != null && adventuremodepredicate.test(arg);
	}

	public boolean canBreakBlockInAdventureMode(BlockInWorld arg) {
		AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_BREAK);
		return adventuremodepredicate != null && adventuremodepredicate.test(arg);
	}

	public int getPopTime() {
		return this.popTime;
	}

	public void setPopTime(int i) {
		this.popTime = i;
	}

	public int getCount() {
		return this.isEmpty() ? 0 : this.count;
	}

	public void setCount(int i) {
		this.count = i;
	}

	public void limitSize(int i) {
		if (!this.isEmpty() && this.getCount() > i) {
			this.setCount(i);
		}
	}

	public void grow(int i) {
		this.setCount(this.getCount() + i);
	}

	public void shrink(int i) {
		this.grow(-i);
	}

	public void consume(int i, @Nullable LivingEntity arg) {
		if (arg == null || !arg.hasInfiniteMaterials()) {
			this.shrink(i);
		}
	}

	public ItemStack consumeAndReturn(int i, @Nullable LivingEntity arg) {
		ItemStack itemstack = this.copyWithCount(i);
		this.consume(i, arg);
		return itemstack;
	}

	public void onUseTick(Level arg, LivingEntity arg2, int i) {
		Consumable consumable = this.get(DataComponents.CONSUMABLE);
		if (consumable != null && consumable.shouldEmitParticlesAndSounds(i)) {
			consumable.emitParticlesAndSounds(arg2.getRandom(), arg2, this, 5);
		}

		this.getItem().onUseTick(arg, arg2, this, i);
	}

	@Deprecated
	public void onDestroyed(ItemEntity arg) {
		this.getItem().onDestroyed(arg);
	}

	public SoundEvent getBreakingSound() {
		return this.getItem().getBreakingSound();
	}

	public boolean canBeHurtBy(DamageSource arg) {
		if (!this.getItem().canBeHurtBy(this, arg)) {
			return false;
		} else {
			DamageResistant damageresistant = this.get(DataComponents.DAMAGE_RESISTANT);
			return damageresistant == null || !damageresistant.isResistantTo(arg);
		}
	}

	public boolean isValidRepairItem(ItemStack arg) {
		Repairable repairable = this.get(DataComponents.REPAIRABLE);
		return repairable != null && repairable.isValidRepairItem(arg);
	}
}
