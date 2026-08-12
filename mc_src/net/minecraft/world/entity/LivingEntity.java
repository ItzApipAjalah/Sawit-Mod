package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.extensions.ILivingEntityExtension;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.slf4j.Logger;

public abstract class LivingEntity extends Entity implements Attackable, ILivingEntityExtension {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String TAG_ACTIVE_EFFECTS = "active_effects";
	private static final ResourceLocation SPEED_MODIFIER_POWDER_SNOW_ID = ResourceLocation.withDefaultNamespace("powder_snow");
	private static final ResourceLocation SPRINTING_MODIFIER_ID = ResourceLocation.withDefaultNamespace("sprinting");
	private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(
		SPRINTING_MODIFIER_ID, 0.3F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);
	public static final int HAND_SLOTS = 2;
	public static final int ARMOR_SLOTS = 4;
	public static final int EQUIPMENT_SLOT_OFFSET = 98;
	public static final int ARMOR_SLOT_OFFSET = 100;
	public static final int BODY_ARMOR_OFFSET = 105;
	public static final int SWING_DURATION = 6;
	public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
	private static final int DAMAGE_SOURCE_TIMEOUT = 40;
	public static final double MIN_MOVEMENT_DISTANCE = 0.003;
	public static final double DEFAULT_BASE_GRAVITY = 0.08;
	public static final int DEATH_DURATION = 20;
	private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
	private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
	public static final float BASE_JUMP_POWER = 0.42F;
	private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0;
	protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
	protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
	protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
	protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES = SynchedEntityData.defineId(
		LivingEntity.class, EntityDataSerializers.PARTICLES
	);
	private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(
		LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
	);
	private static final int PARTICLE_FREQUENCY_WHEN_INVISIBLE = 15;
	protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
	public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
	public static final float DEFAULT_BABY_SCALE = 0.5F;
	public static final String ATTRIBUTES_FIELD = "attributes";
	public static final BiPredicate<LivingEntity, LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM_FOR_TARGET = (arg, target) -> {
		if (arg instanceof Player player) {
			ItemStack itemstack = player.getItemBySlot(EquipmentSlot.HEAD);
			return !itemstack.isGazeDisguise(player, target);
		} else {
			return true;
		}
	};
	@Deprecated
	public static final Predicate<LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM = arg -> PLAYER_NOT_WEARING_DISGUISE_ITEM_FOR_TARGET.test(arg, null);
	private final AttributeMap attributes;
	private final CombatTracker combatTracker = new CombatTracker(this);
	private final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = Maps.<Holder<MobEffect>, MobEffectInstance>newHashMap();
	private final NonNullList<ItemStack> lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
	private final NonNullList<ItemStack> lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);
	private ItemStack lastBodyItemStack = ItemStack.EMPTY;
	public boolean swinging;
	private boolean discardFriction = false;
	public InteractionHand swingingArm;
	public int swingTime;
	public int removeArrowTime;
	public int removeStingerTime;
	public int hurtTime;
	public int hurtDuration;
	public int deathTime;
	public float oAttackAnim;
	public float attackAnim;
	protected int attackStrengthTicker;
	public final WalkAnimationState walkAnimation = new WalkAnimationState();
	public final int invulnerableDuration = 20;
	public final float timeOffs;
	public final float rotA;
	public float yBodyRot;
	public float yBodyRotO;
	public float yHeadRot;
	public float yHeadRotO;
	public final ElytraAnimationState elytraAnimationState = new ElytraAnimationState(this);
	@Nullable
	protected Player lastHurtByPlayer;
	protected int lastHurtByPlayerTime;
	protected boolean dead;
	protected int noActionTime;
	protected float oRun;
	protected float run;
	protected float animStep;
	protected float animStepO;
	protected float rotOffs;
	protected int deathScore;
	protected float lastHurt;
	protected boolean jumping;
	public float xxa;
	public float yya;
	public float zza;
	protected int lerpSteps;
	protected double lerpX;
	protected double lerpY;
	protected double lerpZ;
	protected double lerpYRot;
	protected double lerpXRot;
	protected double lerpYHeadRot;
	protected int lerpHeadSteps;
	private boolean effectsDirty = true;
	@Nullable
	private LivingEntity lastHurtByMob;
	private int lastHurtByMobTimestamp;
	@Nullable
	private LivingEntity lastHurtMob;
	private int lastHurtMobTimestamp;
	private float speed;
	private int noJumpDelay;
	private float absorptionAmount;
	protected ItemStack useItem = ItemStack.EMPTY;
	protected int useItemRemaining;
	protected int fallFlyTicks;
	private BlockPos lastPos;
	private Optional<BlockPos> lastClimbablePos = Optional.empty();
	@Nullable
	private DamageSource lastDamageSource;
	private long lastDamageStamp;
	protected int autoSpinAttackTicks;
	protected float autoSpinAttackDmg;
	@Nullable
	protected ItemStack autoSpinAttackItemStack;
	private float swimAmount;
	private float swimAmountO;
	protected Brain<?> brain;
	private boolean skipDropExperience;
	private final EnumMap<EquipmentSlot, Reference2ObjectMap<Enchantment, Set<EnchantmentLocationBasedEffect>>> activeLocationDependentEnchantments = new EnumMap(
		EquipmentSlot.class
	);
	protected float appliedScale = 1.0F;
	@Nullable
	protected Stack<DamageContainer> damageContainers = new Stack();

	protected LivingEntity(EntityType<? extends LivingEntity> arg, Level arg2) {
		super(arg, arg2);
		this.attributes = new AttributeMap(DefaultAttributes.getSupplier(arg));
		this.setHealth(this.getMaxHealth());
		this.blocksBuilding = true;
		this.rotA = (float)((Math.random() + 1.0) * 0.01F);
		this.reapplyPosition();
		this.timeOffs = (float)Math.random() * 12398.0F;
		this.setYRot((float)(Math.random() * (float) (Math.PI * 2)));
		this.yHeadRot = this.getYRot();
		NbtOps nbtops = NbtOps.INSTANCE;
		this.brain = this.makeBrain(new Dynamic<>(nbtops, nbtops.createMap(ImmutableMap.of(nbtops.createString("memories"), nbtops.emptyMap()))));
	}

	public Brain<?> getBrain() {
		return this.brain;
	}

	protected Brain.Provider<?> brainProvider() {
		return Brain.provider(ImmutableList.of(), ImmutableList.of());
	}

	protected Brain<?> makeBrain(Dynamic<?> dynamic) {
		return this.brainProvider().makeBrain(dynamic);
	}

	@Override
	public void kill(ServerLevel arg) {
		this.hurtServer(arg, this.damageSources().genericKill(), Float.MAX_VALUE);
	}

	public boolean canAttackType(EntityType<?> arg) {
		return true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder arg) {
		arg.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
		arg.define(DATA_EFFECT_PARTICLES, List.of());
		arg.define(DATA_EFFECT_AMBIENCE_ID, false);
		arg.define(DATA_ARROW_COUNT_ID, 0);
		arg.define(DATA_STINGER_COUNT_ID, 0);
		arg.define(DATA_HEALTH_ID, 1.0F);
		arg.define(SLEEPING_POS_ID, Optional.empty());
	}

	public static AttributeSupplier.Builder createLivingAttributes() {
		return AttributeSupplier.builder()
			.add(Attributes.MAX_HEALTH)
			.add(Attributes.KNOCKBACK_RESISTANCE)
			.add(Attributes.MOVEMENT_SPEED)
			.add(Attributes.ARMOR)
			.add(Attributes.ARMOR_TOUGHNESS)
			.add(Attributes.MAX_ABSORPTION)
			.add(Attributes.STEP_HEIGHT)
			.add(Attributes.SCALE)
			.add(Attributes.GRAVITY)
			.add(Attributes.SAFE_FALL_DISTANCE)
			.add(Attributes.FALL_DAMAGE_MULTIPLIER)
			.add(Attributes.JUMP_STRENGTH)
			.add(Attributes.OXYGEN_BONUS)
			.add(Attributes.BURNING_TIME)
			.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
			.add(Attributes.WATER_MOVEMENT_EFFICIENCY)
			.add(Attributes.MOVEMENT_EFFICIENCY)
			.add(Attributes.ATTACK_KNOCKBACK)
			.add(NeoForgeMod.SWIM_SPEED)
			.add(NeoForgeMod.NAMETAG_DISTANCE);
	}

	@Override
	protected void checkFallDamage(double d, boolean bl, BlockState arg, BlockPos arg2) {
		if (!this.isInWater()) {
			this.updateInWaterStateAndDoWaterCurrentPushing();
		}

		if (this.level() instanceof ServerLevel serverlevel && bl && this.fallDistance > 0.0F) {
			this.onChangedBlock(serverlevel, arg2);
			double d7 = this.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
			if (this.fallDistance > d7 && !arg.isAir()) {
				double d0 = this.getX();
				double d1 = this.getY();
				double d2 = this.getZ();
				BlockPos blockpos = this.blockPosition();
				if (arg2.getX() != blockpos.getX() || arg2.getZ() != blockpos.getZ()) {
					double d3 = d0 - arg2.getX() - 0.5;
					double d5 = d2 - arg2.getZ() - 0.5;
					double d6 = Math.max(Math.abs(d3), Math.abs(d5));
					d0 = arg2.getX() + 0.5 + d3 / d6 * 0.5;
					d2 = arg2.getZ() + 0.5 + d5 / d6 * 0.5;
				}

				float f = Mth.ceil(this.fallDistance - d7);
				double d4 = Math.min(0.2F + f / 15.0F, 2.5);
				int i = (int)(150.0 * d4);
				if (!arg.addLandingEffects((ServerLevel)this.level(), arg2, arg, this, i)) {
					((ServerLevel)this.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, arg).setPos(arg2), d0, d1, d2, i, 0.0, 0.0, 0.0, 0.15F);
				}
			}
		}

		super.checkFallDamage(d, bl, arg, arg2);
		if (bl) {
			this.lastClimbablePos = Optional.empty();
		}
	}

	@Deprecated
	public final boolean canBreatheUnderwater() {
		return this.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
	}

	public float getSwimAmount(float f) {
		return Mth.lerp(f, this.swimAmountO, this.swimAmount);
	}

	public boolean hasLandedInLiquid() {
		return this.getDeltaMovement().y() < 1.0E-5F && this.isInLiquid();
	}

	@Override
	public void baseTick() {
		this.oAttackAnim = this.attackAnim;
		if (this.firstTick) {
			this.getSleepingPos().ifPresent(this::setPosToBed);
		}

		if (this.level() instanceof ServerLevel serverlevel) {
			EnchantmentHelper.tickEffects(serverlevel, this);
		}

		super.baseTick();
		ProfilerFiller profilerfiller = Profiler.get();
		profilerfiller.push("livingEntityBaseTick");
		if (this.fireImmune() || this.level().isClientSide) {
			this.clearFire();
		}

		if (this.isAlive()) {
			boolean flag = this instanceof Player;
			if (this.level() instanceof ServerLevel serverlevel1) {
				if (this.isInWall()) {
					this.hurtServer(serverlevel1, this.damageSources().inWall(), 1.0F);
				} else if (flag && !this.level().getWorldBorder().isWithinBounds(this.getBoundingBox())) {
					double d3 = this.level().getWorldBorder().getDistanceToBorder(this) + this.level().getWorldBorder().getDamageSafeZone();
					if (d3 < 0.0) {
						double d0 = this.level().getWorldBorder().getDamagePerBlock();
						if (d0 > 0.0) {
							this.hurtServer(serverlevel1, this.damageSources().outOfBorder(), Math.max(1, Mth.floor(-d3 * d0)));
						}
					}
				}
			}

			int airSupply = this.getAirSupply();
			CommonHooks.onLivingBreathe(this, airSupply - this.decreaseAirSupply(airSupply), this.increaseAirSupply(airSupply) - airSupply);
			if (this.level() instanceof ServerLevel serverlevel2) {
				BlockPos blockpos = this.blockPosition();
				if (!Objects.equal(this.lastPos, blockpos)) {
					this.lastPos = blockpos;
					this.onChangedBlock(serverlevel2, blockpos);
				}
			}
		}

		if (this.isAlive() && (this.isInWaterRainOrBubble() || this.isInPowderSnow || this.isInFluidType((fluidType, height) -> this.canFluidExtinguish(fluidType)))) {
			this.extinguishFire();
		}

		if (this.hurtTime > 0) {
			this.hurtTime--;
		}

		if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
			this.invulnerableTime--;
		}

		if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
			this.tickDeath();
		}

		if (this.lastHurtByPlayerTime > 0) {
			this.lastHurtByPlayerTime--;
		} else {
			this.lastHurtByPlayer = null;
		}

		if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
			this.lastHurtMob = null;
		}

		if (this.lastHurtByMob != null) {
			if (!this.lastHurtByMob.isAlive()) {
				this.setLastHurtByMob(null);
			} else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
				this.setLastHurtByMob(null);
			}
		}

		this.tickEffects();
		this.animStepO = this.animStep;
		this.yBodyRotO = this.yBodyRot;
		this.yHeadRotO = this.yHeadRot;
		this.yRotO = this.getYRot();
		this.xRotO = this.getXRot();
		profilerfiller.pop();
	}

	@Override
	protected float getBlockSpeedFactor() {
		return Mth.lerp((float)this.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0F);
	}

	protected void removeFrost() {
		AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attributeinstance != null && attributeinstance.getModifier(SPEED_MODIFIER_POWDER_SNOW_ID) != null) {
			attributeinstance.removeModifier(SPEED_MODIFIER_POWDER_SNOW_ID);
		}
	}

	protected void tryAddFrost() {
		if (!this.getBlockStateOnLegacy().isAir()) {
			int i = this.getTicksFrozen();
			if (i > 0) {
				AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
				if (attributeinstance == null) {
					return;
				}

				float f = -0.05F * this.getPercentFrozen();
				attributeinstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_ID, f, AttributeModifier.Operation.ADD_VALUE));
			}
		}
	}

	protected void onChangedBlock(ServerLevel arg, BlockPos arg2) {
		EnchantmentHelper.runLocationChangedEffects(arg, this);
	}

	public boolean isBaby() {
		return false;
	}

	public float getAgeScale() {
		return this.isBaby() ? 0.5F : 1.0F;
	}

	public final float getScale() {
		AttributeMap attributemap = this.getAttributes();
		return attributemap == null ? 1.0F : this.sanitizeScale((float)attributemap.getValue(Attributes.SCALE));
	}

	protected float sanitizeScale(float f) {
		return f;
	}

	protected boolean isAffectedByFluids() {
		return true;
	}

	protected void tickDeath() {
		this.deathTime++;
		if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
			this.level().broadcastEntityEvent(this, (byte)60);
			this.remove(Entity.RemovalReason.KILLED);
		}
	}

	public boolean shouldDropExperience() {
		return !this.isBaby();
	}

	protected boolean shouldDropLoot() {
		return !this.isBaby();
	}

	protected int decreaseAirSupply(int i) {
		AttributeInstance attributeinstance = this.getAttribute(Attributes.OXYGEN_BONUS);
		double d0;
		if (attributeinstance != null) {
			d0 = attributeinstance.getValue();
		} else {
			d0 = 0.0;
		}

		return d0 > 0.0 && this.random.nextDouble() >= 1.0 / (d0 + 1.0) ? i : i - 1;
	}

	protected int increaseAirSupply(int i) {
		return Math.min(i + 4, this.getMaxAirSupply());
	}

	public final int getExperienceReward(ServerLevel arg, @Nullable Entity arg2) {
		return EnchantmentHelper.processMobExperience(arg, arg2, this, this.getBaseExperienceReward(arg));
	}

	protected int getBaseExperienceReward(ServerLevel arg) {
		return 0;
	}

	protected boolean isAlwaysExperienceDropper() {
		return false;
	}

	@Nullable
	public LivingEntity getLastHurtByMob() {
		return this.lastHurtByMob;
	}

	@Override
	public LivingEntity getLastAttacker() {
		return this.getLastHurtByMob();
	}

	public int getLastHurtByMobTimestamp() {
		return this.lastHurtByMobTimestamp;
	}

	public void setLastHurtByPlayer(@Nullable Player arg) {
		this.lastHurtByPlayer = arg;
		this.lastHurtByPlayerTime = this.tickCount;
	}

	public void setLastHurtByMob(@Nullable LivingEntity arg) {
		this.lastHurtByMob = arg;
		this.lastHurtByMobTimestamp = this.tickCount;
	}

	@Nullable
	public LivingEntity getLastHurtMob() {
		return this.lastHurtMob;
	}

	public int getLastHurtMobTimestamp() {
		return this.lastHurtMobTimestamp;
	}

	public void setLastHurtMob(Entity arg) {
		if (arg instanceof LivingEntity) {
			this.lastHurtMob = (LivingEntity)arg;
		} else {
			this.lastHurtMob = null;
		}

		this.lastHurtMobTimestamp = this.tickCount;
	}

	public int getNoActionTime() {
		return this.noActionTime;
	}

	public void setNoActionTime(int i) {
		this.noActionTime = i;
	}

	public boolean shouldDiscardFriction() {
		return this.discardFriction;
	}

	public void setDiscardFriction(boolean bl) {
		this.discardFriction = bl;
	}

	protected boolean doesEmitEquipEvent(EquipmentSlot arg) {
		return true;
	}

	public void onEquipItem(EquipmentSlot arg, ItemStack arg2, ItemStack arg3) {
		if (!this.level().isClientSide() && !this.isSpectator()) {
			boolean flag = arg3.isEmpty() && arg2.isEmpty();
			if (!flag && !ItemStack.isSameItemSameComponents(arg2, arg3) && !this.firstTick) {
				Equippable equippable = arg3.get(DataComponents.EQUIPPABLE);
				if (!this.isSilent() && equippable != null && arg == equippable.slot()) {
					this.level()
						.playSeededSound(null, this.getX(), this.getY(), this.getZ(), equippable.equipSound(), this.getSoundSource(), 1.0F, 1.0F, this.random.nextLong());
				}

				if (this.doesEmitEquipEvent(arg)) {
					this.gameEvent(equippable != null ? GameEvent.EQUIP : GameEvent.UNEQUIP);
				}
			}
		}
	}

	@Override
	public void remove(Entity.RemovalReason arg) {
		if ((arg == Entity.RemovalReason.KILLED || arg == Entity.RemovalReason.DISCARDED) && this.level() instanceof ServerLevel serverlevel) {
			this.triggerOnDeathMobEffects(serverlevel, arg);
		}

		super.remove(arg);
		this.brain.clearMemories();
	}

	protected void triggerOnDeathMobEffects(ServerLevel arg, Entity.RemovalReason arg2) {
		for (MobEffectInstance mobeffectinstance : this.getActiveEffects()) {
			mobeffectinstance.onMobRemoved(arg, this, arg2);
		}

		this.activeEffects.clear();
	}

	@Override
	public void addAdditionalSaveData(CompoundTag arg) {
		arg.putFloat("Health", this.getHealth());
		arg.putShort("HurtTime", (short)this.hurtTime);
		arg.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
		arg.putShort("DeathTime", (short)this.deathTime);
		arg.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
		arg.put("attributes", this.getAttributes().save());
		if (!this.activeEffects.isEmpty()) {
			ListTag listtag = new ListTag();

			for (MobEffectInstance mobeffectinstance : this.activeEffects.values()) {
				listtag.add(mobeffectinstance.save());
			}

			arg.put("active_effects", listtag);
		}

		arg.putBoolean("FallFlying", this.isFallFlying());
		this.getSleepingPos().ifPresent(arg2 -> {
			arg.putInt("SleepingX", arg2.getX());
			arg.putInt("SleepingY", arg2.getY());
			arg.putInt("SleepingZ", arg2.getZ());
		});
		DataResult<Tag> dataresult = this.brain.serializeStart(NbtOps.INSTANCE);
		dataresult.resultOrPartial(LOGGER::error).ifPresent(arg2 -> arg.put("Brain", arg2));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag arg) {
		this.internalSetAbsorptionAmount(arg.getFloat("AbsorptionAmount"));
		if (arg.contains("attributes", 9) && this.level() != null && !this.level().isClientSide) {
			this.getAttributes().load(arg.getList("attributes", 10));
		}

		if (arg.contains("active_effects", 9)) {
			ListTag listtag = arg.getList("active_effects", 10);

			for (int i = 0; i < listtag.size(); i++) {
				CompoundTag compoundtag = listtag.getCompound(i);
				MobEffectInstance mobeffectinstance = MobEffectInstance.load(compoundtag);
				if (mobeffectinstance != null) {
					this.activeEffects.put(mobeffectinstance.getEffect(), mobeffectinstance);
				}
			}
		}

		if (arg.contains("Health", 99)) {
			this.setHealth(arg.getFloat("Health"));
		}

		this.hurtTime = arg.getShort("HurtTime");
		this.deathTime = arg.getShort("DeathTime");
		this.lastHurtByMobTimestamp = arg.getInt("HurtByTimestamp");
		if (arg.contains("Team", 8)) {
			String s = arg.getString("Team");
			Scoreboard scoreboard = this.level().getScoreboard();
			PlayerTeam playerteam = scoreboard.getPlayerTeam(s);
			boolean flag = playerteam != null && scoreboard.addPlayerToTeam(this.getStringUUID(), playerteam);
			if (!flag) {
				LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", s);
			}
		}

		if (arg.getBoolean("FallFlying")) {
			this.setSharedFlag(7, true);
		}

		if (arg.contains("SleepingX", 99) && arg.contains("SleepingY", 99) && arg.contains("SleepingZ", 99)) {
			BlockPos blockpos = new BlockPos(arg.getInt("SleepingX"), arg.getInt("SleepingY"), arg.getInt("SleepingZ"));
			this.setSleepingPos(blockpos);
			this.entityData.set(DATA_POSE, Pose.SLEEPING);
			if (!this.firstTick) {
				this.setPosToBed(blockpos);
			}
		}

		if (arg.contains("Brain", 10)) {
			this.brain = this.makeBrain(new Dynamic<>(NbtOps.INSTANCE, arg.get("Brain")));
		}
	}

	protected void tickEffects() {
		Iterator<Holder<MobEffect>> iterator = this.activeEffects.keySet().iterator();

		try {
			while (iterator.hasNext()) {
				Holder<MobEffect> holder = (Holder<MobEffect>)iterator.next();
				MobEffectInstance mobeffectinstance = (MobEffectInstance)this.activeEffects.get(holder);
				if (!mobeffectinstance.tick(this, () -> this.onEffectUpdated(mobeffectinstance, true, null))) {
					if (!this.level().isClientSide && !NeoForge.EVENT_BUS.post(new MobEffectEvent.Expired(this, mobeffectinstance)).isCanceled()) {
						iterator.remove();
						this.onEffectsRemoved(List.of(mobeffectinstance));
					}
				} else if (mobeffectinstance.getDuration() % 600 == 0) {
					this.onEffectUpdated(mobeffectinstance, false, null);
				}
			}
		} catch (ConcurrentModificationException var6) {
		}

		if (this.effectsDirty) {
			if (!this.level().isClientSide) {
				this.updateInvisibilityStatus();
				this.updateGlowingStatus();
			}

			this.effectsDirty = false;
		}

		List<ParticleOptions> list = this.entityData.get(DATA_EFFECT_PARTICLES);
		if (!list.isEmpty()) {
			boolean flag = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
			int i = this.isInvisible() ? 15 : 4;
			int j = flag ? 5 : 1;
			if (this.random.nextInt(i * j) == 0) {
				this.level().addParticle(Util.getRandom(list, this.random), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 1.0, 1.0, 1.0);
			}
		}
	}

	protected void updateInvisibilityStatus() {
		if (this.activeEffects.isEmpty()) {
			this.removeEffectParticles();
			this.setInvisible(false);
		} else {
			this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
			this.updateSynchronizedMobEffectParticles();
		}
	}

	private void updateSynchronizedMobEffectParticles() {
		List<ParticleOptions> list = this.activeEffects
			.values()
			.stream()
			.map(effect -> NeoForge.EVENT_BUS.post(new EffectParticleModificationEvent(this, effect)))
			.filter(EffectParticleModificationEvent::isVisible)
			.map(EffectParticleModificationEvent::getParticleOptions)
			.toList();
		this.entityData.set(DATA_EFFECT_PARTICLES, list);
		this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(this.activeEffects.values()));
	}

	private void updateGlowingStatus() {
		boolean flag = this.isCurrentlyGlowing();
		if (this.getSharedFlag(6) != flag) {
			this.setSharedFlag(6, flag);
		}
	}

	public double getVisibilityPercent(@Nullable Entity arg) {
		double d0 = 1.0;
		if (this.isDiscrete()) {
			d0 *= 0.8;
		}

		if (this.isInvisible()) {
			float f = this.getArmorCoverPercentage();
			if (f < 0.1F) {
				f = 0.1F;
			}

			d0 *= 0.7 * f;
		}

		if (arg != null) {
			ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
			EntityType<?> entitytype = arg.getType();
			if (entitytype == EntityType.SKELETON && itemstack.is(Items.SKELETON_SKULL)
				|| entitytype == EntityType.ZOMBIE && itemstack.is(Items.ZOMBIE_HEAD)
				|| entitytype == EntityType.PIGLIN && itemstack.is(Items.PIGLIN_HEAD)
				|| entitytype == EntityType.PIGLIN_BRUTE && itemstack.is(Items.PIGLIN_HEAD)
				|| entitytype == EntityType.CREEPER && itemstack.is(Items.CREEPER_HEAD)) {
				d0 *= 0.5;
			}
		}

		return CommonHooks.getEntityVisibilityMultiplier(this, arg, d0);
	}

	public boolean canAttack(LivingEntity arg) {
		return arg instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL ? false : arg.canBeSeenAsEnemy();
	}

	public boolean canBeSeenAsEnemy() {
		return !this.isInvulnerable() && this.canBeSeenByAnyone();
	}

	public boolean canBeSeenByAnyone() {
		return !this.isSpectator() && this.isAlive();
	}

	public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> collection) {
		for (MobEffectInstance mobeffectinstance : collection) {
			if (mobeffectinstance.isVisible() && !mobeffectinstance.isAmbient()) {
				return false;
			}
		}

		return true;
	}

	protected void removeEffectParticles() {
		this.entityData.set(DATA_EFFECT_PARTICLES, List.of());
	}

	public boolean removeAllEffects() {
		if (this.level().isClientSide) {
			return false;
		} else if (this.activeEffects.isEmpty()) {
			return false;
		} else {
			Map<Holder<MobEffect>, MobEffectInstance> map = new HashMap(this.activeEffects.size());

			for (Entry<Holder<MobEffect>, MobEffectInstance> entry : this.activeEffects.entrySet()) {
				if (!EventHooks.onEffectRemoved(this, (MobEffectInstance)entry.getValue())) {
					map.put((Holder)entry.getKey(), (MobEffectInstance)entry.getValue());
				}
			}

			map.keySet().forEach(this.activeEffects::remove);
			this.onEffectsRemoved(map.values());
			return true;
		}
	}

	public Collection<MobEffectInstance> getActiveEffects() {
		return this.activeEffects.values();
	}

	public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
		return this.activeEffects;
	}

	public boolean hasEffect(Holder<MobEffect> arg) {
		return this.activeEffects.containsKey(arg);
	}

	@Nullable
	public MobEffectInstance getEffect(Holder<MobEffect> arg) {
		return (MobEffectInstance)this.activeEffects.get(arg);
	}

	public final boolean addEffect(MobEffectInstance arg) {
		return this.addEffect(arg, null);
	}

	public boolean addEffect(MobEffectInstance arg, @Nullable Entity arg2) {
		if (!CommonHooks.canMobEffectBeApplied(this, arg)) {
			return false;
		} else {
			MobEffectInstance mobeffectinstance = (MobEffectInstance)this.activeEffects.get(arg.getEffect());
			boolean flag = false;
			NeoForge.EVENT_BUS.post(new MobEffectEvent.Added(this, mobeffectinstance, arg, arg2));
			if (mobeffectinstance == null) {
				this.activeEffects.put(arg.getEffect(), arg);
				this.onEffectAdded(arg, arg2);
				flag = true;
				arg.onEffectAdded(this);
			} else if (mobeffectinstance.update(arg)) {
				this.onEffectUpdated(mobeffectinstance, true, arg2);
				flag = true;
			}

			arg.onEffectStarted(this);
			return flag;
		}
	}

	@Deprecated
	@OverrideOnly
	public boolean canBeAffected(MobEffectInstance arg) {
		if (this.getType().is(EntityTypeTags.IMMUNE_TO_INFESTED)) {
			return !arg.is(MobEffects.INFESTED);
		} else if (this.getType().is(EntityTypeTags.IMMUNE_TO_OOZING)) {
			return !arg.is(MobEffects.OOZING);
		} else {
			return !this.getType().is(EntityTypeTags.IGNORES_POISON_AND_REGEN) ? true : !arg.is(MobEffects.REGENERATION) && !arg.is(MobEffects.POISON);
		}
	}

	public void forceAddEffect(MobEffectInstance arg, @Nullable Entity arg2) {
		if (CommonHooks.canMobEffectBeApplied(this, arg)) {
			MobEffectInstance mobeffectinstance = (MobEffectInstance)this.activeEffects.put(arg.getEffect(), arg);
			if (mobeffectinstance == null) {
				this.onEffectAdded(arg, arg2);
			} else {
				arg.copyBlendState(mobeffectinstance);
				this.onEffectUpdated(arg, true, arg2);
			}
		}
	}

	public boolean isInvertedHealAndHarm() {
		return this.getType().is(EntityTypeTags.INVERTED_HEALING_AND_HARM);
	}

	@Nullable
	public MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> arg) {
		return (MobEffectInstance)this.activeEffects.remove(arg);
	}

	public boolean removeEffect(Holder<MobEffect> arg) {
		if (EventHooks.onEffectRemoved(this, arg)) {
			return false;
		} else {
			MobEffectInstance mobeffectinstance = this.removeEffectNoUpdate(arg);
			if (mobeffectinstance != null) {
				this.onEffectsRemoved(List.of(mobeffectinstance));
				return true;
			} else {
				return false;
			}
		}
	}

	protected void onEffectAdded(MobEffectInstance arg, @Nullable Entity arg2) {
		this.effectsDirty = true;
		if (!this.level().isClientSide) {
			arg.getEffect().value().addAttributeModifiers(this.getAttributes(), arg.getAmplifier());
			this.sendEffectToPassengers(arg);
		}
	}

	public void sendEffectToPassengers(MobEffectInstance arg) {
		for (Entity entity : this.getPassengers()) {
			if (entity instanceof ServerPlayer serverplayer) {
				serverplayer.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), arg, false));
			}
		}
	}

	protected void onEffectUpdated(MobEffectInstance arg, boolean bl, @Nullable Entity arg2) {
		this.effectsDirty = true;
		if (bl && !this.level().isClientSide) {
			MobEffect mobeffect = arg.getEffect().value();
			mobeffect.removeAttributeModifiers(this.getAttributes());
			mobeffect.addAttributeModifiers(this.getAttributes(), arg.getAmplifier());
			this.refreshDirtyAttributes();
		}

		if (!this.level().isClientSide) {
			this.sendEffectToPassengers(arg);
		}
	}

	protected void onEffectsRemoved(Collection<MobEffectInstance> collection) {
		this.effectsDirty = true;
		if (!this.level().isClientSide) {
			for (MobEffectInstance mobeffectinstance : collection) {
				mobeffectinstance.getEffect().value().removeAttributeModifiers(this.getAttributes());

				for (Entity entity : this.getPassengers()) {
					if (entity instanceof ServerPlayer serverplayer) {
						serverplayer.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), mobeffectinstance.getEffect()));
					}
				}
			}

			this.refreshDirtyAttributes();
		}
	}

	private void refreshDirtyAttributes() {
		Set<AttributeInstance> set = this.getAttributes().getAttributesToUpdate();

		for (AttributeInstance attributeinstance : set) {
			this.onAttributeUpdated(attributeinstance.getAttribute());
		}

		set.clear();
	}

	protected void onAttributeUpdated(Holder<Attribute> arg) {
		if (arg.is(Attributes.MAX_HEALTH)) {
			float f = this.getMaxHealth();
			if (this.getHealth() > f) {
				this.setHealth(f);
			}
		} else if (arg.is(Attributes.MAX_ABSORPTION)) {
			float f1 = this.getMaxAbsorption();
			if (this.getAbsorptionAmount() > f1) {
				this.setAbsorptionAmount(f1);
			}
		}
	}

	public void heal(float g) {
		g = EventHooks.onLivingHeal(this, g);
		if (!(g <= 0.0F)) {
			float f = this.getHealth();
			if (f > 0.0F) {
				this.setHealth(f + g);
			}
		}
	}

	public float getHealth() {
		return this.entityData.get(DATA_HEALTH_ID);
	}

	public void setHealth(float f) {
		this.entityData.set(DATA_HEALTH_ID, Mth.clamp(f, 0.0F, this.getMaxHealth()));
	}

	public boolean isDeadOrDying() {
		return this.getHealth() <= 0.0F;
	}

	@Override
	public boolean hurtServer(ServerLevel arg, DamageSource arg2, float g) {
		if (this.isInvulnerableTo(arg, arg2)) {
			return false;
		} else if (this.isDeadOrDying()) {
			return false;
		} else if (arg2.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
			return false;
		} else {
			this.damageContainers.push(new DamageContainer(arg2, g));
			if (CommonHooks.onEntityIncomingDamage(this, (DamageContainer)this.damageContainers.peek())) {
				return false;
			} else {
				if (this.isSleeping()) {
					this.stopSleeping();
				}

				this.noActionTime = 0;
				g = ((DamageContainer)this.damageContainers.peek()).getNewDamage();
				if (g < 0.0F) {
					g = 0.0F;
				}

				float f = g;
				boolean flag = false;
				float f1 = 0.0F;
				LivingShieldBlockEvent ev;
				if (g > 0.0F && (ev = CommonHooks.onDamageBlock(this, (DamageContainer)this.damageContainers.peek(), this.isDamageSourceBlocked(arg2))).getBlocked()) {
					((DamageContainer)this.damageContainers.peek()).setBlockedDamage(ev);
					if (ev.shieldDamage() > 0.0F) {
						this.hurtCurrentlyUsedShield(ev.shieldDamage());
					}

					f1 = ev.getBlockedDamage();
					g = ev.getDamageContainer().getNewDamage();
					if (!arg2.is(DamageTypeTags.IS_PROJECTILE) && arg2.getDirectEntity() instanceof LivingEntity livingentity) {
						this.blockUsingShield(livingentity);
					}

					flag = g <= 0.0F;
				}

				if (arg2.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
					g *= 5.0F;
				}

				if (arg2.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
					this.hurtHelmet(arg2, g);
					g *= 0.75F;
				}

				this.walkAnimation.setSpeed(1.5F);
				if (Float.isNaN(g) || Float.isInfinite(g)) {
					g = Float.MAX_VALUE;
				}

				((DamageContainer)this.damageContainers.peek()).setNewDamage(g);
				boolean flag1 = true;
				if (this.invulnerableTime > 10.0F && !arg2.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
					if (g <= this.lastHurt) {
						this.damageContainers.pop();
						return false;
					}

					((DamageContainer)this.damageContainers.peek()).setReduction(DamageContainer.Reduction.INVULNERABILITY, this.lastHurt);
					this.actuallyHurt(arg, arg2, g - this.lastHurt);
					this.lastHurt = g;
					flag1 = false;
				} else {
					this.lastHurt = g;
					this.invulnerableTime = ((DamageContainer)this.damageContainers.peek()).getPostAttackInvulnerabilityTicks();
					this.actuallyHurt(arg, arg2, g);
					this.hurtDuration = 10;
					this.hurtTime = this.hurtDuration;
				}

				g = ((DamageContainer)this.damageContainers.peek()).getNewDamage();
				Entity entity = arg2.getEntity();
				if (entity != null) {
					if (entity instanceof LivingEntity livingentity1
						&& !arg2.is(DamageTypeTags.NO_ANGER)
						&& (!arg2.is(DamageTypes.WIND_CHARGE) || !this.getType().is(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE))) {
						this.setLastHurtByMob(livingentity1);
					}

					if (entity instanceof Player player1) {
						this.lastHurtByPlayerTime = 100;
						this.lastHurtByPlayer = player1;
					} else if (entity instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
						this.lastHurtByPlayerTime = 100;
						if (tamableAnimal.getOwner() instanceof Player player) {
							this.lastHurtByPlayer = player;
						} else {
							this.lastHurtByPlayer = null;
						}
					}
				}

				if (flag1) {
					if (flag) {
						arg.broadcastEntityEvent(this, (byte)29);
					} else {
						arg.broadcastDamageEvent(this, arg2);
					}

					if (!arg2.is(DamageTypeTags.NO_IMPACT) && (!flag || g > 0.0F)) {
						this.markHurt();
					}

					if (!arg2.is(DamageTypeTags.NO_KNOCKBACK)) {
						double d0 = 0.0;
						double d1 = 0.0;
						if (arg2.getDirectEntity() instanceof Projectile projectile) {
							DoubleDoubleImmutablePair doubledoubleimmutablepair = projectile.calculateHorizontalHurtKnockbackDirection(this, arg2);
							d0 = -doubledoubleimmutablepair.leftDouble();
							d1 = -doubledoubleimmutablepair.rightDouble();
						} else if (arg2.getSourcePosition() != null) {
							d0 = arg2.getSourcePosition().x() - this.getX();
							d1 = arg2.getSourcePosition().z() - this.getZ();
						}

						this.knockback(0.4F, d0, d1);
						if (!flag) {
							this.indicateDamage(d0, d1);
						}
					}
				}

				if (this.isDeadOrDying()) {
					if (!this.checkTotemDeathProtection(arg2)) {
						if (flag1) {
							this.makeSound(this.getDeathSound());
						}

						this.die(arg2);
					}
				} else if (flag1) {
					this.playHurtSound(arg2);
				}

				boolean flag2 = !flag || g > 0.0F;
				if (flag2) {
					this.lastDamageSource = arg2;
					this.lastDamageStamp = this.level().getGameTime();

					for (MobEffectInstance mobeffectinstance : this.getActiveEffects()) {
						mobeffectinstance.onMobHurt(arg, this, arg2, g);
					}
				}

				if (this instanceof ServerPlayer serverplayer) {
					CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(serverplayer, arg2, f, g, flag);
					if (f1 > 0.0F && f1 < 3.4028235E37F) {
						serverplayer.awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(f1 * 10.0F));
					}
				}

				if (entity instanceof ServerPlayer serverplayer1) {
					CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(serverplayer1, this, arg2, f, g, flag);
				}

				this.damageContainers.pop();
				return flag2;
			}
		}
	}

	protected void blockUsingShield(LivingEntity arg) {
		arg.blockedByShield(this);
	}

	protected void blockedByShield(LivingEntity arg) {
		arg.knockback(0.5, arg.getX() - this.getX(), arg.getZ() - this.getZ());
	}

	private boolean checkTotemDeathProtection(DamageSource arg) {
		if (arg.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		} else {
			ItemStack itemstack = null;
			DeathProtection deathprotection = null;

			for (InteractionHand interactionhand : InteractionHand.values()) {
				ItemStack itemstack1 = this.getItemInHand(interactionhand);
				deathprotection = itemstack1.get(DataComponents.DEATH_PROTECTION);
				if (deathprotection != null && CommonHooks.onLivingUseTotem(this, arg, itemstack1, interactionhand)) {
					itemstack = itemstack1.copy();
					itemstack1.shrink(1);
					break;
				}
			}

			if (itemstack != null) {
				if (this instanceof ServerPlayer serverplayer) {
					serverplayer.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));
					CriteriaTriggers.USED_TOTEM.trigger(serverplayer, itemstack);
					this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
				}

				this.setHealth(1.0F);
				deathprotection.applyEffects(itemstack, this);
				this.level().broadcastEntityEvent(this, (byte)35);
			}

			return deathprotection != null;
		}
	}

	@Nullable
	public DamageSource getLastDamageSource() {
		if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
			this.lastDamageSource = null;
		}

		return this.lastDamageSource;
	}

	protected void playHurtSound(DamageSource arg) {
		this.makeSound(this.getHurtSound(arg));
	}

	public void makeSound(@Nullable SoundEvent arg) {
		if (arg != null) {
			this.playSound(arg, this.getSoundVolume(), this.getVoicePitch());
		}
	}

	public boolean isDamageSourceBlocked(DamageSource arg) {
		Entity entity = arg.getDirectEntity();
		boolean flag = false;
		if (entity instanceof AbstractArrow abstractarrow && abstractarrow.getPierceLevel() > 0) {
			flag = true;
		}

		ItemStack itemstack = this.getItemBlockingWith();
		if (!arg.is(DamageTypeTags.BYPASSES_SHIELD) && itemstack != null && !flag) {
			Vec3 vec3 = arg.getSourcePosition();
			if (vec3 != null) {
				Vec3 vec31 = this.calculateViewVector(0.0F, this.getYHeadRot());
				Vec3 vec32 = vec3.vectorTo(this.position());
				vec32 = new Vec3(vec32.x, 0.0, vec32.z).normalize();
				return vec32.dot(vec31) < 0.0;
			}
		}

		return false;
	}

	private void breakItem(ItemStack arg) {
		if (!arg.isEmpty()) {
			if (!this.isSilent()) {
				this.level()
					.playLocalSound(
						this.getX(), this.getY(), this.getZ(), arg.getBreakingSound(), this.getSoundSource(), 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F, false
					);
			}

			this.spawnItemParticles(arg, 5);
		}
	}

	public void die(DamageSource arg) {
		if (!CommonHooks.onLivingDeath(this, arg)) {
			if (!this.isRemoved() && !this.dead) {
				Entity entity = arg.getEntity();
				LivingEntity livingentity = this.getKillCredit();
				if (this.deathScore >= 0 && livingentity != null) {
					livingentity.awardKillScore(this, this.deathScore, arg);
				}

				if (this.isSleeping()) {
					this.stopSleeping();
				}

				if (!this.level().isClientSide && this.hasCustomName()) {
					LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
				}

				this.dead = true;
				this.getCombatTracker().recheckStatus();
				if (this.level() instanceof ServerLevel serverlevel) {
					if (entity == null || entity.killedEntity(serverlevel, this)) {
						this.gameEvent(GameEvent.ENTITY_DIE);
						this.dropAllDeathLoot(serverlevel, arg);
						this.createWitherRose(livingentity);
					}

					this.level().broadcastEntityEvent(this, (byte)3);
				}

				this.setPose(Pose.DYING);
			}
		}
	}

	protected void createWitherRose(@Nullable LivingEntity arg) {
		if (this.level() instanceof ServerLevel serverlevel) {
			boolean flag = false;
			if (arg instanceof WitherBoss) {
				if (EventHooks.canEntityGrief(serverlevel, arg)) {
					BlockPos blockpos = this.blockPosition();
					BlockState blockstate = Blocks.WITHER_ROSE.defaultBlockState();
					if (this.level().getBlockState(blockpos).isAir() && blockstate.canSurvive(this.level(), blockpos)) {
						this.level().setBlock(blockpos, blockstate, 3);
						flag = true;
					}
				}

				if (!flag) {
					ItemEntity itementity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
					this.level().addFreshEntity(itementity);
				}
			}
		}
	}

	protected void dropAllDeathLoot(ServerLevel arg, DamageSource arg2) {
		this.captureDrops(new ArrayList());
		boolean flag = this.lastHurtByPlayerTime > 0;
		if (this.shouldDropLoot() && arg.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
			this.dropFromLootTable(arg, arg2, flag);
			this.dropCustomDeathLoot(arg, arg2, flag);
		}

		this.dropEquipment(arg);
		this.dropExperience(arg, arg2.getEntity());
		Collection<ItemEntity> drops = this.captureDrops(null);
		if (!CommonHooks.onLivingDrops(this, arg2, drops, this.lastHurtByPlayerTime > 0)) {
			drops.forEach(e -> this.level().addFreshEntity(e));
		}
	}

	protected void dropEquipment(ServerLevel arg) {
	}

	protected void dropExperience(ServerLevel arg, @Nullable Entity arg2) {
		if (!this.wasExperienceConsumed()
			&& (
				this.isAlwaysExperienceDropper() || this.lastHurtByPlayerTime > 0 && this.shouldDropExperience() && arg.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
			)) {
			int reward = EventHooks.getExperienceDrop(this, this.lastHurtByPlayer, this.getExperienceReward(arg, arg2));
			ExperienceOrb.award((ServerLevel)this.level(), this.position(), reward);
		}
	}

	protected void dropCustomDeathLoot(ServerLevel arg, DamageSource arg2, boolean bl) {
	}

	public long getLootTableSeed() {
		return 0L;
	}

	protected float getKnockback(Entity arg, DamageSource arg2) {
		float f = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
		return this.level() instanceof ServerLevel serverlevel ? EnchantmentHelper.modifyKnockback(serverlevel, this.getWeaponItem(), arg, arg2, f) : f;
	}

	protected void dropFromLootTable(ServerLevel arg, DamageSource arg2, boolean bl) {
		Optional<ResourceKey<LootTable>> optional = this.getLootTable();
		if (!optional.isEmpty()) {
			LootTable loottable = arg.getServer().reloadableRegistries().getLootTable((ResourceKey<LootTable>)optional.get());
			LootParams.Builder lootparams$builder = new LootParams.Builder(arg)
				.withParameter(LootContextParams.THIS_ENTITY, this)
				.withParameter(LootContextParams.ORIGIN, this.position())
				.withParameter(LootContextParams.DAMAGE_SOURCE, arg2)
				.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, arg2.getEntity())
				.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, arg2.getDirectEntity());
			if (bl && this.lastHurtByPlayer != null) {
				lootparams$builder = lootparams$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, this.lastHurtByPlayer)
					.withLuck(this.lastHurtByPlayer.getLuck());
			}

			LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
			loottable.getRandomItems(lootparams, this.getLootTableSeed(), arg2x -> this.spawnAtLocation(arg, arg2x));
		}
	}

	public boolean dropFromGiftLootTable(ServerLevel arg, ResourceKey<LootTable> arg2, BiConsumer<ServerLevel, ItemStack> biConsumer) {
		return this.dropFromLootTable(
			arg,
			arg2,
			argx -> argx.withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.THIS_ENTITY, this).create(LootContextParamSets.GIFT),
			biConsumer
		);
	}

	protected void dropFromShearingLootTable(ServerLevel arg, ResourceKey<LootTable> arg2, ItemStack arg3, BiConsumer<ServerLevel, ItemStack> biConsumer) {
		this.dropFromLootTable(
			arg,
			arg2,
			arg2x -> arg2x.withParameter(LootContextParams.ORIGIN, this.position())
				.withParameter(LootContextParams.THIS_ENTITY, this)
				.withParameter(LootContextParams.TOOL, arg3)
				.create(LootContextParamSets.SHEARING),
			biConsumer
		);
	}

	protected boolean dropFromLootTable(
		ServerLevel arg, ResourceKey<LootTable> arg2, Function<LootParams.Builder, LootParams> function, BiConsumer<ServerLevel, ItemStack> biConsumer
	) {
		LootTable loottable = arg.getServer().reloadableRegistries().getLootTable(arg2);
		LootParams lootparams = (LootParams)function.apply(new LootParams.Builder(arg));
		List<ItemStack> list = loottable.getRandomItems(lootparams);
		if (!list.isEmpty()) {
			list.forEach(arg2x -> biConsumer.accept(arg, arg2x));
			return true;
		} else {
			return false;
		}
	}

	public void knockback(double d, double e, double f) {
		LivingKnockBackEvent event = CommonHooks.onLivingKnockBack(this, (float)d, e, f);
		if (!event.isCanceled()) {
			d = event.getStrength();
			e = event.getRatioX();
			f = event.getRatioZ();
			d *= 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
			if (!(d <= 0.0)) {
				this.hasImpulse = true;

				Vec3 vec3;
				for (vec3 = this.getDeltaMovement(); e * e + f * f < 1.0E-5F; f = (Math.random() - Math.random()) * 0.01) {
					e = (Math.random() - Math.random()) * 0.01;
				}

				Vec3 vec31 = new Vec3(e, 0.0, f).normalize().scale(d);
				this.setDeltaMovement(vec3.x / 2.0 - vec31.x, this.onGround() ? Math.min(0.4, vec3.y / 2.0 + d) : vec3.y, vec3.z / 2.0 - vec31.z);
			}
		}
	}

	public void indicateDamage(double d, double e) {
	}

	@Nullable
	protected SoundEvent getHurtSound(DamageSource arg) {
		return SoundEvents.GENERIC_HURT;
	}

	@Nullable
	protected SoundEvent getDeathSound() {
		return SoundEvents.GENERIC_DEATH;
	}

	private SoundEvent getFallDamageSound(int i) {
		return i > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
	}

	public void skipDropExperience() {
		this.skipDropExperience = true;
	}

	public boolean wasExperienceConsumed() {
		return this.skipDropExperience;
	}

	public float getHurtDir() {
		return 0.0F;
	}

	public AABB getHitbox() {
		AABB aabb = this.getBoundingBox();
		Entity entity = this.getVehicle();
		if (entity != null) {
			Vec3 vec3 = entity.getPassengerRidingPosition(this);
			return aabb.setMinY(Math.max(vec3.y, aabb.minY));
		} else {
			return aabb;
		}
	}

	public Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantments(EquipmentSlot arg) {
		return (Map<Enchantment, Set<EnchantmentLocationBasedEffect>>)this.activeLocationDependentEnchantments
			.computeIfAbsent(arg, argx -> new Reference2ObjectArrayMap());
	}

	public LivingEntity.Fallsounds getFallSounds() {
		return new LivingEntity.Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
	}

	public Optional<BlockPos> getLastClimbablePos() {
		return this.lastClimbablePos;
	}

	public boolean onClimbable() {
		if (this.isSpectator()) {
			return false;
		} else {
			BlockPos blockpos = this.blockPosition();
			BlockState blockstate = this.getInBlockState();
			Optional<BlockPos> ladderPos = CommonHooks.isLivingOnLadder(blockstate, this.level(), blockpos, this);
			if (ladderPos.isPresent()) {
				this.lastClimbablePos = ladderPos;
			}

			return ladderPos.isPresent();
		}
	}

	private boolean trapdoorUsableAsLadder(BlockPos arg, BlockState arg2) {
		if (!(Boolean)arg2.getValue(TrapDoorBlock.OPEN)) {
			return false;
		} else {
			BlockState blockstate = this.level().getBlockState(arg.below());
			return blockstate.is(Blocks.LADDER) && blockstate.getValue(LadderBlock.FACING) == arg2.getValue(TrapDoorBlock.FACING);
		}
	}

	@Override
	public boolean isAlive() {
		return !this.isRemoved() && this.getHealth() > 0.0F;
	}

	@Deprecated
	public boolean isLookingAtMe(LivingEntity arg, double d, boolean bl, boolean bl2, Predicate<LivingEntity> predicate, DoubleSupplier... doubleSuppliers) {
		return this.isLookingAtMe(arg, d, bl, bl2, (BiPredicate<LivingEntity, LivingEntity>)((observer, target) -> predicate.test(observer)), doubleSuppliers);
	}

	public boolean isLookingAtMe(
		LivingEntity arg, double d, boolean bl, boolean bl2, BiPredicate<LivingEntity, LivingEntity> biPredicate, DoubleSupplier... doubleSuppliers
	) {
		if (!biPredicate.test(arg, this)) {
			return false;
		} else {
			Vec3 vec3 = arg.getViewVector(1.0F).normalize();

			for (DoubleSupplier doublesupplier : doubleSuppliers) {
				Vec3 vec31 = new Vec3(this.getX() - arg.getX(), doublesupplier.getAsDouble() - arg.getEyeY(), this.getZ() - arg.getZ());
				double d0 = vec31.length();
				vec31 = vec31.normalize();
				double d1 = vec3.dot(vec31);
				if (d1 > 1.0 - d / (bl ? d0 : 1.0)) {
					return arg.hasLineOfSight(this, bl2 ? ClipContext.Block.VISUAL : ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, doublesupplier);
				}
			}

			return false;
		}
	}

	@Override
	public int getMaxFallDistance() {
		return this.getComfortableFallDistance(0.0F);
	}

	protected final int getComfortableFallDistance(float f) {
		return Mth.floor(f + 3.0F);
	}

	@Override
	public boolean causeFallDamage(float f, float g, DamageSource arg) {
		float[] ret = CommonHooks.onLivingFall(this, f, g);
		if (ret == null) {
			return false;
		} else {
			f = ret[0];
			g = ret[1];
			boolean flag = super.causeFallDamage(f, g, arg);
			int i = this.calculateFallDamage(f, g);
			if (i > 0) {
				this.playSound(this.getFallDamageSound(i), 1.0F, 1.0F);
				this.playBlockFallSound();
				this.hurt(arg, i);
				return true;
			} else {
				return flag;
			}
		}
	}

	protected int calculateFallDamage(float g, float h) {
		if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
			return 0;
		} else {
			float f = (float)this.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
			float f1 = g - f;
			return Mth.ceil(f1 * h * this.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
		}
	}

	protected void playBlockFallSound() {
		if (!this.isSilent()) {
			int i = Mth.floor(this.getX());
			int j = Mth.floor(this.getY() - 0.2F);
			int k = Mth.floor(this.getZ());
			BlockPos pos = new BlockPos(i, j, k);
			BlockState blockstate = this.level().getBlockState(pos);
			if (!blockstate.isAir()) {
				SoundType soundtype = blockstate.getSoundType(this.level(), pos, this);
				this.playSound(soundtype.getFallSound(), soundtype.getVolume() * 0.5F, soundtype.getPitch() * 0.75F);
			}
		}
	}

	@Override
	public void animateHurt(float f) {
		this.hurtDuration = 10;
		this.hurtTime = this.hurtDuration;
	}

	public int getArmorValue() {
		return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
	}

	protected void hurtArmor(DamageSource arg, float f) {
	}

	protected void hurtHelmet(DamageSource arg, float f) {
	}

	protected void hurtCurrentlyUsedShield(float f) {
	}

	protected void doHurtEquipment(DamageSource arg, float f, EquipmentSlot... args) {
		if (!(f <= 0.0F)) {
			int i = (int)Math.max(1.0F, f / 4.0F);
			CommonHooks.onArmorHurt(arg, args, i, this);
		}
	}

	protected float getDamageAfterArmorAbsorb(DamageSource arg, float f) {
		if (!arg.is(DamageTypeTags.BYPASSES_ARMOR)) {
			this.hurtArmor(arg, f);
			f = CombatRules.getDamageAfterAbsorb(this, f, arg, this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		}

		return f;
	}

	protected float getDamageAfterMagicAbsorb(DamageSource arg, float g) {
		if (arg.is(DamageTypeTags.BYPASSES_EFFECTS)) {
			return g;
		} else {
			if (this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !arg.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
				int i = (this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
				int j = 25 - i;
				float f = g * j;
				float f1 = g;
				g = Math.max(f / 25.0F, 0.0F);
				float f2 = f1 - g;
				if (f2 > 0.0F && f2 < 3.4028235E37F) {
					((DamageContainer)this.damageContainers.peek()).setReduction(DamageContainer.Reduction.MOB_EFFECTS, f2);
					if (this instanceof ServerPlayer) {
						((ServerPlayer)this).awardStat(Stats.DAMAGE_RESISTED, Math.round(f2 * 10.0F));
					} else if (arg.getEntity() instanceof ServerPlayer) {
						((ServerPlayer)arg.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(f2 * 10.0F));
					}
				}
			}

			if (g <= 0.0F) {
				return 0.0F;
			} else if (arg.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
				return g;
			} else {
				float f3;
				if (this.level() instanceof ServerLevel serverlevel) {
					f3 = EnchantmentHelper.getDamageProtection(serverlevel, this, arg);
				} else {
					f3 = 0.0F;
				}

				if (f3 > 0.0F) {
					g = CombatRules.getDamageAfterMagicAbsorb(g, f3);
					((DamageContainer)this.damageContainers.peek())
						.setReduction(DamageContainer.Reduction.ENCHANTMENTS, ((DamageContainer)this.damageContainers.peek()).getNewDamage() - g);
				}

				return g;
			}
		}
	}

	protected void actuallyHurt(ServerLevel arg, DamageSource arg2, float g) {
		if (!this.isInvulnerableTo(arg, arg2)) {
			((DamageContainer)this.damageContainers.peek())
				.setReduction(
					DamageContainer.Reduction.ARMOR,
					((DamageContainer)this.damageContainers.peek()).getNewDamage()
						- this.getDamageAfterArmorAbsorb(arg2, ((DamageContainer)this.damageContainers.peek()).getNewDamage())
				);
			this.getDamageAfterMagicAbsorb(arg2, ((DamageContainer)this.damageContainers.peek()).getNewDamage());
			float damage = CommonHooks.onLivingDamagePre(this, (DamageContainer)this.damageContainers.peek());
			((DamageContainer)this.damageContainers.peek()).setReduction(DamageContainer.Reduction.ABSORPTION, Math.min(this.getAbsorptionAmount(), damage));
			float absorbed = Math.min(damage, ((DamageContainer)this.damageContainers.peek()).getReduction(DamageContainer.Reduction.ABSORPTION));
			this.setAbsorptionAmount(Math.max(0.0F, this.getAbsorptionAmount() - absorbed));
			float f1 = ((DamageContainer)this.damageContainers.peek()).getNewDamage();
			if (absorbed > 0.0F && absorbed < 3.4028235E37F && arg2.getEntity() instanceof ServerPlayer serverplayer) {
				serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(absorbed * 10.0F));
			}

			if (f1 != 0.0F) {
				this.getCombatTracker().recordDamage(arg2, f1);
				this.setHealth(this.getHealth() - f1);
				this.gameEvent(GameEvent.ENTITY_DAMAGE);
				this.onDamageTaken((DamageContainer)this.damageContainers.peek());
			}

			CommonHooks.onLivingDamagePost(this, (DamageContainer)this.damageContainers.peek());
		}
	}

	public CombatTracker getCombatTracker() {
		return this.combatTracker;
	}

	@Nullable
	public LivingEntity getKillCredit() {
		if (this.lastHurtByPlayer != null) {
			return this.lastHurtByPlayer;
		} else {
			return this.lastHurtByMob != null ? this.lastHurtByMob : null;
		}
	}

	public final float getMaxHealth() {
		return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
	}

	public final float getMaxAbsorption() {
		return (float)this.getAttributeValue(Attributes.MAX_ABSORPTION);
	}

	public final int getArrowCount() {
		return this.entityData.get(DATA_ARROW_COUNT_ID);
	}

	public final void setArrowCount(int i) {
		this.entityData.set(DATA_ARROW_COUNT_ID, i);
	}

	public final int getStingerCount() {
		return this.entityData.get(DATA_STINGER_COUNT_ID);
	}

	public final void setStingerCount(int i) {
		this.entityData.set(DATA_STINGER_COUNT_ID, i);
	}

	public int getCurrentSwingDuration() {
		if (MobEffectUtil.hasDigSpeed(this)) {
			return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
		} else {
			return this.hasEffect(MobEffects.DIG_SLOWDOWN) ? 6 + (1 + this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
		}
	}

	public void swing(InteractionHand arg) {
		this.swing(arg, false);
	}

	public void swing(InteractionHand arg, boolean bl) {
		ItemStack stack = this.getItemInHand(arg);
		if (stack.isEmpty() || !stack.onEntitySwing(this, arg)) {
			if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
				this.swingTime = -1;
				this.swinging = true;
				this.swingingArm = arg;
				if (this.level() instanceof ServerLevel) {
					ClientboundAnimatePacket clientboundanimatepacket = new ClientboundAnimatePacket(this, arg == InteractionHand.MAIN_HAND ? 0 : 3);
					ServerChunkCache serverchunkcache = ((ServerLevel)this.level()).getChunkSource();
					if (bl) {
						serverchunkcache.broadcastAndSend(this, clientboundanimatepacket);
					} else {
						serverchunkcache.broadcast(this, clientboundanimatepacket);
					}
				}
			}
		}
	}

	@Override
	public void handleDamageEvent(DamageSource arg) {
		this.walkAnimation.setSpeed(1.5F);
		this.invulnerableTime = 20;
		this.hurtDuration = 10;
		this.hurtTime = this.hurtDuration;
		SoundEvent soundevent = this.getHurtSound(arg);
		if (soundevent != null) {
			this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
		}

		this.lastDamageSource = arg;
		this.lastDamageStamp = this.level().getGameTime();
	}

	@Override
	public void handleEntityEvent(byte b) {
		switch (b) {
			case 3:
				SoundEvent soundevent = this.getDeathSound();
				if (soundevent != null) {
					this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
				}

				if (!(this instanceof Player)) {
					this.setHealth(0.0F);
					this.die(this.damageSources().generic());
				}
				break;
			case 29:
				this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + this.level().random.nextFloat() * 0.4F);
				break;
			case 30:
				this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F);
				break;
			case 46:
				int i = 128;

				for (int j = 0; j < 128; j++) {
					double d0 = j / 127.0;
					float f = (this.random.nextFloat() - 0.5F) * 0.2F;
					float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
					float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
					double d1 = Mth.lerp(d0, this.xo, this.getX()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
					double d2 = Mth.lerp(d0, this.yo, this.getY()) + this.random.nextDouble() * this.getBbHeight();
					double d3 = Mth.lerp(d0, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
					this.level().addParticle(ParticleTypes.PORTAL, d1, d2, d3, f, f1, f2);
				}
				break;
			case 47:
				this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
				break;
			case 48:
				this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
				break;
			case 49:
				this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
				break;
			case 50:
				this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
				break;
			case 51:
				this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
				break;
			case 52:
				this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
				break;
			case 54:
				HoneyBlock.showJumpParticles(this);
				break;
			case 55:
				this.swapHandItems();
				break;
			case 60:
				this.makePoofParticles();
				break;
			case 65:
				this.breakItem(this.getItemBySlot(EquipmentSlot.BODY));
				break;
			default:
				super.handleEntityEvent(b);
		}
	}

	public void makePoofParticles() {
		for (int i = 0; i < 20; i++) {
			double d0 = this.random.nextGaussian() * 0.02;
			double d1 = this.random.nextGaussian() * 0.02;
			double d2 = this.random.nextGaussian() * 0.02;
			double d3 = 10.0;
			this.level().addParticle(ParticleTypes.POOF, this.getRandomX(1.0) - d0 * 10.0, this.getRandomY() - d1 * 10.0, this.getRandomZ(1.0) - d2 * 10.0, d0, d1, d2);
		}
	}

	private void swapHandItems() {
		ItemStack itemstack = this.getItemBySlot(EquipmentSlot.OFFHAND);
		LivingSwapItemsEvent.Hands event = CommonHooks.onLivingSwapHandItems(this);
		if (!event.isCanceled()) {
			this.setItemSlot(EquipmentSlot.OFFHAND, event.getItemSwappedToOffHand());
			this.setItemSlot(EquipmentSlot.MAINHAND, event.getItemSwappedToMainHand());
		}
	}

	@Override
	protected void onBelowWorld() {
		this.hurt(this.damageSources().fellOutOfWorld(), 4.0F);
	}

	protected void updateSwingTime() {
		int i = this.getCurrentSwingDuration();
		if (this.swinging) {
			this.swingTime++;
			if (this.swingTime >= i) {
				this.swingTime = 0;
				this.swinging = false;
			}
		} else {
			this.swingTime = 0;
		}

		this.attackAnim = (float)this.swingTime / i;
	}

	@Nullable
	public AttributeInstance getAttribute(Holder<Attribute> arg) {
		return this.getAttributes().getInstance(arg);
	}

	public double getAttributeValue(Holder<Attribute> arg) {
		return this.getAttributes().getValue(arg);
	}

	public double getAttributeBaseValue(Holder<Attribute> arg) {
		return this.getAttributes().getBaseValue(arg);
	}

	public AttributeMap getAttributes() {
		return this.attributes;
	}

	public ItemStack getMainHandItem() {
		return this.getItemBySlot(EquipmentSlot.MAINHAND);
	}

	public ItemStack getOffhandItem() {
		return this.getItemBySlot(EquipmentSlot.OFFHAND);
	}

	public ItemStack getItemHeldByArm(HumanoidArm arg) {
		return this.getMainArm() == arg ? this.getMainHandItem() : this.getOffhandItem();
	}

	@Nonnull
	@Override
	public ItemStack getWeaponItem() {
		return this.getMainHandItem();
	}

	public boolean isHolding(Item arg) {
		return this.isHolding(arg2 -> arg2.is(arg));
	}

	public boolean isHolding(Predicate<ItemStack> predicate) {
		return predicate.test(this.getMainHandItem()) || predicate.test(this.getOffhandItem());
	}

	public ItemStack getItemInHand(InteractionHand arg) {
		if (arg == InteractionHand.MAIN_HAND) {
			return this.getItemBySlot(EquipmentSlot.MAINHAND);
		} else if (arg == InteractionHand.OFF_HAND) {
			return this.getItemBySlot(EquipmentSlot.OFFHAND);
		} else {
			throw new IllegalArgumentException("Invalid hand " + arg);
		}
	}

	public void setItemInHand(InteractionHand arg, ItemStack arg2) {
		if (arg == InteractionHand.MAIN_HAND) {
			this.setItemSlot(EquipmentSlot.MAINHAND, arg2);
		} else {
			if (arg != InteractionHand.OFF_HAND) {
				throw new IllegalArgumentException("Invalid hand " + arg);
			}

			this.setItemSlot(EquipmentSlot.OFFHAND, arg2);
		}
	}

	public boolean hasItemInSlot(EquipmentSlot arg) {
		return !this.getItemBySlot(arg).isEmpty();
	}

	public boolean canUseSlot(EquipmentSlot arg) {
		return false;
	}

	public abstract Iterable<ItemStack> getArmorSlots();

	public abstract ItemStack getItemBySlot(EquipmentSlot arg);

	public abstract void setItemSlot(EquipmentSlot arg, ItemStack arg2);

	public Iterable<ItemStack> getHandSlots() {
		return List.of();
	}

	public Iterable<ItemStack> getArmorAndBodyArmorSlots() {
		return this.getArmorSlots();
	}

	public Iterable<ItemStack> getAllSlots() {
		return Iterables.concat(this.getHandSlots(), this.getArmorAndBodyArmorSlots());
	}

	protected void verifyEquippedItem(ItemStack arg) {
		arg.getItem().verifyComponentsAfterLoad(arg);
	}

	public float getArmorCoverPercentage() {
		Iterable<ItemStack> iterable = this.getArmorSlots();
		int i = 0;
		int j = 0;

		for (ItemStack itemstack : iterable) {
			if (!itemstack.isEmpty()) {
				j++;
			}

			i++;
		}

		return i > 0 ? (float)j / i : 0.0F;
	}

	@Override
	public void setSprinting(boolean bl) {
		super.setSprinting(bl);
		AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
		attributeinstance.removeModifier(SPEED_MODIFIER_SPRINTING.id());
		if (bl) {
			attributeinstance.addTransientModifier(SPEED_MODIFIER_SPRINTING);
		}
	}

	protected float getSoundVolume() {
		return 1.0F;
	}

	public float getVoicePitch() {
		return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
	}

	protected boolean isImmobile() {
		return this.isDeadOrDying();
	}

	@Override
	public void push(Entity arg) {
		if (!this.isSleeping()) {
			super.push(arg);
		}
	}

	private void dismountVehicle(Entity arg) {
		Vec3 vec3;
		if (this.isRemoved()) {
			vec3 = this.position();
		} else if (!arg.isRemoved() && !this.level().getBlockState(arg.blockPosition()).is(BlockTags.PORTALS)) {
			vec3 = arg.getDismountLocationForPassenger(this);
		} else {
			double d0 = Math.max(this.getY(), arg.getY());
			vec3 = new Vec3(this.getX(), d0, this.getZ());
			boolean flag = this.getBbWidth() <= 4.0F && this.getBbHeight() <= 4.0F;
			if (flag) {
				double d1 = this.getBbHeight() / 2.0;
				Vec3 vec31 = vec3.add(0.0, d1, 0.0);
				VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec31, this.getBbWidth(), this.getBbHeight(), this.getBbWidth()));
				vec3 = (Vec3)this.level()
					.findFreePosition(this, voxelshape, vec31, this.getBbWidth(), this.getBbHeight(), this.getBbWidth())
					.map(argx -> argx.add(0.0, -d1, 0.0))
					.orElse(vec3);
			}
		}

		this.dismountTo(vec3.x, vec3.y, vec3.z);
	}

	@Override
	public boolean shouldShowName() {
		return this.isCustomNameVisible();
	}

	protected float getJumpPower() {
		return this.getJumpPower(1.0F);
	}

	protected float getJumpPower(float f) {
		return (float)this.getAttributeValue(Attributes.JUMP_STRENGTH) * f * this.getBlockJumpFactor() + this.getJumpBoostPower();
	}

	public float getJumpBoostPower() {
		return this.hasEffect(MobEffects.JUMP) ? 0.1F * (this.getEffect(MobEffects.JUMP).getAmplifier() + 1.0F) : 0.0F;
	}

	@VisibleForTesting
	public void jumpFromGround() {
		float f = this.getJumpPower();
		if (!(f <= 1.0E-5F)) {
			Vec3 vec3 = this.getDeltaMovement();
			this.setDeltaMovement(vec3.x, Math.max(f, vec3.y), vec3.z);
			if (this.isSprinting()) {
				float f1 = this.getYRot() * (float) (Math.PI / 180.0);
				this.addDeltaMovement(new Vec3(-Mth.sin(f1) * 0.2, 0.0, Mth.cos(f1) * 0.2));
			}

			this.hasImpulse = true;
			CommonHooks.onLivingJump(this);
		}
	}

	@Deprecated
	protected void goDownInWater() {
		this.sinkInFluid(NeoForgeMod.WATER_TYPE.value());
	}

	@Deprecated
	protected void jumpInLiquid(TagKey<Fluid> arg) {
		this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F * this.getAttributeValue(NeoForgeMod.SWIM_SPEED), 0.0));
	}

	protected float getWaterSlowDown() {
		return 0.8F;
	}

	public boolean canStandOnFluid(FluidState arg) {
		return false;
	}

	@Override
	protected double getDefaultGravity() {
		return this.getAttributeValue(Attributes.GRAVITY);
	}

	protected double getEffectiveGravity() {
		boolean flag = this.getDeltaMovement().y <= 0.0;
		return flag && this.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(this.getGravity(), 0.01) : this.getGravity();
	}

	public void travel(Vec3 arg) {
		if (this.isControlledByLocalInstance()) {
			FluidState fluidstate = this.level().getFluidState(this.blockPosition());
			if ((this.isInWater() || this.isInLava() || this.isInFluidType(fluidstate)) && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
				this.travelInFluid(arg, fluidstate);
			} else if (this.isFallFlying()) {
				this.travelFallFlying();
			} else {
				this.travelInAir(arg);
			}
		}
	}

	private void travelInAir(Vec3 arg) {
		BlockPos blockpos = this.getBlockPosBelowThatAffectsMyMovement();
		float f = this.onGround() ? this.level().getBlockState(blockpos).getFriction(this.level(), blockpos, this) : 1.0F;
		float f1 = f * 0.91F;
		Vec3 vec3 = this.handleRelativeFrictionAndCalculateMovement(arg, f);
		double d0 = vec3.y;
		MobEffectInstance mobeffectinstance = this.getEffect(MobEffects.LEVITATION);
		if (mobeffectinstance != null) {
			d0 += (0.05 * (mobeffectinstance.getAmplifier() + 1) - vec3.y) * 0.2;
		} else if (!this.level().isClientSide || this.level().hasChunkAt(blockpos)) {
			d0 -= this.getEffectiveGravity();
		} else if (this.getY() > this.level().getMinY()) {
			d0 = -0.1;
		} else {
			d0 = 0.0;
		}

		if (this.shouldDiscardFriction()) {
			this.setDeltaMovement(vec3.x, d0, vec3.z);
		} else {
			float f2 = this instanceof FlyingAnimal ? f1 : 0.98F;
			this.setDeltaMovement(vec3.x * f1, d0 * f2, vec3.z * f1);
		}
	}

	@Deprecated
	private void travelInFluid(Vec3 arg) {
		this.travelInFluid(arg, Fluids.EMPTY.defaultFluidState());
	}

	private void travelInFluid(Vec3 arg, FluidState fluidState) {
		boolean flag = this.getDeltaMovement().y <= 0.0;
		double d0 = this.getY();
		double d1 = this.getEffectiveGravity();
		if (this.isInWater() || this.isInFluidType(fluidState) && !this.moveInFluid(fluidState, arg, d1)) {
			float f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
			float f1 = 0.02F;
			float f2 = (float)this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
			if (!this.onGround()) {
				f2 *= 0.5F;
			}

			if (f2 > 0.0F) {
				f += (0.54600006F - f) * f2;
				f1 += (this.getSpeed() - f1) * f2;
			}

			if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
				f = 0.96F;
			}

			f1 *= (float)this.getAttributeValue(NeoForgeMod.SWIM_SPEED);
			this.moveRelative(f1, arg);
			this.move(MoverType.SELF, this.getDeltaMovement());
			Vec3 vec3 = this.getDeltaMovement();
			if (this.horizontalCollision && this.onClimbable()) {
				vec3 = new Vec3(vec3.x, 0.2, vec3.z);
			}

			vec3 = vec3.multiply(f, 0.8F, f);
			this.setDeltaMovement(this.getFluidFallingAdjustedMovement(d1, flag, vec3));
		} else {
			this.moveRelative(0.02F, arg);
			this.move(MoverType.SELF, this.getDeltaMovement());
			if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
				this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
				Vec3 vec31 = this.getFluidFallingAdjustedMovement(d1, flag, this.getDeltaMovement());
				this.setDeltaMovement(vec31);
			} else {
				this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
			}

			if (d1 != 0.0) {
				this.setDeltaMovement(this.getDeltaMovement().add(0.0, -d1 / 4.0, 0.0));
			}
		}

		Vec3 vec32 = this.getDeltaMovement();
		if (this.horizontalCollision && this.isFree(vec32.x, vec32.y + 0.6F - this.getY() + d0, vec32.z)) {
			this.setDeltaMovement(vec32.x, 0.3F, vec32.z);
		}
	}

	private void travelFallFlying() {
		Vec3 vec3 = this.getDeltaMovement();
		double d0 = vec3.horizontalDistance();
		this.setDeltaMovement(this.updateFallFlyingMovement(vec3));
		this.move(MoverType.SELF, this.getDeltaMovement());
		if (!this.level().isClientSide) {
			double d1 = this.getDeltaMovement().horizontalDistance();
			this.handleFallFlyingCollisions(d0, d1);
		}
	}

	private Vec3 updateFallFlyingMovement(Vec3 arg) {
		Vec3 vec3 = this.getLookAngle();
		float f = this.getXRot() * (float) (Math.PI / 180.0);
		double d0 = Math.sqrt(vec3.x * vec3.x + vec3.z * vec3.z);
		double d1 = arg.horizontalDistance();
		double d2 = this.getEffectiveGravity();
		double d3 = Mth.square(Math.cos(f));
		arg = arg.add(0.0, d2 * (-1.0 + d3 * 0.75), 0.0);
		if (arg.y < 0.0 && d0 > 0.0) {
			double d4 = arg.y * -0.1 * d3;
			arg = arg.add(vec3.x * d4 / d0, d4, vec3.z * d4 / d0);
		}

		if (f < 0.0F && d0 > 0.0) {
			double d5 = d1 * -Mth.sin(f) * 0.04;
			arg = arg.add(-vec3.x * d5 / d0, d5 * 3.2, -vec3.z * d5 / d0);
		}

		if (d0 > 0.0) {
			arg = arg.add((vec3.x / d0 * d1 - arg.x) * 0.1, 0.0, (vec3.z / d0 * d1 - arg.z) * 0.1);
		}

		return arg.multiply(0.99F, 0.98F, 0.99F);
	}

	private void handleFallFlyingCollisions(double d, double e) {
		if (this.horizontalCollision) {
			double d0 = d - e;
			float f = (float)(d0 * 10.0 - 3.0);
			if (f > 0.0F) {
				this.playSound(this.getFallDamageSound((int)f), 1.0F, 1.0F);
				this.hurt(this.damageSources().flyIntoWall(), f);
			}
		}
	}

	private void travelRidden(Player arg, Vec3 arg2) {
		Vec3 vec3 = this.getRiddenInput(arg, arg2);
		this.tickRidden(arg, vec3);
		if (this.isControlledByLocalInstance()) {
			this.setSpeed(this.getRiddenSpeed(arg));
			this.travel(vec3);
		} else {
			this.setDeltaMovement(Vec3.ZERO);
		}
	}

	protected void tickRidden(Player arg, Vec3 arg2) {
	}

	protected Vec3 getRiddenInput(Player arg, Vec3 arg2) {
		return arg2;
	}

	protected float getRiddenSpeed(Player arg) {
		return this.getSpeed();
	}

	public void calculateEntityAnimation(boolean bl) {
		float f = (float)Mth.length(this.getX() - this.xo, bl ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
		if (!this.isPassenger() && this.isAlive()) {
			this.updateWalkAnimation(f);
		} else {
			this.walkAnimation.stop();
		}
	}

	protected void updateWalkAnimation(float g) {
		float f = Math.min(g * 4.0F, 1.0F);
		this.walkAnimation.update(f, 0.4F, this.isBaby() ? 3.0F : 1.0F);
	}

	private Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 arg, float f) {
		this.moveRelative(this.getFrictionInfluencedSpeed(f), arg);
		this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
		this.move(MoverType.SELF, this.getDeltaMovement());
		Vec3 vec3 = this.getDeltaMovement();
		if ((this.horizontalCollision || this.jumping)
			&& (this.onClimbable() || this.getInBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
			vec3 = new Vec3(vec3.x, 0.2, vec3.z);
		}

		return vec3;
	}

	public Vec3 getFluidFallingAdjustedMovement(double d, boolean bl, Vec3 arg) {
		if (d != 0.0 && !this.isSprinting()) {
			double d0;
			if (bl && Math.abs(arg.y - 0.005) >= 0.003 && Math.abs(arg.y - d / 16.0) < 0.003) {
				d0 = -0.003;
			} else {
				d0 = arg.y - d / 16.0;
			}

			return new Vec3(arg.x, d0, arg.z);
		} else {
			return arg;
		}
	}

	private Vec3 handleOnClimbable(Vec3 arg) {
		if (this.onClimbable()) {
			this.resetFallDistance();
			float f = 0.15F;
			double d0 = Mth.clamp(arg.x, -0.15F, 0.15F);
			double d1 = Mth.clamp(arg.z, -0.15F, 0.15F);
			double d2 = Math.max(arg.y, -0.15F);
			if (d2 < 0.0 && !this.getInBlockState().isScaffolding(this) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
				d2 = 0.0;
			}

			arg = new Vec3(d0, d2, d1);
		}

		return arg;
	}

	private float getFrictionInfluencedSpeed(float f) {
		return this.onGround() ? this.getSpeed() * (0.21600002F / (f * f * f)) : this.getFlyingSpeed();
	}

	protected float getFlyingSpeed() {
		return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
	}

	public float getSpeed() {
		return this.speed;
	}

	public void setSpeed(float f) {
		this.speed = f;
	}

	public boolean doHurtTarget(ServerLevel arg, Entity arg2) {
		this.setLastHurtMob(arg2);
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		this.updatingUsingItem();
		this.updateSwimAmount();
		if (!this.level().isClientSide) {
			int i = this.getArrowCount();
			if (i > 0) {
				if (this.removeArrowTime <= 0) {
					this.removeArrowTime = 20 * (30 - i);
				}

				this.removeArrowTime--;
				if (this.removeArrowTime <= 0) {
					this.setArrowCount(i - 1);
				}
			}

			int j = this.getStingerCount();
			if (j > 0) {
				if (this.removeStingerTime <= 0) {
					this.removeStingerTime = 20 * (30 - j);
				}

				this.removeStingerTime--;
				if (this.removeStingerTime <= 0) {
					this.setStingerCount(j - 1);
				}
			}

			this.detectEquipmentUpdates();
			if (this.tickCount % 20 == 0) {
				this.getCombatTracker().recheckStatus();
			}

			if (this.isSleeping() && !this.checkBedExists()) {
				this.stopSleeping();
			}
		}

		if (!this.isRemoved()) {
			this.aiStep();
		}

		double d1 = this.getX() - this.xo;
		double d0 = this.getZ() - this.zo;
		float f = (float)(d1 * d1 + d0 * d0);
		float f1 = this.yBodyRot;
		float f2 = 0.0F;
		this.oRun = this.run;
		float f3 = 0.0F;
		if (f > 0.0025000002F) {
			f3 = 1.0F;
			f2 = (float)Math.sqrt(f) * 3.0F;
			float f4 = (float)Mth.atan2(d0, d1) * (180.0F / (float)Math.PI) - 90.0F;
			float f5 = Mth.abs(Mth.wrapDegrees(this.getYRot()) - f4);
			if (95.0F < f5 && f5 < 265.0F) {
				f1 = f4 - 180.0F;
			} else {
				f1 = f4;
			}
		}

		if (this.attackAnim > 0.0F) {
			f1 = this.getYRot();
		}

		if (!this.onGround()) {
			f3 = 0.0F;
		}

		this.run = this.run + (f3 - this.run) * 0.3F;
		ProfilerFiller profilerfiller = Profiler.get();
		profilerfiller.push("headTurn");
		f2 = this.tickHeadTurn(f1, f2);
		profilerfiller.pop();
		profilerfiller.push("rangeChecks");

		while (this.getYRot() - this.yRotO < -180.0F) {
			this.yRotO -= 360.0F;
		}

		while (this.getYRot() - this.yRotO >= 180.0F) {
			this.yRotO += 360.0F;
		}

		while (this.yBodyRot - this.yBodyRotO < -180.0F) {
			this.yBodyRotO -= 360.0F;
		}

		while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
			this.yBodyRotO += 360.0F;
		}

		while (this.getXRot() - this.xRotO < -180.0F) {
			this.xRotO -= 360.0F;
		}

		while (this.getXRot() - this.xRotO >= 180.0F) {
			this.xRotO += 360.0F;
		}

		while (this.yHeadRot - this.yHeadRotO < -180.0F) {
			this.yHeadRotO -= 360.0F;
		}

		while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
			this.yHeadRotO += 360.0F;
		}

		profilerfiller.pop();
		this.animStep += f2;
		if (this.isFallFlying()) {
			this.fallFlyTicks++;
		} else {
			this.fallFlyTicks = 0;
		}

		if (this.isSleeping()) {
			this.setXRot(0.0F);
		}

		this.refreshDirtyAttributes();
		float f6 = this.getScale();
		if (f6 != this.appliedScale) {
			this.appliedScale = f6;
			this.refreshDimensions();
		}

		this.elytraAnimationState.tick();
	}

	private void detectEquipmentUpdates() {
		Map<EquipmentSlot, ItemStack> map = this.collectEquipmentChanges();
		if (map != null) {
			this.handleHandSwap(map);
			if (!map.isEmpty()) {
				this.handleEquipmentChanges(map);
			}
		}
	}

	@Nullable
	private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
		Map<EquipmentSlot, ItemStack> map = null;

		for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
			ItemStack itemstack = switch (equipmentslot.getType()) {
				case HAND -> this.getLastHandItem(equipmentslot);
				case HUMANOID_ARMOR -> this.getLastArmorItem(equipmentslot);
				case ANIMAL_ARMOR -> this.lastBodyItemStack;
			};
			ItemStack itemstack1 = this.getItemBySlot(equipmentslot);
			if (this.equipmentHasChanged(itemstack, itemstack1)) {
				NeoForge.EVENT_BUS.post(new LivingEquipmentChangeEvent(this, equipmentslot, itemstack, itemstack1));
				if (map == null) {
					map = Maps.newEnumMap(EquipmentSlot.class);
				}

				map.put(equipmentslot, itemstack1);
				AttributeMap attributemap = this.getAttributes();
				if (!itemstack.isEmpty()) {
					this.stopLocationBasedEffects(itemstack, equipmentslot, attributemap);
				}
			}
		}

		if (map != null) {
			for (Entry<EquipmentSlot, ItemStack> entry : map.entrySet()) {
				EquipmentSlot equipmentslot1 = (EquipmentSlot)entry.getKey();
				ItemStack itemstack2 = (ItemStack)entry.getValue();
				if (!itemstack2.isEmpty() && !itemstack2.isBroken()) {
					itemstack2.forEachModifier(equipmentslot1, (arg, arg2) -> {
						AttributeInstance attributeinstance = this.attributes.getInstance(arg);
						if (attributeinstance != null) {
							attributeinstance.removeModifier(arg2.id());
							attributeinstance.addTransientModifier(arg2);
						}
					});
					if (this.level() instanceof ServerLevel serverlevel) {
						EnchantmentHelper.runLocationChangedEffects(serverlevel, itemstack2, this, equipmentslot1);
					}
				}
			}
		}

		return map;
	}

	public boolean equipmentHasChanged(ItemStack arg, ItemStack arg2) {
		return !ItemStack.matches(arg2, arg);
	}

	private void handleHandSwap(Map<EquipmentSlot, ItemStack> map) {
		ItemStack itemstack = (ItemStack)map.get(EquipmentSlot.MAINHAND);
		ItemStack itemstack1 = (ItemStack)map.get(EquipmentSlot.OFFHAND);
		if (itemstack != null
			&& itemstack1 != null
			&& ItemStack.matches(itemstack, this.getLastHandItem(EquipmentSlot.OFFHAND))
			&& ItemStack.matches(itemstack1, this.getLastHandItem(EquipmentSlot.MAINHAND))) {
			((ServerLevel)this.level()).getChunkSource().broadcast(this, new ClientboundEntityEventPacket(this, (byte)55));
			map.remove(EquipmentSlot.MAINHAND);
			map.remove(EquipmentSlot.OFFHAND);
			this.setLastHandItem(EquipmentSlot.MAINHAND, itemstack.copy());
			this.setLastHandItem(EquipmentSlot.OFFHAND, itemstack1.copy());
		}
	}

	private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> map) {
		List<Pair<EquipmentSlot, ItemStack>> list = Lists.<Pair<EquipmentSlot, ItemStack>>newArrayListWithCapacity(map.size());
		map.forEach((arg, arg2) -> {
			ItemStack itemstack = arg2.copy();
			list.add(Pair.of(arg, itemstack));
			switch (arg.getType()) {
				case HAND:
					this.setLastHandItem(arg, itemstack);
					break;
				case HUMANOID_ARMOR:
					this.setLastArmorItem(arg, itemstack);
					break;
				case ANIMAL_ARMOR:
					this.lastBodyItemStack = itemstack;
			}
		});
		((ServerLevel)this.level()).getChunkSource().broadcast(this, new ClientboundSetEquipmentPacket(this.getId(), list));
	}

	private ItemStack getLastArmorItem(EquipmentSlot arg) {
		return this.lastArmorItemStacks.get(arg.getIndex());
	}

	private void setLastArmorItem(EquipmentSlot arg, ItemStack arg2) {
		this.lastArmorItemStacks.set(arg.getIndex(), arg2);
	}

	private ItemStack getLastHandItem(EquipmentSlot arg) {
		return this.lastHandItemStacks.get(arg.getIndex());
	}

	private void setLastHandItem(EquipmentSlot arg, ItemStack arg2) {
		this.lastHandItemStacks.set(arg.getIndex(), arg2);
	}

	protected float tickHeadTurn(float g, float h) {
		float f = Mth.wrapDegrees(g - this.yBodyRot);
		this.yBodyRot += f * 0.3F;
		float f1 = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
		float f2 = this.getMaxHeadRotationRelativeToBody();
		if (Math.abs(f1) > f2) {
			this.yBodyRot = this.yBodyRot + (f1 - Mth.sign(f1) * f2);
		}

		boolean flag = f1 < -90.0F || f1 >= 90.0F;
		if (flag) {
			h *= -1.0F;
		}

		return h;
	}

	protected float getMaxHeadRotationRelativeToBody() {
		return 50.0F;
	}

	public void aiStep() {
		if (this.noJumpDelay > 0) {
			this.noJumpDelay--;
		}

		if (this.lerpSteps > 0) {
			this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
			this.lerpSteps--;
		} else if (!this.isEffectiveAi()) {
			this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
		}

		if (this.lerpHeadSteps > 0) {
			this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
			this.lerpHeadSteps--;
		}

		Vec3 vec3 = this.getDeltaMovement();
		double d0 = vec3.x;
		double d1 = vec3.y;
		double d2 = vec3.z;
		if (Math.abs(vec3.x) < 0.003) {
			d0 = 0.0;
		}

		if (Math.abs(vec3.y) < 0.003) {
			d1 = 0.0;
		}

		if (Math.abs(vec3.z) < 0.003) {
			d2 = 0.0;
		}

		this.setDeltaMovement(d0, d1, d2);
		ProfilerFiller profilerfiller = Profiler.get();
		profilerfiller.push("ai");
		if (this.isImmobile()) {
			this.jumping = false;
			this.xxa = 0.0F;
			this.zza = 0.0F;
		} else if (this.isEffectiveAi()) {
			profilerfiller.push("newAi");
			this.serverAiStep();
			profilerfiller.pop();
		}

		profilerfiller.pop();
		profilerfiller.push("jump");
		if (this.jumping && this.isAffectedByFluids()) {
			FluidType fluidType = this.getMaxHeightFluidType();
			double d3;
			if (!fluidType.isAir()) {
				d3 = this.getFluidTypeHeight(fluidType);
			} else if (this.isInLava()) {
				d3 = this.getFluidHeight(FluidTags.LAVA);
			} else {
				d3 = this.getFluidHeight(FluidTags.WATER);
			}

			boolean flag = this.isInWater() && d3 > 0.0;
			double d4 = this.getFluidJumpThreshold();
			if (!flag || this.onGround() && !(d3 > d4)) {
				if (!this.isInLava() || this.onGround() && !(d3 > d4)) {
					if (fluidType.isAir() || this.onGround() && !(d3 > d4)) {
						if ((this.onGround() || flag && d3 <= d4) && this.noJumpDelay == 0) {
							this.jumpFromGround();
							this.noJumpDelay = 10;
						}
					} else {
						this.jumpInFluid(fluidType);
					}
				} else {
					this.jumpInFluid(NeoForgeMod.LAVA_TYPE.value());
				}
			} else {
				this.jumpInFluid(NeoForgeMod.WATER_TYPE.value());
			}
		} else {
			this.noJumpDelay = 0;
		}

		profilerfiller.pop();
		profilerfiller.push("travel");
		this.xxa *= 0.98F;
		this.zza *= 0.98F;
		if (this.isFallFlying()) {
			this.updateFallFlying();
		}

		AABB aabb = this.getBoundingBox();
		Vec3 vec31 = new Vec3(this.xxa, this.yya, this.zza);
		if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
			this.resetFallDistance();
		}

		if (this.getControllingPassenger() instanceof Player player && this.isAlive()) {
			this.travelRidden(player, vec31);
		} else {
			this.travel(vec31);
		}

		if (!this.level().isClientSide() || this.isControlledByLocalInstance()) {
			this.applyEffectsFromBlocks();
		}

		this.calculateEntityAnimation(this instanceof FlyingAnimal);
		profilerfiller.pop();
		profilerfiller.push("freezing");
		if (!this.level().isClientSide && !this.isDeadOrDying()) {
			int i = this.getTicksFrozen();
			if (this.isInPowderSnow && this.canFreeze()) {
				this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), i + 1));
			} else {
				this.setTicksFrozen(Math.max(0, i - 2));
			}
		}

		this.removeFrost();
		this.tryAddFrost();
		if (this.level() instanceof ServerLevel serverlevel && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
			this.hurtServer(serverlevel, this.damageSources().freeze(), 1.0F);
		}

		profilerfiller.pop();
		profilerfiller.push("push");
		if (this.autoSpinAttackTicks > 0) {
			this.autoSpinAttackTicks--;
			this.checkAutoSpinAttack(aabb, this.getBoundingBox());
		}

		this.pushEntities();
		profilerfiller.pop();
		if (this.level() instanceof ServerLevel serverlevel1 && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
			this.hurtServer(serverlevel1, this.damageSources().drown(), 1.0F);
		}
	}

	public boolean isSensitiveToWater() {
		return false;
	}

	protected void updateFallFlying() {
		this.checkSlowFallDistance();
		if (!this.level().isClientSide) {
			if (!this.canGlide()) {
				this.setSharedFlag(7, false);
				return;
			}

			int i = this.fallFlyTicks + 1;
			if (i % 10 == 0) {
				int j = i / 10;
				if (j % 2 == 0) {
					List<EquipmentSlot> list = EquipmentSlot.VALUES.stream().filter(arg -> canGlideUsing(this.getItemBySlot(arg), arg)).toList();
					EquipmentSlot equipmentslot = Util.getRandom(list, this.random);
					this.getItemBySlot(equipmentslot).hurtAndBreak(1, this, equipmentslot);
				}

				this.gameEvent(GameEvent.ELYTRA_GLIDE);
			}
		}
	}

	protected boolean canGlide() {
		if (!this.onGround() && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
			for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
				if (canGlideUsing(this.getItemBySlot(equipmentslot), equipmentslot)) {
					return true;
				}
			}

			return false;
		} else {
			return false;
		}
	}

	protected void serverAiStep() {
	}

	protected void pushEntities() {
		if (this.level() instanceof ServerLevel serverlevel) {
			List<Entity> list = this.level().getEntities(this, this.getBoundingBox(), EntitySelector.pushableBy(this));
			if (!list.isEmpty()) {
				int i = serverlevel.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
				if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
					int j = 0;

					for (Entity entity : list) {
						if (!entity.isPassenger()) {
							j++;
						}
					}

					if (j > i - 1) {
						this.hurtServer(serverlevel, this.damageSources().cramming(), 6.0F);
					}
				}

				for (Entity entity1 : list) {
					this.doPush(entity1);
				}
			}
		} else {
			this.level().getEntities(EntityTypeTest.forClass(Player.class), this.getBoundingBox(), EntitySelector.pushableBy(this)).forEach(this::doPush);
		}
	}

	protected void checkAutoSpinAttack(AABB arg, AABB arg2) {
		AABB aabb = arg.minmax(arg2);
		List<Entity> list = this.level().getEntities(this, aabb);
		if (!list.isEmpty()) {
			for (Entity entity : list) {
				if (entity instanceof LivingEntity) {
					this.doAutoAttackOnTouch((LivingEntity)entity);
					this.autoSpinAttackTicks = 0;
					this.setDeltaMovement(this.getDeltaMovement().scale(-0.2));
					break;
				}
			}
		} else if (this.horizontalCollision) {
			this.autoSpinAttackTicks = 0;
		}

		if (!this.level().isClientSide && this.autoSpinAttackTicks <= 0) {
			this.setLivingEntityFlag(4, false);
			this.autoSpinAttackDmg = 0.0F;
			this.autoSpinAttackItemStack = null;
		}
	}

	protected void doPush(Entity arg) {
		arg.push(this);
	}

	protected void doAutoAttackOnTouch(LivingEntity arg) {
	}

	public boolean isAutoSpinAttack() {
		return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
	}

	@Override
	public void stopRiding() {
		Entity entity = this.getVehicle();
		super.stopRiding();
		if (entity != null && entity != this.getVehicle() && !this.level().isClientSide) {
			this.dismountVehicle(entity);
		}
	}

	@Override
	public void rideTick() {
		super.rideTick();
		this.oRun = this.run;
		this.run = 0.0F;
		this.resetFallDistance();
	}

	@Override
	public void cancelLerp() {
		this.lerpSteps = 0;
	}

	@Override
	public void lerpTo(double d, double e, double f, float g, float h, int i) {
		this.lerpX = d;
		this.lerpY = e;
		this.lerpZ = f;
		this.lerpYRot = g;
		this.lerpXRot = h;
		this.lerpSteps = i;
	}

	@Override
	public double lerpTargetX() {
		return this.lerpSteps > 0 ? this.lerpX : this.getX();
	}

	@Override
	public double lerpTargetY() {
		return this.lerpSteps > 0 ? this.lerpY : this.getY();
	}

	@Override
	public double lerpTargetZ() {
		return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
	}

	@Override
	public float lerpTargetXRot() {
		return this.lerpSteps > 0 ? (float)this.lerpXRot : this.getXRot();
	}

	@Override
	public float lerpTargetYRot() {
		return this.lerpSteps > 0 ? (float)this.lerpYRot : this.getYRot();
	}

	@Override
	public void lerpHeadTo(float f, int i) {
		this.lerpYHeadRot = f;
		this.lerpHeadSteps = i;
	}

	public void setJumping(boolean bl) {
		this.jumping = bl;
	}

	public void onItemPickup(ItemEntity arg) {
		Entity entity = arg.getOwner();
		if (entity instanceof ServerPlayer) {
			CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayer)entity, arg.getItem(), this);
		}
	}

	public void take(Entity arg, int i) {
		if (!arg.isRemoved() && !this.level().isClientSide && (arg instanceof ItemEntity || arg instanceof AbstractArrow || arg instanceof ExperienceOrb)) {
			((ServerLevel)this.level()).getChunkSource().broadcast(arg, new ClientboundTakeItemEntityPacket(arg.getId(), this.getId(), i));
		}
	}

	public boolean hasLineOfSight(Entity arg) {
		return this.hasLineOfSight(arg, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, arg::getEyeY);
	}

	public boolean hasLineOfSight(Entity arg, ClipContext.Block arg2, ClipContext.Fluid arg3, DoubleSupplier doubleSupplier) {
		if (arg.level() != this.level()) {
			return false;
		} else {
			Vec3 vec3 = new Vec3(this.getX(), this.getEyeY(), this.getZ());
			Vec3 vec31 = new Vec3(arg.getX(), doubleSupplier.getAsDouble(), arg.getZ());
			return vec31.distanceTo(vec3) > 128.0 ? false : this.level().clip(new ClipContext(vec3, vec31, arg2, arg3, this)).getType() == HitResult.Type.MISS;
		}
	}

	@Override
	public float getViewYRot(float f) {
		return f == 1.0F ? this.yHeadRot : Mth.rotLerp(f, this.yHeadRotO, this.yHeadRot);
	}

	public float getAttackAnim(float g) {
		float f = this.attackAnim - this.oAttackAnim;
		if (f < 0.0F) {
			f++;
		}

		return this.oAttackAnim + f * g;
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean isPushable() {
		return this.isAlive() && !this.isSpectator() && !this.onClimbable();
	}

	@Override
	public float getYHeadRot() {
		return this.yHeadRot;
	}

	@Override
	public void setYHeadRot(float f) {
		this.yHeadRot = f;
	}

	@Override
	public void setYBodyRot(float f) {
		this.yBodyRot = f;
	}

	@Override
	public Vec3 getRelativePortalPosition(Direction.Axis arg, BlockUtil.FoundRectangle arg2) {
		return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(arg, arg2));
	}

	public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 arg) {
		return new Vec3(arg.x, arg.y, 0.0);
	}

	public float getAbsorptionAmount() {
		return this.absorptionAmount;
	}

	public final void setAbsorptionAmount(float f) {
		this.internalSetAbsorptionAmount(Mth.clamp(f, 0.0F, this.getMaxAbsorption()));
	}

	protected void internalSetAbsorptionAmount(float f) {
		this.absorptionAmount = f;
	}

	public void onEnterCombat() {
	}

	public void onLeaveCombat() {
	}

	protected void updateEffectVisibility() {
		this.effectsDirty = true;
	}

	public abstract HumanoidArm getMainArm();

	public boolean isUsingItem() {
		return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
	}

	public InteractionHand getUsedItemHand() {
		return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	private void updatingUsingItem() {
		if (this.isUsingItem()) {
			ItemStack itemStack = this.getItemInHand(this.getUsedItemHand());
			if (CommonHooks.canContinueUsing(this.useItem, itemStack)) {
				this.useItem = itemStack;
			}

			if (itemStack == this.useItem) {
				this.updateUsingItem(this.useItem);
			} else {
				this.stopUsingItem();
			}
		}
	}

	protected void updateUsingItem(ItemStack arg) {
		if (!arg.isEmpty()) {
			this.useItemRemaining = EventHooks.onItemUseTick(this, arg, this.getUseItemRemainingTicks());
		}

		if (this.getUseItemRemainingTicks() > 0) {
			arg.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
		}

		if (--this.useItemRemaining <= 0 && !this.level().isClientSide && !arg.useOnRelease()) {
			this.completeUsingItem();
		}
	}

	private void updateSwimAmount() {
		this.swimAmountO = this.swimAmount;
		if (this.isVisuallySwimming()) {
			this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
		} else {
			this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
		}
	}

	protected void setLivingEntityFlag(int j, boolean bl) {
		int i = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
		if (bl) {
			i |= j;
		} else {
			i &= ~j;
		}

		this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)i);
	}

	public void startUsingItem(InteractionHand arg) {
		ItemStack itemstack = this.getItemInHand(arg);
		if (!itemstack.isEmpty() && !this.isUsingItem()) {
			int duration = EventHooks.onItemUseStart(this, itemstack, itemstack.getUseDuration(this));
			if (duration < 0) {
				return;
			}

			this.useItem = itemstack;
			this.useItemRemaining = duration;
			if (!this.level().isClientSide) {
				this.setLivingEntityFlag(1, true);
				this.setLivingEntityFlag(2, arg == InteractionHand.OFF_HAND);
				this.gameEvent(GameEvent.ITEM_INTERACT_START);
			}
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> arg) {
		super.onSyncedDataUpdated(arg);
		if (SLEEPING_POS_ID.equals(arg)) {
			if (this.level().isClientSide) {
				this.getSleepingPos().ifPresent(this::setPosToBed);
			}
		} else if (DATA_LIVING_ENTITY_FLAGS.equals(arg) && this.level().isClientSide) {
			if (this.isUsingItem() && this.useItem.isEmpty()) {
				this.useItem = this.getItemInHand(this.getUsedItemHand());
				if (!this.useItem.isEmpty()) {
					this.useItemRemaining = this.useItem.getUseDuration(this);
				}
			} else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
				this.useItem = ItemStack.EMPTY;
				this.useItemRemaining = 0;
			}
		}
	}

	@Override
	public void lookAt(EntityAnchorArgument.Anchor arg, Vec3 arg2) {
		super.lookAt(arg, arg2);
		this.yHeadRotO = this.yHeadRot;
		this.yBodyRot = this.yHeadRot;
		this.yBodyRotO = this.yBodyRot;
	}

	@Override
	public float getPreciseBodyRotation(float f) {
		return Mth.lerp(f, this.yBodyRotO, this.yBodyRot);
	}

	public void spawnItemParticles(ItemStack arg, int j) {
		for (int i = 0; i < j; i++) {
			Vec3 vec3 = new Vec3((this.random.nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, 0.0);
			vec3 = vec3.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
			vec3 = vec3.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
			double d0 = -this.random.nextFloat() * 0.6 - 0.3;
			Vec3 vec31 = new Vec3((this.random.nextFloat() - 0.5) * 0.3, d0, 0.6);
			vec31 = vec31.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
			vec31 = vec31.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
			vec31 = vec31.add(this.getX(), this.getEyeY(), this.getZ());
			this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, arg), vec31.x, vec31.y, vec31.z, vec3.x, vec3.y + 0.05, vec3.z);
		}
	}

	protected void completeUsingItem() {
		if (!this.level().isClientSide || this.isUsingItem()) {
			InteractionHand interactionhand = this.getUsedItemHand();
			if (!this.useItem.equals(this.getItemInHand(interactionhand))) {
				this.releaseUsingItem();
			} else if (!this.useItem.isEmpty() && this.isUsingItem()) {
				ItemStack copy = this.useItem.copy();
				ItemStack itemstack = EventHooks.onItemUseFinish(this, copy, this.getUseItemRemainingTicks(), this.useItem.finishUsingItem(this.level(), this));
				if (itemstack != this.useItem) {
					this.setItemInHand(interactionhand, itemstack);
				}

				this.stopUsingItem();
			}
		}
	}

	public void handleExtraItemsCreatedOnUse(ItemStack arg) {
	}

	public ItemStack getUseItem() {
		return this.useItem;
	}

	public int getUseItemRemainingTicks() {
		return this.useItemRemaining;
	}

	public int getTicksUsingItem() {
		return this.isUsingItem() ? this.useItem.getUseDuration(this) - this.getUseItemRemainingTicks() : 0;
	}

	public void releaseUsingItem() {
		if (!this.useItem.isEmpty()) {
			if (!EventHooks.onUseItemStop(this, this.useItem, this.getUseItemRemainingTicks())) {
				ItemStack copy = this instanceof Player ? this.useItem.copy() : null;
				this.useItem.releaseUsing(this.level(), this, this.getUseItemRemainingTicks());
				if (copy != null && this.useItem.isEmpty()) {
					EventHooks.onPlayerDestroyItem((Player)this, copy, this.getUsedItemHand());
				}
			}

			if (this.useItem.useOnRelease()) {
				this.updatingUsingItem();
			}
		}

		this.stopUsingItem();
	}

	public void stopUsingItem() {
		if (this.isUsingItem() && !this.useItem.isEmpty()) {
			this.useItem.onStopUsing(this, this.useItemRemaining);
		}

		if (!this.level().isClientSide) {
			boolean flag = this.isUsingItem();
			this.setLivingEntityFlag(1, false);
			if (flag) {
				this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
			}
		}

		this.useItem = ItemStack.EMPTY;
		this.useItemRemaining = 0;
	}

	public boolean isBlocking() {
		return this.getItemBlockingWith() != null;
	}

	@Nullable
	public ItemStack getItemBlockingWith() {
		if (this.isUsingItem() && !this.useItem.isEmpty()) {
			Item item = this.useItem.getItem();
			if (!this.useItem.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
				return null;
			} else {
				return item.getUseDuration(this.useItem, this) - this.useItemRemaining < 5 ? null : this.useItem;
			}
		} else {
			return null;
		}
	}

	public boolean isSuppressingSlidingDownLadder() {
		return this.isShiftKeyDown();
	}

	public boolean isFallFlying() {
		return this.getSharedFlag(7);
	}

	@Override
	public boolean isVisuallySwimming() {
		return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
	}

	public int getFallFlyingTicks() {
		return this.fallFlyTicks;
	}

	public boolean randomTeleport(double d, double e, double f, boolean bl) {
		double d0 = this.getX();
		double d1 = this.getY();
		double d2 = this.getZ();
		double d3 = e;
		boolean flag = false;
		BlockPos blockpos = BlockPos.containing(d, e, f);
		Level level = this.level();
		if (level.hasChunkAt(blockpos)) {
			boolean flag1 = false;

			while (!flag1 && blockpos.getY() > level.getMinY()) {
				BlockPos blockpos1 = blockpos.below();
				BlockState blockstate = level.getBlockState(blockpos1);
				if (blockstate.blocksMotion()) {
					flag1 = true;
				} else {
					d3--;
					blockpos = blockpos1;
				}
			}

			if (flag1) {
				this.teleportTo(d, d3, f);
				if (level.noCollision(this) && !level.containsAnyLiquid(this.getBoundingBox())) {
					flag = true;
				}
			}
		}

		if (!flag) {
			this.teleportTo(d0, d1, d2);
			return false;
		} else {
			if (bl) {
				level.broadcastEntityEvent(this, (byte)46);
			}

			if (this instanceof PathfinderMob pathfindermob) {
				pathfindermob.getNavigation().stop();
			}

			return true;
		}
	}

	public boolean isAffectedByPotions() {
		return !this.isDeadOrDying();
	}

	public boolean attackable() {
		return true;
	}

	public void setRecordPlayingNearby(BlockPos arg, boolean bl) {
	}

	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public final EntityDimensions getDimensions(Pose arg) {
		return arg == Pose.SLEEPING ? SLEEPING_DIMENSIONS : this.getDefaultDimensions(arg).scale(this.getScale());
	}

	protected EntityDimensions getDefaultDimensions(Pose arg) {
		return this.getType().getDimensions().scale(this.getAgeScale());
	}

	public ImmutableList<Pose> getDismountPoses() {
		return ImmutableList.of(Pose.STANDING);
	}

	public AABB getLocalBoundsForPose(Pose arg) {
		EntityDimensions entitydimensions = this.getDimensions(arg);
		return new AABB(
			-entitydimensions.width() / 2.0F,
			0.0,
			-entitydimensions.width() / 2.0F,
			entitydimensions.width() / 2.0F,
			entitydimensions.height(),
			entitydimensions.width() / 2.0F
		);
	}

	protected boolean wouldNotSuffocateAtTargetPose(Pose arg) {
		AABB aabb = this.getDimensions(arg).makeBoundingBox(this.position());
		return this.level().noBlockCollision(this, aabb);
	}

	@Override
	public boolean canUsePortal(boolean bl) {
		return super.canUsePortal(bl) && !this.isSleeping();
	}

	public Optional<BlockPos> getSleepingPos() {
		return this.entityData.get(SLEEPING_POS_ID);
	}

	public void setSleepingPos(BlockPos arg) {
		this.entityData.set(SLEEPING_POS_ID, Optional.of(arg));
	}

	public void clearSleepingPos() {
		this.entityData.set(SLEEPING_POS_ID, Optional.empty());
	}

	public boolean isSleeping() {
		return this.getSleepingPos().isPresent();
	}

	public void startSleeping(BlockPos arg) {
		if (this.isPassenger()) {
			this.stopRiding();
		}

		BlockState blockstate = this.level().getBlockState(arg);
		if (blockstate.isBed(this.level(), arg, this)) {
			blockstate.setBedOccupied(this.level(), arg, this, true);
		}

		this.setPose(Pose.SLEEPING);
		this.setPosToBed(arg);
		this.setSleepingPos(arg);
		this.setDeltaMovement(Vec3.ZERO);
		this.hasImpulse = true;
	}

	private void setPosToBed(BlockPos arg) {
		this.setPos(arg.getX() + 0.5, arg.getY() + 0.6875, arg.getZ() + 0.5);
	}

	private boolean checkBedExists() {
		boolean hasBed = (Boolean)this.getSleepingPos().map(pos -> this.level().getBlockState(pos).isBed(this.level(), pos, this)).orElse(false);
		return EventHooks.canEntityContinueSleeping(this, hasBed ? null : Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
	}

	public void stopSleeping() {
		this.getSleepingPos().filter(this.level()::hasChunkAt).ifPresent(arg -> {
			BlockState blockstate = this.level().getBlockState(arg);
			if (blockstate.isBed(this.level(), arg, this)) {
				Direction direction = blockstate.getValue(BedBlock.FACING);
				blockstate.setBedOccupied(this.level(), arg, this, false);
				Vec3 vec31 = (Vec3)BedBlock.findStandUpPosition(this.getType(), this.level(), arg, direction, this.getYRot()).orElseGet(() -> {
					BlockPos blockpos = arg.above();
					return new Vec3(blockpos.getX() + 0.5, blockpos.getY() + 0.1, blockpos.getZ() + 0.5);
				});
				Vec3 vec32 = Vec3.atBottomCenterOf(arg).subtract(vec31).normalize();
				float f = (float)Mth.wrapDegrees(Mth.atan2(vec32.z, vec32.x) * 180.0 / (float) Math.PI - 90.0);
				this.setPos(vec31.x, vec31.y, vec31.z);
				this.setYRot(f);
				this.setXRot(0.0F);
			}
		});
		Vec3 vec3 = this.position();
		this.setPose(Pose.STANDING);
		this.setPos(vec3.x, vec3.y, vec3.z);
		this.clearSleepingPos();
	}

	@Nullable
	public Direction getBedOrientation() {
		BlockPos blockpos = (BlockPos)this.getSleepingPos().orElse(null);
		if (blockpos == null) {
			return Direction.UP;
		} else {
			BlockState state = this.level().getBlockState(blockpos);
			return !state.isBed(this.level(), blockpos, this) ? Direction.UP : state.getBedDirection(this.level(), blockpos);
		}
	}

	@Override
	public boolean isInWall() {
		return !this.isSleeping() && super.isInWall();
	}

	public ItemStack getProjectile(ItemStack arg) {
		return CommonHooks.getProjectile(this, arg, ItemStack.EMPTY);
	}

	private static byte entityEventForEquipmentBreak(EquipmentSlot arg) {
		return switch (arg) {
			case MAINHAND -> 47;
			case OFFHAND -> 48;
			case HEAD -> 49;
			case CHEST -> 50;
			case FEET -> 52;
			case LEGS -> 51;
			case BODY -> 65;
		};
	}

	public void onEquippedItemBroken(Item arg, EquipmentSlot arg2) {
		this.level().broadcastEntityEvent(this, entityEventForEquipmentBreak(arg2));
		this.stopLocationBasedEffects(this.getItemBySlot(arg2), arg2, this.attributes);
	}

	private void stopLocationBasedEffects(ItemStack arg, EquipmentSlot arg2, AttributeMap arg3) {
		arg.forEachModifier(arg2, (arg2x, arg3x) -> {
			AttributeInstance attributeinstance = arg3.getInstance(arg2x);
			if (attributeinstance != null) {
				attributeinstance.removeModifier(arg3x);
			}
		});
		EnchantmentHelper.stopLocationBasedEffects(arg, this, arg2);
	}

	public static EquipmentSlot getSlotForHand(InteractionHand arg) {
		return arg == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
	}

	public final boolean canEquipWithDispenser(ItemStack arg) {
		if (this.isAlive() && !this.isSpectator()) {
			Equippable equippable = arg.get(DataComponents.EQUIPPABLE);
			if (equippable != null && equippable.dispensable()) {
				EquipmentSlot equipmentslot = equippable.slot();
				return this.canUseSlot(equipmentslot) && equippable.canBeEquippedBy(this.getType())
					? this.getItemBySlot(equipmentslot).isEmpty() && this.canDispenserEquipIntoSlot(equipmentslot)
					: false;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	protected boolean canDispenserEquipIntoSlot(EquipmentSlot arg) {
		return true;
	}

	public final EquipmentSlot getEquipmentSlotForItem(ItemStack arg) {
		EquipmentSlot slot = arg.getEquipmentSlot();
		if (slot != null) {
			return slot;
		} else {
			Equippable equippable = arg.get(DataComponents.EQUIPPABLE);
			return equippable != null && this.canUseSlot(equippable.slot()) ? equippable.slot() : EquipmentSlot.MAINHAND;
		}
	}

	public final boolean isEquippableInSlot(ItemStack arg, EquipmentSlot arg2) {
		Equippable equippable = arg.get(DataComponents.EQUIPPABLE);
		return equippable == null
			? arg2 == EquipmentSlot.MAINHAND && this.canUseSlot(EquipmentSlot.MAINHAND)
			: arg2 == equippable.slot() && this.canUseSlot(equippable.slot()) && equippable.canBeEquippedBy(this.getType());
	}

	private static SlotAccess createEquipmentSlotAccess(LivingEntity arg, EquipmentSlot arg2) {
		return arg2 != EquipmentSlot.HEAD && arg2 != EquipmentSlot.MAINHAND && arg2 != EquipmentSlot.OFFHAND
			? SlotAccess.forEquipmentSlot(arg, arg2, arg3 -> arg3.isEmpty() || arg.getEquipmentSlotForItem(arg3) == arg2)
			: SlotAccess.forEquipmentSlot(arg, arg2);
	}

	@Nullable
	private static EquipmentSlot getEquipmentSlot(int i) {
		if (i == 100 + EquipmentSlot.HEAD.getIndex()) {
			return EquipmentSlot.HEAD;
		} else if (i == 100 + EquipmentSlot.CHEST.getIndex()) {
			return EquipmentSlot.CHEST;
		} else if (i == 100 + EquipmentSlot.LEGS.getIndex()) {
			return EquipmentSlot.LEGS;
		} else if (i == 100 + EquipmentSlot.FEET.getIndex()) {
			return EquipmentSlot.FEET;
		} else if (i == 98) {
			return EquipmentSlot.MAINHAND;
		} else if (i == 99) {
			return EquipmentSlot.OFFHAND;
		} else {
			return i == 105 ? EquipmentSlot.BODY : null;
		}
	}

	@Override
	public SlotAccess getSlot(int i) {
		EquipmentSlot equipmentslot = getEquipmentSlot(i);
		return equipmentslot != null ? createEquipmentSlotAccess(this, equipmentslot) : super.getSlot(i);
	}

	@Override
	public boolean canFreeze() {
		if (this.isSpectator()) {
			return false;
		} else {
			boolean flag = !this.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
				&& !this.getItemBySlot(EquipmentSlot.CHEST).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
				&& !this.getItemBySlot(EquipmentSlot.LEGS).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
				&& !this.getItemBySlot(EquipmentSlot.FEET).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
				&& !this.getItemBySlot(EquipmentSlot.BODY).is(ItemTags.FREEZE_IMMUNE_WEARABLES);
			return flag && super.canFreeze();
		}
	}

	@Override
	public boolean isCurrentlyGlowing() {
		return !this.level().isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
	}

	@Override
	public float getVisualRotationYInDegrees() {
		return this.yBodyRot;
	}

	@Override
	public void recreateFromPacket(ClientboundAddEntityPacket arg) {
		double d0 = arg.getX();
		double d1 = arg.getY();
		double d2 = arg.getZ();
		float f = arg.getYRot();
		float f1 = arg.getXRot();
		this.syncPacketPositionCodec(d0, d1, d2);
		this.yBodyRot = arg.getYHeadRot();
		this.yHeadRot = arg.getYHeadRot();
		this.yBodyRotO = this.yBodyRot;
		this.yHeadRotO = this.yHeadRot;
		this.setId(arg.getId());
		this.setUUID(arg.getUUID());
		this.absMoveTo(d0, d1, d2, f, f1);
		this.setDeltaMovement(arg.getXa(), arg.getYa(), arg.getZa());
	}

	public boolean canDisableShield() {
		return this.getMainHandItem().canDisableShield(this.useItem, this, this);
	}

	@Override
	public float maxUpStep() {
		float f = (float)this.getAttributeValue(Attributes.STEP_HEIGHT);
		return this.getControllingPassenger() instanceof Player ? Math.max(f, 1.0F) : f;
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity arg) {
		return this.position().add(this.getPassengerAttachmentPoint(arg, this.getDimensions(this.getPose()), this.getScale() * this.getAgeScale()));
	}

	protected void lerpHeadRotationStep(int i, double d) {
		this.yHeadRot = (float)Mth.rotLerp(1.0 / i, (double)this.yHeadRot, d);
	}

	@Override
	public void igniteForTicks(int i) {
		super.igniteForTicks(Mth.ceil(i * this.getAttributeValue(Attributes.BURNING_TIME)));
	}

	public boolean hasInfiniteMaterials() {
		return false;
	}

	public boolean isInvulnerableTo(ServerLevel arg, DamageSource arg2) {
		return this.isInvulnerableToBase(arg2) || EnchantmentHelper.isImmuneToDamage(arg, this, arg2);
	}

	public static boolean canGlideUsing(ItemStack arg, EquipmentSlot arg2) {
		if (!arg.has(DataComponents.GLIDER)) {
			return false;
		} else {
			Equippable equippable = arg.get(DataComponents.EQUIPPABLE);
			return equippable != null && arg2 == equippable.slot() && !arg.nextDamageWillBreak();
		}
	}

	@VisibleForTesting
	public int getLastHurtByPlayerTime() {
		return this.lastHurtByPlayerTime;
	}

	public record Fallsounds(SoundEvent small, SoundEvent big) {
	}
}
