package net.minecraft.world.item.enchantment;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.effects.DamageImmunity;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableFloat;

public record Enchantment(Component description, Enchantment.EnchantmentDefinition definition, HolderSet<Enchantment> exclusiveSet, DataComponentMap effects) {
	public static final int MAX_LEVEL = 255;
	public static final Codec<Enchantment> DIRECT_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				ComponentSerialization.CODEC.fieldOf("description").forGetter(Enchantment::description),
				Enchantment.EnchantmentDefinition.CODEC.forGetter(Enchantment::definition),
				RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("exclusive_set", HolderSet.direct()).forGetter(Enchantment::exclusiveSet),
				EnchantmentEffectComponents.CODEC.optionalFieldOf("effects", DataComponentMap.EMPTY).forGetter(Enchantment::effects)
			)
			.apply(instance, Enchantment::new)
	);
	public static final Codec<Holder<Enchantment>> CODEC = RegistryFixedCodec.create(Registries.ENCHANTMENT);
	public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Enchantment>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT);

	public static Enchantment.Cost constantCost(int i) {
		return new Enchantment.Cost(i, 0);
	}

	public static Enchantment.Cost dynamicCost(int i, int j) {
		return new Enchantment.Cost(i, j);
	}

	public static Enchantment.EnchantmentDefinition definition(
		HolderSet<Item> arg, HolderSet<Item> arg2, int i, int j, Enchantment.Cost arg3, Enchantment.Cost arg4, int k, EquipmentSlotGroup... args
	) {
		return new Enchantment.EnchantmentDefinition(arg, Optional.of(arg2), i, j, arg3, arg4, k, List.of(args));
	}

	public static Enchantment.EnchantmentDefinition definition(
		HolderSet<Item> arg, int i, int j, Enchantment.Cost arg2, Enchantment.Cost arg3, int k, EquipmentSlotGroup... args
	) {
		return new Enchantment.EnchantmentDefinition(arg, Optional.empty(), i, j, arg2, arg3, k, List.of(args));
	}

	public Map<EquipmentSlot, ItemStack> getSlotItems(LivingEntity arg) {
		Map<EquipmentSlot, ItemStack> map = Maps.newEnumMap(EquipmentSlot.class);

		for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
			if (this.matchingSlot(equipmentslot)) {
				ItemStack itemstack = arg.getItemBySlot(equipmentslot);
				if (!itemstack.isEmpty()) {
					map.put(equipmentslot, itemstack);
				}
			}
		}

		return map;
	}

	@Deprecated
	public HolderSet<Item> getSupportedItems() {
		return this.definition.supportedItems();
	}

	public boolean matchingSlot(EquipmentSlot arg) {
		return this.definition.slots().stream().anyMatch(arg2 -> arg2.test(arg));
	}

	@Deprecated
	public boolean isPrimaryItem(ItemStack arg) {
		return this.isSupportedItem(arg) && (this.definition.primaryItems.isEmpty() || arg.is((HolderSet<Item>)this.definition.primaryItems.get()));
	}

	@Deprecated
	public boolean isSupportedItem(ItemStack arg) {
		return arg.is(this.definition.supportedItems);
	}

	public int getWeight() {
		return this.definition.weight();
	}

	public int getAnvilCost() {
		return this.definition.anvilCost();
	}

	public int getMinLevel() {
		return 1;
	}

	public int getMaxLevel() {
		return this.definition.maxLevel();
	}

	public int getMinCost(int i) {
		return this.definition.minCost().calculate(i);
	}

	public int getMaxCost(int i) {
		return this.definition.maxCost().calculate(i);
	}

	public String toString() {
		return "Enchantment " + this.description.getString();
	}

	public static boolean areCompatible(Holder<Enchantment> arg, Holder<Enchantment> arg2) {
		return !arg.equals(arg2) && !((Enchantment)arg.value()).exclusiveSet.contains(arg2) && !((Enchantment)arg2.value()).exclusiveSet.contains(arg);
	}

	public static Component getFullname(Holder<Enchantment> arg, int i) {
		MutableComponent mutablecomponent = ((Enchantment)arg.value()).description.copy();
		if (arg.is(EnchantmentTags.CURSE)) {
			ComponentUtils.mergeStyles(mutablecomponent, Style.EMPTY.withColor(ChatFormatting.RED));
		} else {
			ComponentUtils.mergeStyles(mutablecomponent, Style.EMPTY.withColor(ChatFormatting.GRAY));
		}

		if (i != 1 || ((Enchantment)arg.value()).getMaxLevel() != 1) {
			mutablecomponent.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + i));
		}

		return mutablecomponent;
	}

	@Deprecated
	public boolean canEnchant(ItemStack arg) {
		return this.definition.supportedItems().contains(arg.getItemHolder());
	}

	public <T> List<T> getEffects(DataComponentType<List<T>> arg) {
		return this.effects.getOrDefault(arg, List.of());
	}

	public boolean isImmuneToDamage(ServerLevel arg, int i, Entity arg2, DamageSource arg3) {
		LootContext lootcontext = damageContext(arg, i, arg2, arg3);

		for (ConditionalEffect<DamageImmunity> conditionaleffect : this.getEffects(EnchantmentEffectComponents.DAMAGE_IMMUNITY)) {
			if (conditionaleffect.matches(lootcontext)) {
				return true;
			}
		}

		return false;
	}

	public void modifyDamageProtection(ServerLevel arg, int i, ItemStack arg2, Entity arg3, DamageSource arg4, MutableFloat mutableFloat) {
		LootContext lootcontext = damageContext(arg, i, arg3, arg4);

		for (ConditionalEffect<EnchantmentValueEffect> conditionaleffect : this.getEffects(EnchantmentEffectComponents.DAMAGE_PROTECTION)) {
			if (conditionaleffect.matches(lootcontext)) {
				mutableFloat.setValue(conditionaleffect.effect().process(i, arg3.getRandom(), mutableFloat.floatValue()));
			}
		}
	}

	public void modifyDurabilityChange(ServerLevel arg, int i, ItemStack arg2, MutableFloat mutableFloat) {
		this.modifyItemFilteredCount(EnchantmentEffectComponents.ITEM_DAMAGE, arg, i, arg2, mutableFloat);
	}

	public void modifyAmmoCount(ServerLevel arg, int i, ItemStack arg2, MutableFloat mutableFloat) {
		this.modifyItemFilteredCount(EnchantmentEffectComponents.AMMO_USE, arg, i, arg2, mutableFloat);
	}

	public void modifyPiercingCount(ServerLevel arg, int i, ItemStack arg2, MutableFloat mutableFloat) {
		this.modifyItemFilteredCount(EnchantmentEffectComponents.PROJECTILE_PIERCING, arg, i, arg2, mutableFloat);
	}

	public void modifyBlockExperience(ServerLevel arg, int i, ItemStack arg2, MutableFloat mutableFloat) {
		this.modifyItemFilteredCount(EnchantmentEffectComponents.BLOCK_EXPERIENCE, arg, i, arg2, mutableFloat);
	}

	public void modifyMobExperience(ServerLevel arg, int i, ItemStack arg2, Entity arg3, MutableFloat mutableFloat) {
		this.modifyEntityFilteredValue(EnchantmentEffectComponents.MOB_EXPERIENCE, arg, i, arg2, arg3, mutableFloat);
	}

	public void modifyDurabilityToRepairFromXp(ServerLevel arg, int i, ItemStack arg2, MutableFloat mutableFloat) {
		this.modifyItemFilteredCount(EnchantmentEffectComponents.REPAIR_WITH_XP, arg, i, arg2, mutableFloat);
	}

	public void modifyTridentReturnToOwnerAcceleration(ServerLevel arg, int i, ItemStack arg2, Entity arg3, MutableFloat mutableFloat) {
		this.modifyEntityFilteredValue(EnchantmentEffectComponents.TRIDENT_RETURN_ACCELERATION, arg, i, arg2, arg3, mutableFloat);
	}

	public void modifyTridentSpinAttackStrength(RandomSource arg, int i, MutableFloat mutableFloat) {
		this.modifyUnfilteredValue(EnchantmentEffectComponents.TRIDENT_SPIN_ATTACK_STRENGTH, arg, i, mutableFloat);
	}

	public void modifyFishingTimeReduction(ServerLevel arg, int i, ItemStack arg2, Entity arg3, MutableFloat mutableFloat) {
		this.modifyEntityFilteredValue(EnchantmentEffectComponents.FISHING_TIME_REDUCTION, arg, i, arg2, arg3, mutableFloat);
	}

	public void modifyFishingLuckBonus(ServerLevel arg, int i, ItemStack arg2, Entity arg3, MutableFloat mutableFloat) {
		this.modifyEntityFilteredValue(EnchantmentEffectComponents.FISHING_LUCK_BONUS, arg, i, arg2, arg3, mutableFloat);
	}

	public void modifyDamage(ServerLevel arg, int i, ItemStack arg2, Entity arg3, DamageSource arg4, MutableFloat mutableFloat) {
		this.modifyDamageFilteredValue(EnchantmentEffectComponents.DAMAGE, arg, i, arg2, arg3, arg4, mutableFloat);
	}

	public void modifyFallBasedDamage(ServerLevel arg, int i, ItemStack arg2, Entity arg3, DamageSource arg4, MutableFloat mutableFloat) {
		this.modifyDamageFilteredValue(EnchantmentEffectComponents.SMASH_DAMAGE_PER_FALLEN_BLOCK, arg, i, arg2, arg3, arg4, mutableFloat);
	}

	public void modifyKnockback(ServerLevel arg, int i, ItemStack arg2, Entity arg3, DamageSource arg4, MutableFloat mutableFloat) {
		this.modifyDamageFilteredValue(EnchantmentEffectComponents.KNOCKBACK, arg, i, arg2, arg3, arg4, mutableFloat);
	}

	public void modifyArmorEffectivness(ServerLevel arg, int i, ItemStack arg2, Entity arg3, DamageSource arg4, MutableFloat mutableFloat) {
		this.modifyDamageFilteredValue(EnchantmentEffectComponents.ARMOR_EFFECTIVENESS, arg, i, arg2, arg3, arg4, mutableFloat);
	}

	public void doPostAttack(ServerLevel arg, int i, EnchantedItemInUse arg2, EnchantmentTarget arg3, Entity arg4, DamageSource arg5) {
		for (TargetedConditionalEffect<EnchantmentEntityEffect> targetedconditionaleffect : this.getEffects(EnchantmentEffectComponents.POST_ATTACK)) {
			if (arg3 == targetedconditionaleffect.enchanted()) {
				doPostAttack(targetedconditionaleffect, arg, i, arg2, arg4, arg5);
			}
		}
	}

	public static void doPostAttack(
		TargetedConditionalEffect<EnchantmentEntityEffect> arg, ServerLevel arg2, int i, EnchantedItemInUse arg3, Entity arg4, DamageSource arg5
	) {
		if (arg.matches(damageContext(arg2, i, arg4, arg5))) {
			Entity entity = switch (arg.affected()) {
				case ATTACKER -> arg5.getEntity();
				case DAMAGING_ENTITY -> arg5.getDirectEntity();
				case VICTIM -> arg4;
			};
			if (entity != null) {
				((EnchantmentEntityEffect)arg.effect()).apply(arg2, i, arg3, entity, entity.position());
			}
		}
	}

	public void modifyProjectileCount(ServerLevel arg, int i, ItemStack arg2, Entity arg3, MutableFloat mutableFloat) {
		this.modifyEntityFilteredValue(EnchantmentEffectComponents.PROJECTILE_COUNT, arg, i, arg2, arg3, mutableFloat);
	}

	public void modifyProjectileSpread(ServerLevel arg, int i, ItemStack arg2, Entity arg3, MutableFloat mutableFloat) {
		this.modifyEntityFilteredValue(EnchantmentEffectComponents.PROJECTILE_SPREAD, arg, i, arg2, arg3, mutableFloat);
	}

	public void modifyCrossbowChargeTime(RandomSource arg, int i, MutableFloat mutableFloat) {
		this.modifyUnfilteredValue(EnchantmentEffectComponents.CROSSBOW_CHARGE_TIME, arg, i, mutableFloat);
	}

	public void modifyUnfilteredValue(DataComponentType<EnchantmentValueEffect> arg, RandomSource arg2, int i, MutableFloat mutableFloat) {
		EnchantmentValueEffect enchantmentvalueeffect = this.effects.get(arg);
		if (enchantmentvalueeffect != null) {
			mutableFloat.setValue(enchantmentvalueeffect.process(i, arg2, mutableFloat.floatValue()));
		}
	}

	public void tick(ServerLevel arg, int i, EnchantedItemInUse arg2, Entity arg3) {
		applyEffects(
			this.getEffects(EnchantmentEffectComponents.TICK), entityContext(arg, i, arg3, arg3.position()), arg4 -> arg4.apply(arg, i, arg2, arg3, arg3.position())
		);
	}

	public void onProjectileSpawned(ServerLevel arg, int i, EnchantedItemInUse arg2, Entity arg3) {
		applyEffects(
			this.getEffects(EnchantmentEffectComponents.PROJECTILE_SPAWNED),
			entityContext(arg, i, arg3, arg3.position()),
			arg4 -> arg4.apply(arg, i, arg2, arg3, arg3.position())
		);
	}

	public void onHitBlock(ServerLevel arg, int i, EnchantedItemInUse arg2, Entity arg3, Vec3 arg4, BlockState arg5) {
		applyEffects(
			this.getEffects(EnchantmentEffectComponents.HIT_BLOCK), blockHitContext(arg, i, arg3, arg4, arg5), arg5x -> arg5x.apply(arg, i, arg2, arg3, arg4)
		);
	}

	public void modifyItemFilteredCount(
		DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> arg, ServerLevel arg2, int i, ItemStack arg3, MutableFloat mutableFloat
	) {
		applyEffects(this.getEffects(arg), itemContext(arg2, i, arg3), arg2x -> mutableFloat.setValue(arg2x.process(i, arg2.getRandom(), mutableFloat.getValue())));
	}

	public void modifyEntityFilteredValue(
		DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> arg, ServerLevel arg2, int i, ItemStack arg3, Entity arg4, MutableFloat mutableFloat
	) {
		applyEffects(
			this.getEffects(arg),
			entityContext(arg2, i, arg4, arg4.position()),
			arg2x -> mutableFloat.setValue(arg2x.process(i, arg4.getRandom(), mutableFloat.floatValue()))
		);
	}

	public void modifyDamageFilteredValue(
		DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> arg,
		ServerLevel arg2,
		int i,
		ItemStack arg3,
		Entity arg4,
		DamageSource arg5,
		MutableFloat mutableFloat
	) {
		applyEffects(
			this.getEffects(arg), damageContext(arg2, i, arg4, arg5), arg2x -> mutableFloat.setValue(arg2x.process(i, arg4.getRandom(), mutableFloat.floatValue()))
		);
	}

	public static LootContext damageContext(ServerLevel arg, int i, Entity arg2, DamageSource arg3) {
		LootParams lootparams = new LootParams.Builder(arg)
			.withParameter(LootContextParams.THIS_ENTITY, arg2)
			.withParameter(LootContextParams.ENCHANTMENT_LEVEL, i)
			.withParameter(LootContextParams.ORIGIN, arg2.position())
			.withParameter(LootContextParams.DAMAGE_SOURCE, arg3)
			.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, arg3.getEntity())
			.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, arg3.getDirectEntity())
			.create(LootContextParamSets.ENCHANTED_DAMAGE);
		return new LootContext.Builder(lootparams).create(Optional.empty());
	}

	public static LootContext itemContext(ServerLevel arg, int i, ItemStack arg2) {
		LootParams lootparams = new LootParams.Builder(arg)
			.withParameter(LootContextParams.TOOL, arg2)
			.withParameter(LootContextParams.ENCHANTMENT_LEVEL, i)
			.create(LootContextParamSets.ENCHANTED_ITEM);
		return new LootContext.Builder(lootparams).create(Optional.empty());
	}

	public static LootContext locationContext(ServerLevel arg, int i, Entity arg2, boolean bl) {
		LootParams lootparams = new LootParams.Builder(arg)
			.withParameter(LootContextParams.THIS_ENTITY, arg2)
			.withParameter(LootContextParams.ENCHANTMENT_LEVEL, i)
			.withParameter(LootContextParams.ORIGIN, arg2.position())
			.withParameter(LootContextParams.ENCHANTMENT_ACTIVE, bl)
			.create(LootContextParamSets.ENCHANTED_LOCATION);
		return new LootContext.Builder(lootparams).create(Optional.empty());
	}

	public static LootContext entityContext(ServerLevel arg, int i, Entity arg2, Vec3 arg3) {
		LootParams lootparams = new LootParams.Builder(arg)
			.withParameter(LootContextParams.THIS_ENTITY, arg2)
			.withParameter(LootContextParams.ENCHANTMENT_LEVEL, i)
			.withParameter(LootContextParams.ORIGIN, arg3)
			.create(LootContextParamSets.ENCHANTED_ENTITY);
		return new LootContext.Builder(lootparams).create(Optional.empty());
	}

	public static LootContext blockHitContext(ServerLevel arg, int i, Entity arg2, Vec3 arg3, BlockState arg4) {
		LootParams lootparams = new LootParams.Builder(arg)
			.withParameter(LootContextParams.THIS_ENTITY, arg2)
			.withParameter(LootContextParams.ENCHANTMENT_LEVEL, i)
			.withParameter(LootContextParams.ORIGIN, arg3)
			.withParameter(LootContextParams.BLOCK_STATE, arg4)
			.create(LootContextParamSets.HIT_BLOCK);
		return new LootContext.Builder(lootparams).create(Optional.empty());
	}

	public static <T> void applyEffects(List<ConditionalEffect<T>> list, LootContext arg, Consumer<T> consumer) {
		for (ConditionalEffect<T> conditionaleffect : list) {
			if (conditionaleffect.matches(arg)) {
				consumer.accept(conditionaleffect.effect());
			}
		}
	}

	public void runLocationChangedEffects(ServerLevel arg, int i, EnchantedItemInUse arg2, LivingEntity arg3) {
		EquipmentSlot equipmentslot = arg2.inSlot();
		if (equipmentslot != null) {
			Map<Enchantment, Set<EnchantmentLocationBasedEffect>> map = arg3.activeLocationDependentEnchantments(equipmentslot);
			if (!this.matchingSlot(equipmentslot)) {
				Set<EnchantmentLocationBasedEffect> set1 = (Set<EnchantmentLocationBasedEffect>)map.remove(this);
				if (set1 != null) {
					set1.forEach(arg3x -> arg3x.onDeactivated(arg2, arg3, arg3.position(), i));
				}
			} else {
				Set<EnchantmentLocationBasedEffect> set = (Set<EnchantmentLocationBasedEffect>)map.get(this);

				for (ConditionalEffect<EnchantmentLocationBasedEffect> conditionaleffect : this.getEffects(EnchantmentEffectComponents.LOCATION_CHANGED)) {
					EnchantmentLocationBasedEffect enchantmentlocationbasedeffect = conditionaleffect.effect();
					boolean flag = set != null && set.contains(enchantmentlocationbasedeffect);
					if (conditionaleffect.matches(locationContext(arg, i, arg3, flag))) {
						if (!flag) {
							if (set == null) {
								set = new ObjectArraySet<>();
								map.put(this, set);
							}

							set.add(enchantmentlocationbasedeffect);
						}

						enchantmentlocationbasedeffect.onChangedBlock(arg, i, arg2, arg3, arg3.position(), !flag);
					} else if (set != null && set.remove(enchantmentlocationbasedeffect)) {
						enchantmentlocationbasedeffect.onDeactivated(arg2, arg3, arg3.position(), i);
					}
				}

				if (set != null && set.isEmpty()) {
					map.remove(this);
				}
			}
		}
	}

	public void stopLocationBasedEffects(int i, EnchantedItemInUse arg, LivingEntity arg2) {
		EquipmentSlot equipmentslot = arg.inSlot();
		if (equipmentslot != null) {
			Set<EnchantmentLocationBasedEffect> set = (Set<EnchantmentLocationBasedEffect>)arg2.activeLocationDependentEnchantments(equipmentslot).remove(this);
			if (set != null) {
				for (EnchantmentLocationBasedEffect enchantmentlocationbasedeffect : set) {
					enchantmentlocationbasedeffect.onDeactivated(arg, arg2, arg2.position(), i);
				}
			}
		}
	}

	public static Enchantment.Builder enchantment(Enchantment.EnchantmentDefinition arg) {
		return new Enchantment.Builder(arg);
	}

	public static class Builder {
		private final Enchantment.EnchantmentDefinition definition;
		private HolderSet<Enchantment> exclusiveSet = HolderSet.direct();
		private final Map<DataComponentType<?>, List<?>> effectLists = new HashMap();
		private final DataComponentMap.Builder effectMapBuilder = DataComponentMap.builder();
		protected UnaryOperator<MutableComponent> nameFactory = UnaryOperator.identity();

		public Builder(Enchantment.EnchantmentDefinition arg) {
			this.definition = arg;
		}

		public Enchantment.Builder exclusiveWith(HolderSet<Enchantment> arg) {
			this.exclusiveSet = arg;
			return this;
		}

		public <E> Enchantment.Builder withEffect(DataComponentType<List<ConditionalEffect<E>>> arg, E object, LootItemCondition.Builder arg2) {
			this.getEffectsList(arg).add(new ConditionalEffect<>(object, Optional.of(arg2.build())));
			return this;
		}

		public <E> Enchantment.Builder withEffect(DataComponentType<List<ConditionalEffect<E>>> arg, E object) {
			this.getEffectsList(arg).add(new ConditionalEffect<>(object, Optional.empty()));
			return this;
		}

		public <E> Enchantment.Builder withEffect(
			DataComponentType<List<TargetedConditionalEffect<E>>> arg, EnchantmentTarget arg2, EnchantmentTarget arg3, E object, LootItemCondition.Builder arg4
		) {
			this.getEffectsList(arg).add(new TargetedConditionalEffect<>(arg2, arg3, object, Optional.of(arg4.build())));
			return this;
		}

		public <E> Enchantment.Builder withEffect(DataComponentType<List<TargetedConditionalEffect<E>>> arg, EnchantmentTarget arg2, EnchantmentTarget arg3, E object) {
			this.getEffectsList(arg).add(new TargetedConditionalEffect<>(arg2, arg3, object, Optional.empty()));
			return this;
		}

		public Enchantment.Builder withEffect(DataComponentType<List<EnchantmentAttributeEffect>> arg, EnchantmentAttributeEffect arg2) {
			this.getEffectsList(arg).add(arg2);
			return this;
		}

		public <E> Enchantment.Builder withSpecialEffect(DataComponentType<E> arg, E object) {
			this.effectMapBuilder.set(arg, object);
			return this;
		}

		public Enchantment.Builder withEffect(DataComponentType<Unit> arg) {
			this.effectMapBuilder.set(arg, Unit.INSTANCE);
			return this;
		}

		public Enchantment.Builder withCustomName(UnaryOperator<MutableComponent> nameFactory) {
			this.nameFactory = nameFactory;
			return this;
		}

		private <E> List<E> getEffectsList(DataComponentType<List<E>> arg) {
			return (List<E>)this.effectLists.computeIfAbsent(arg, arg2 -> {
				ArrayList<E> arraylist = new ArrayList();
				this.effectMapBuilder.set(arg, arraylist);
				return arraylist;
			});
		}

		public Enchantment build(ResourceLocation arg) {
			return new Enchantment(
				(Component)this.nameFactory.apply(Component.translatable(Util.makeDescriptionId("enchantment", arg))),
				this.definition,
				this.exclusiveSet,
				this.effectMapBuilder.build()
			);
		}
	}

	public record Cost(int base, int perLevelAboveFirst) {
		public static final Codec<Enchantment.Cost> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Codec.INT.fieldOf("base").forGetter(Enchantment.Cost::base), Codec.INT.fieldOf("per_level_above_first").forGetter(Enchantment.Cost::perLevelAboveFirst)
				)
				.apply(instance, Enchantment.Cost::new)
		);

		public int calculate(int i) {
			return this.base + this.perLevelAboveFirst * (i - 1);
		}
	}

	public record EnchantmentDefinition(
		HolderSet<Item> supportedItems,
		Optional<HolderSet<Item>> primaryItems,
		int weight,
		int maxLevel,
		Enchantment.Cost minCost,
		Enchantment.Cost maxCost,
		int anvilCost,
		List<EquipmentSlotGroup> slots
	) {
		public static final MapCodec<Enchantment.EnchantmentDefinition> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("supported_items").forGetter(Enchantment.EnchantmentDefinition::supportedItems),
					RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("primary_items").forGetter(Enchantment.EnchantmentDefinition::primaryItems),
					ExtraCodecs.intRange(1, 1024).fieldOf("weight").forGetter(Enchantment.EnchantmentDefinition::weight),
					ExtraCodecs.intRange(1, 255).fieldOf("max_level").forGetter(Enchantment.EnchantmentDefinition::maxLevel),
					Enchantment.Cost.CODEC.fieldOf("min_cost").forGetter(Enchantment.EnchantmentDefinition::minCost),
					Enchantment.Cost.CODEC.fieldOf("max_cost").forGetter(Enchantment.EnchantmentDefinition::maxCost),
					ExtraCodecs.NON_NEGATIVE_INT.fieldOf("anvil_cost").forGetter(Enchantment.EnchantmentDefinition::anvilCost),
					EquipmentSlotGroup.CODEC.listOf().fieldOf("slots").forGetter(Enchantment.EnchantmentDefinition::slots)
				)
				.apply(instance, Enchantment.EnchantmentDefinition::new)
		);
	}
}
