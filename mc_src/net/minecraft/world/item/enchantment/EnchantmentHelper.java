package net.minecraft.world.item.enchantment;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.item.enchantment.providers.EnchantmentProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;

public class EnchantmentHelper {
	@Deprecated
	public static int getItemEnchantmentLevel(Holder<Enchantment> arg, ItemStack arg2) {
		return arg2.getEnchantmentLevel(arg);
	}

	public static int getTagEnchantmentLevel(Holder<Enchantment> arg, ItemStack arg2) {
		ItemEnchantments itemenchantments = arg2.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		return itemenchantments.getLevel(arg);
	}

	public static ItemEnchantments updateEnchantments(ItemStack arg, Consumer<ItemEnchantments.Mutable> consumer) {
		DataComponentType<ItemEnchantments> datacomponenttype = getComponentType(arg);
		ItemEnchantments itemenchantments = arg.get(datacomponenttype);
		if (itemenchantments == null) {
			return ItemEnchantments.EMPTY;
		} else {
			ItemEnchantments.Mutable itemenchantments$mutable = new ItemEnchantments.Mutable(itemenchantments);
			consumer.accept(itemenchantments$mutable);
			ItemEnchantments itemenchantments1 = itemenchantments$mutable.toImmutable();
			arg.set(datacomponenttype, itemenchantments1);
			return itemenchantments1;
		}
	}

	public static boolean canStoreEnchantments(ItemStack arg) {
		return arg.has(getComponentType(arg));
	}

	public static void setEnchantments(ItemStack arg, ItemEnchantments arg2) {
		arg.set(getComponentType(arg), arg2);
	}

	public static ItemEnchantments getEnchantmentsForCrafting(ItemStack arg) {
		return arg.getOrDefault(getComponentType(arg), ItemEnchantments.EMPTY);
	}

	public static DataComponentType<ItemEnchantments> getComponentType(ItemStack arg) {
		return arg.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
	}

	public static boolean hasAnyEnchantments(ItemStack arg) {
		return !arg.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
			|| !arg.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
	}

	public static int processDurabilityChange(ServerLevel arg, ItemStack arg2, int i) {
		MutableFloat mutablefloat = new MutableFloat(i);
		runIterationOnItem(arg2, (arg3, ix) -> arg3.value().modifyDurabilityChange(arg, ix, arg2, mutablefloat));
		return mutablefloat.intValue();
	}

	public static int processAmmoUse(ServerLevel arg, ItemStack arg2, ItemStack arg3, int i) {
		MutableFloat mutablefloat = new MutableFloat(i);
		runIterationOnItem(arg2, (arg3x, ix) -> ((Enchantment)arg3x.value()).modifyAmmoCount(arg, ix, arg3, mutablefloat));
		return mutablefloat.intValue();
	}

	public static int processBlockExperience(ServerLevel arg, ItemStack arg2, int i) {
		MutableFloat mutablefloat = new MutableFloat(i);
		runIterationOnItem(arg2, (arg3, ix) -> arg3.value().modifyBlockExperience(arg, ix, arg2, mutablefloat));
		return mutablefloat.intValue();
	}

	public static int processMobExperience(ServerLevel arg, @Nullable Entity arg2, Entity arg3, int i) {
		if (arg2 instanceof LivingEntity livingentity) {
			MutableFloat mutablefloat = new MutableFloat(i);
			runIterationOnEquipment(livingentity, (arg3x, ix, arg4) -> ((Enchantment)arg3x.value()).modifyMobExperience(arg, ix, arg4.itemStack(), arg3, mutablefloat));
			return mutablefloat.intValue();
		} else {
			return i;
		}
	}

	public static ItemStack createBook(EnchantmentInstance arg) {
		ItemStack itemstack = new ItemStack(Items.ENCHANTED_BOOK);
		itemstack.enchant(arg.enchantment, arg.level);
		return itemstack;
	}

	public static void runIterationOnItem(ItemStack arg, EnchantmentHelper.EnchantmentVisitor arg2) {
		ItemEnchantments itemenchantments = arg.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		HolderLookup.RegistryLookup<Enchantment> lookup = CommonHooks.resolveLookup(Registries.ENCHANTMENT);
		if (lookup != null) {
			itemenchantments = arg.getAllEnchantments(lookup);
		}

		for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
			arg2.accept((Holder<Enchantment>)entry.getKey(), entry.getIntValue());
		}
	}

	public static void runIterationOnItem(ItemStack arg, EquipmentSlot arg2, LivingEntity arg3, EnchantmentHelper.EnchantmentInSlotVisitor arg4) {
		if (!arg.isEmpty()) {
			ItemEnchantments itemenchantments = arg.get(DataComponents.ENCHANTMENTS);
			itemenchantments = arg.getAllEnchantments(arg3.registryAccess().lookupOrThrow(Registries.ENCHANTMENT));
			if (itemenchantments != null && !itemenchantments.isEmpty()) {
				EnchantedItemInUse enchantediteminuse = new EnchantedItemInUse(arg, arg2, arg3);

				for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
					Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
					if (holder.value().matchingSlot(arg2)) {
						arg4.accept(holder, entry.getIntValue(), enchantediteminuse);
					}
				}
			}
		}
	}

	public static void runIterationOnEquipment(LivingEntity arg, EnchantmentHelper.EnchantmentInSlotVisitor arg2) {
		for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
			runIterationOnItem(arg.getItemBySlot(equipmentslot), equipmentslot, arg, arg2);
		}
	}

	public static boolean isImmuneToDamage(ServerLevel arg, LivingEntity arg2, DamageSource arg3) {
		MutableBoolean mutableboolean = new MutableBoolean();
		runIterationOnEquipment(arg2, (arg4, i, arg5) -> mutableboolean.setValue(mutableboolean.isTrue() || arg4.value().isImmuneToDamage(arg, i, arg2, arg3)));
		return mutableboolean.isTrue();
	}

	public static float getDamageProtection(ServerLevel arg, LivingEntity arg2, DamageSource arg3) {
		MutableFloat mutablefloat = new MutableFloat(0.0F);
		runIterationOnEquipment(arg2, (arg4, i, arg5) -> arg4.value().modifyDamageProtection(arg, i, arg5.itemStack(), arg2, arg3, mutablefloat));
		return mutablefloat.floatValue();
	}

	public static float modifyDamage(ServerLevel arg, ItemStack arg2, Entity arg3, DamageSource arg4, float f) {
		MutableFloat mutablefloat = new MutableFloat(f);
		runIterationOnItem(arg2, (arg5, i) -> arg5.value().modifyDamage(arg, i, arg2, arg3, arg4, mutablefloat));
		return mutablefloat.floatValue();
	}

	public static float modifyFallBasedDamage(ServerLevel arg, ItemStack arg2, Entity arg3, DamageSource arg4, float f) {
		MutableFloat mutablefloat = new MutableFloat(f);
		runIterationOnItem(arg2, (arg5, i) -> arg5.value().modifyFallBasedDamage(arg, i, arg2, arg3, arg4, mutablefloat));
		return mutablefloat.floatValue();
	}

	public static float modifyArmorEffectiveness(ServerLevel arg, ItemStack arg2, Entity arg3, DamageSource arg4, float f) {
		MutableFloat mutablefloat = new MutableFloat(f);
		runIterationOnItem(arg2, (arg5, i) -> arg5.value().modifyArmorEffectivness(arg, i, arg2, arg3, arg4, mutablefloat));
		return mutablefloat.floatValue();
	}

	public static float modifyKnockback(ServerLevel arg, ItemStack arg2, Entity arg3, DamageSource arg4, float f) {
		MutableFloat mutablefloat = new MutableFloat(f);
		runIterationOnItem(arg2, (arg5, i) -> arg5.value().modifyKnockback(arg, i, arg2, arg3, arg4, mutablefloat));
		return mutablefloat.floatValue();
	}

	public static void doPostAttackEffects(ServerLevel arg, Entity arg2, DamageSource arg3) {
		if (arg3.getEntity() instanceof LivingEntity livingentity) {
			doPostAttackEffectsWithItemSource(arg, arg2, arg3, livingentity.getWeaponItem());
		} else {
			doPostAttackEffectsWithItemSource(arg, arg2, arg3, null);
		}
	}

	public static void doPostAttackEffectsWithItemSource(ServerLevel arg, Entity arg2, DamageSource arg3, @Nullable ItemStack arg4) {
		doPostAttackEffectsWithItemSourceOnBreak(arg, arg2, arg3, arg4, null);
	}

	public static void doPostAttackEffectsWithItemSourceOnBreak(
		ServerLevel arg, Entity arg2, DamageSource arg3, @Nullable ItemStack arg4, @Nullable Consumer<Item> consumer
	) {
		if (arg2 instanceof LivingEntity livingentity) {
			runIterationOnEquipment(livingentity, (arg4x, i, arg5) -> ((Enchantment)arg4x.value()).doPostAttack(arg, i, arg5, EnchantmentTarget.VICTIM, arg2, arg3));
		}

		if (arg4 != null) {
			if (arg3.getEntity() instanceof LivingEntity livingentity1) {
				runIterationOnItem(
					arg4,
					EquipmentSlot.MAINHAND,
					livingentity1,
					(arg4x, i, arg5) -> ((Enchantment)arg4x.value()).doPostAttack(arg, i, arg5, EnchantmentTarget.ATTACKER, arg2, arg3)
				);
			} else if (consumer != null) {
				EnchantedItemInUse enchantediteminuse = new EnchantedItemInUse(arg4, null, null, consumer);
				runIterationOnItem(arg4, (arg4x, i) -> ((Enchantment)arg4x.value()).doPostAttack(arg, i, enchantediteminuse, EnchantmentTarget.ATTACKER, arg2, arg3));
			}
		}
	}

	public static void runLocationChangedEffects(ServerLevel arg, LivingEntity arg2) {
		runIterationOnEquipment(arg2, (arg3, i, arg4) -> arg3.value().runLocationChangedEffects(arg, i, arg4, arg2));
	}

	public static void runLocationChangedEffects(ServerLevel arg, ItemStack arg2, LivingEntity arg3, EquipmentSlot arg4) {
		runIterationOnItem(arg2, arg4, arg3, (arg3x, i, arg4x) -> ((Enchantment)arg3x.value()).runLocationChangedEffects(arg, i, arg4x, arg3));
	}

	public static void stopLocationBasedEffects(LivingEntity arg) {
		runIterationOnEquipment(arg, (arg2, i, arg3) -> arg2.value().stopLocationBasedEffects(i, arg3, arg));
	}

	public static void stopLocationBasedEffects(ItemStack arg, LivingEntity arg2, EquipmentSlot arg3) {
		runIterationOnItem(arg, arg3, arg2, (arg2x, i, arg3x) -> ((Enchantment)arg2x.value()).stopLocationBasedEffects(i, arg3x, arg2));
	}

	public static void tickEffects(ServerLevel arg, LivingEntity arg2) {
		runIterationOnEquipment(arg2, (arg3, i, arg4) -> arg3.value().tick(arg, i, arg4, arg2));
	}

	public static int getEnchantmentLevel(Holder<Enchantment> arg, LivingEntity arg2) {
		Iterable<ItemStack> iterable = ((Enchantment)arg.value()).getSlotItems(arg2).values();
		int i = 0;

		for (ItemStack itemstack : iterable) {
			int j = getItemEnchantmentLevel(arg, itemstack);
			if (j > i) {
				i = j;
			}
		}

		return i;
	}

	public static int processProjectileCount(ServerLevel arg, ItemStack arg2, Entity arg3, int i) {
		MutableFloat mutablefloat = new MutableFloat(i);
		runIterationOnItem(arg2, (arg4, ix) -> arg4.value().modifyProjectileCount(arg, ix, arg2, arg3, mutablefloat));
		return Math.max(0, mutablefloat.intValue());
	}

	public static float processProjectileSpread(ServerLevel arg, ItemStack arg2, Entity arg3, float f) {
		MutableFloat mutablefloat = new MutableFloat(f);
		runIterationOnItem(arg2, (arg4, i) -> arg4.value().modifyProjectileSpread(arg, i, arg2, arg3, mutablefloat));
		return Math.max(0.0F, mutablefloat.floatValue());
	}

	public static int getPiercingCount(ServerLevel arg, ItemStack arg2, ItemStack arg3) {
		MutableFloat mutablefloat = new MutableFloat(0.0F);
		runIterationOnItem(arg2, (arg3x, i) -> ((Enchantment)arg3x.value()).modifyPiercingCount(arg, i, arg3, mutablefloat));
		return Math.max(0, mutablefloat.intValue());
	}

	public static void onProjectileSpawned(ServerLevel arg, ItemStack arg2, Projectile arg3, Consumer<Item> consumer) {
		LivingEntity livingentity = arg3.getOwner() instanceof LivingEntity livingentity1 ? livingentity1 : null;
		EnchantedItemInUse enchantediteminuse = new EnchantedItemInUse(arg2, null, livingentity, consumer);
		runIterationOnItem(arg2, (arg3x, i) -> ((Enchantment)arg3x.value()).onProjectileSpawned(arg, i, enchantediteminuse, arg3));
	}

	public static void onHitBlock(
		ServerLevel arg, ItemStack arg2, @Nullable LivingEntity arg3, Entity arg4, @Nullable EquipmentSlot arg5, Vec3 arg6, BlockState arg7, Consumer<Item> consumer
	) {
		EnchantedItemInUse enchantediteminuse = new EnchantedItemInUse(arg2, arg5, arg3, consumer);
		runIterationOnItem(arg2, (arg5x, i) -> ((Enchantment)arg5x.value()).onHitBlock(arg, i, enchantediteminuse, arg4, arg6, arg7));
	}

	public static int modifyDurabilityToRepairFromXp(ServerLevel arg, ItemStack arg2, int i) {
		MutableFloat mutablefloat = new MutableFloat(i);
		runIterationOnItem(arg2, (arg3, ix) -> arg3.value().modifyDurabilityToRepairFromXp(arg, ix, arg2, mutablefloat));
		return Math.max(0, mutablefloat.intValue());
	}

	public static float processEquipmentDropChance(ServerLevel arg, LivingEntity arg2, DamageSource arg3, float f) {
		MutableFloat mutablefloat = new MutableFloat(f);
		RandomSource randomsource = arg2.getRandom();
		runIterationOnEquipment(arg2, (arg4, i, arg5) -> {
			LootContext lootcontext = Enchantment.damageContext(arg, i, arg2, arg3);
			arg4.value().getEffects(EnchantmentEffectComponents.EQUIPMENT_DROPS).forEach(argxx -> {
				if (argxx.enchanted() == EnchantmentTarget.VICTIM && argxx.affected() == EnchantmentTarget.VICTIM && argxx.matches(lootcontext)) {
					mutablefloat.setValue(((EnchantmentValueEffect)argxx.effect()).process(i, randomsource, mutablefloat.floatValue()));
				}
			});
		});
		if (arg3.getEntity() instanceof LivingEntity livingentity) {
			runIterationOnEquipment(livingentity, (arg4, i, arg5) -> {
				LootContext lootcontext = Enchantment.damageContext(arg, i, arg2, arg3);
				arg4.value().getEffects(EnchantmentEffectComponents.EQUIPMENT_DROPS).forEach(argxx -> {
					if (argxx.enchanted() == EnchantmentTarget.ATTACKER && argxx.affected() == EnchantmentTarget.VICTIM && argxx.matches(lootcontext)) {
						mutablefloat.setValue(((EnchantmentValueEffect)argxx.effect()).process(i, randomsource, mutablefloat.floatValue()));
					}
				});
			});
		}

		return mutablefloat.floatValue();
	}

	public static void forEachModifier(ItemStack arg, EquipmentSlotGroup arg2, BiConsumer<Holder<Attribute>, AttributeModifier> biConsumer) {
		runIterationOnItem(arg, (arg2x, i) -> ((Enchantment)arg2x.value()).getEffects(EnchantmentEffectComponents.ATTRIBUTES).forEach(arg3 -> {
			if (((Enchantment)arg2x.value()).definition().slots().contains(arg2)) {
				biConsumer.accept(arg3.attribute(), arg3.getModifier(i, arg2));
			}
		}));
	}

	public static void forEachModifier(ItemStack arg, EquipmentSlot arg2, BiConsumer<Holder<Attribute>, AttributeModifier> biConsumer) {
		runIterationOnItem(arg, (arg2x, i) -> ((Enchantment)arg2x.value()).getEffects(EnchantmentEffectComponents.ATTRIBUTES).forEach(arg3 -> {
			if (((Enchantment)arg2x.value()).matchingSlot(arg2)) {
				biConsumer.accept(arg3.attribute(), arg3.getModifier(i, arg2));
			}
		}));
	}

	public static int getFishingLuckBonus(ServerLevel arg, ItemStack arg2, Entity arg3) {
		MutableFloat mutablefloat = new MutableFloat(0.0F);
		runIterationOnItem(arg2, (arg4, i) -> arg4.value().modifyFishingLuckBonus(arg, i, arg2, arg3, mutablefloat));
		return Math.max(0, mutablefloat.intValue());
	}

	public static float getFishingTimeReduction(ServerLevel arg, ItemStack arg2, Entity arg3) {
		MutableFloat mutablefloat = new MutableFloat(0.0F);
		runIterationOnItem(arg2, (arg4, i) -> arg4.value().modifyFishingTimeReduction(arg, i, arg2, arg3, mutablefloat));
		return Math.max(0.0F, mutablefloat.floatValue());
	}

	public static int getTridentReturnToOwnerAcceleration(ServerLevel arg, ItemStack arg2, Entity arg3) {
		MutableFloat mutablefloat = new MutableFloat(0.0F);
		runIterationOnItem(arg2, (arg4, i) -> arg4.value().modifyTridentReturnToOwnerAcceleration(arg, i, arg2, arg3, mutablefloat));
		return Math.max(0, mutablefloat.intValue());
	}

	public static float modifyCrossbowChargingTime(ItemStack arg, LivingEntity arg2, float f) {
		MutableFloat mutablefloat = new MutableFloat(f);
		runIterationOnItem(arg, (arg2x, i) -> ((Enchantment)arg2x.value()).modifyCrossbowChargeTime(arg2.getRandom(), i, mutablefloat));
		return Math.max(0.0F, mutablefloat.floatValue());
	}

	public static float getTridentSpinAttackStrength(ItemStack arg, LivingEntity arg2) {
		MutableFloat mutablefloat = new MutableFloat(0.0F);
		runIterationOnItem(arg, (arg2x, i) -> ((Enchantment)arg2x.value()).modifyTridentSpinAttackStrength(arg2.getRandom(), i, mutablefloat));
		return mutablefloat.floatValue();
	}

	public static boolean hasTag(ItemStack arg, TagKey<Enchantment> arg2) {
		ItemEnchantments itemenchantments = arg.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		HolderLookup.RegistryLookup<Enchantment> lookup = CommonHooks.resolveLookup(Registries.ENCHANTMENT);
		if (lookup != null) {
			itemenchantments = arg.getAllEnchantments(lookup);
		}

		for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
			Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
			if (holder.is(arg2)) {
				return true;
			}
		}

		return false;
	}

	public static boolean has(ItemStack arg, DataComponentType<?> arg2) {
		MutableBoolean mutableboolean = new MutableBoolean(false);
		runIterationOnItem(arg, (arg2x, i) -> {
			if (((Enchantment)arg2x.value()).effects().has(arg2)) {
				mutableboolean.setTrue();
			}
		});
		return mutableboolean.booleanValue();
	}

	public static <T> Optional<T> pickHighestLevel(ItemStack arg, DataComponentType<List<T>> arg2) {
		Pair<List<T>, Integer> pair = getHighestLevel(arg, arg2);
		if (pair != null) {
			List<T> list = pair.getFirst();
			int i = pair.getSecond();
			return Optional.of(list.get(Math.min(i, list.size()) - 1));
		} else {
			return Optional.empty();
		}
	}

	@Nullable
	public static <T> Pair<T, Integer> getHighestLevel(ItemStack arg, DataComponentType<T> arg2) {
		MutableObject<Pair<T, Integer>> mutableobject = new MutableObject<>();
		runIterationOnItem(arg, (arg2x, i) -> {
			if (mutableobject.getValue() == null || mutableobject.getValue().getSecond() < i) {
				T t = ((Enchantment)arg2x.value()).effects().get(arg2);
				if (t != null) {
					mutableobject.setValue(Pair.of(t, i));
				}
			}
		});
		return mutableobject.getValue();
	}

	public static Optional<EnchantedItemInUse> getRandomItemWith(DataComponentType<?> arg, LivingEntity arg2, Predicate<ItemStack> predicate) {
		List<EnchantedItemInUse> list = new ArrayList();

		for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
			ItemStack itemstack = arg2.getItemBySlot(equipmentslot);
			if (predicate.test(itemstack)) {
				ItemEnchantments itemenchantments = itemstack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

				for (Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) {
					Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
					if (holder.value().effects().has(arg) && holder.value().matchingSlot(equipmentslot)) {
						list.add(new EnchantedItemInUse(itemstack, equipmentslot, arg2));
					}
				}
			}
		}

		return Util.getRandomSafe(list, arg2.getRandom());
	}

	public static int getEnchantmentCost(RandomSource arg, int j, int k, ItemStack arg2) {
		Enchantable enchantable = arg2.get(DataComponents.ENCHANTABLE);
		if (enchantable == null) {
			return 0;
		} else {
			if (k > 15) {
				k = 15;
			}

			int i = arg.nextInt(8) + 1 + (k >> 1) + arg.nextInt(k + 1);
			if (j == 0) {
				return Math.max(i / 3, 1);
			} else {
				return j == 1 ? i * 2 / 3 + 1 : Math.max(i, k * 2);
			}
		}
	}

	public static ItemStack enchantItem(RandomSource arg, ItemStack arg2, int i, RegistryAccess arg3, Optional<? extends HolderSet<Enchantment>> optional) {
		return enchantItem(
			arg,
			arg2,
			i,
			(Stream<Holder<Enchantment>>)optional.map(HolderSet::stream).orElseGet(() -> arg3.lookupOrThrow(Registries.ENCHANTMENT).listElements().map(argxx -> argxx))
		);
	}

	public static ItemStack enchantItem(RandomSource arg, ItemStack arg2, int i, Stream<Holder<Enchantment>> stream) {
		List<EnchantmentInstance> list = selectEnchantment(arg, arg2, i, stream);
		if (arg2.is(Items.BOOK)) {
			arg2 = new ItemStack(Items.ENCHANTED_BOOK);
		}

		for (EnchantmentInstance enchantmentinstance : list) {
			arg2.enchant(enchantmentinstance.enchantment, enchantmentinstance.level);
		}

		return arg2;
	}

	public static List<EnchantmentInstance> selectEnchantment(RandomSource arg, ItemStack arg2, int i, Stream<Holder<Enchantment>> stream) {
		List<EnchantmentInstance> list = Lists.<EnchantmentInstance>newArrayList();
		Enchantable enchantable = arg2.get(DataComponents.ENCHANTABLE);
		if (enchantable == null) {
			return list;
		} else {
			i += 1 + arg.nextInt(enchantable.value() / 4 + 1) + arg.nextInt(enchantable.value() / 4 + 1);
			float f = (arg.nextFloat() + arg.nextFloat() - 1.0F) * 0.15F;
			i = Mth.clamp(Math.round(i + i * f), 1, Integer.MAX_VALUE);
			List<EnchantmentInstance> list1 = getAvailableEnchantmentResults(i, arg2, stream);
			if (!list1.isEmpty()) {
				WeightedRandom.getRandomItem(arg, list1).ifPresent(list::add);

				while (arg.nextInt(50) <= i) {
					if (!list.isEmpty()) {
						filterCompatibleEnchantments(list1, Util.lastOf(list));
					}

					if (list1.isEmpty()) {
						break;
					}

					WeightedRandom.getRandomItem(arg, list1).ifPresent(list::add);
					i /= 2;
				}
			}

			return list;
		}
	}

	public static void filterCompatibleEnchantments(List<EnchantmentInstance> list, EnchantmentInstance arg) {
		list.removeIf(arg2 -> !Enchantment.areCompatible(arg.enchantment, arg2.enchantment));
	}

	public static boolean isEnchantmentCompatible(Collection<Holder<Enchantment>> collection, Holder<Enchantment> arg) {
		for (Holder<Enchantment> holder : collection) {
			if (!Enchantment.areCompatible(holder, arg)) {
				return false;
			}
		}

		return true;
	}

	public static List<EnchantmentInstance> getAvailableEnchantmentResults(int i, ItemStack arg, Stream<Holder<Enchantment>> stream) {
		List<EnchantmentInstance> list = Lists.<EnchantmentInstance>newArrayList();
		boolean flag = arg.is(Items.BOOK);
		stream.filter(arg::isPrimaryItemFor).forEach(argx -> {
			Enchantment enchantment = (Enchantment)argx.value();

			for (int ix = enchantment.getMaxLevel(); ix >= enchantment.getMinLevel(); ix--) {
				if (i >= enchantment.getMinCost(ix) && i <= enchantment.getMaxCost(ix)) {
					list.add(new EnchantmentInstance(argx, ix));
					break;
				}
			}
		});
		return list;
	}

	public static void enchantItemFromProvider(
		ItemStack arg, RegistryAccess arg2, ResourceKey<EnchantmentProvider> arg3, DifficultyInstance arg4, RandomSource arg5
	) {
		EnchantmentProvider enchantmentprovider = arg2.lookupOrThrow(Registries.ENCHANTMENT_PROVIDER).getValue(arg3);
		if (enchantmentprovider != null) {
			updateEnchantments(arg, arg4x -> enchantmentprovider.enchant(arg, arg4x, arg5, arg4));
		}
	}

	@FunctionalInterface
	public interface EnchantmentInSlotVisitor {
		void accept(Holder<Enchantment> arg, int i, EnchantedItemInUse arg2);
	}

	@FunctionalInterface
	public interface EnchantmentVisitor {
		void accept(Holder<Enchantment> arg, int i);
	}
}
