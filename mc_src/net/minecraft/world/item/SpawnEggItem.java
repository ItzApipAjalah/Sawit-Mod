package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class SpawnEggItem extends Item {
	private static final Map<EntityType<? extends Mob>, SpawnEggItem> BY_ID = Maps.<EntityType<? extends Mob>, SpawnEggItem>newIdentityHashMap();
	private static final MapCodec<EntityType<?>> ENTITY_TYPE_FIELD_CODEC = BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("id");
	private final int backgroundColor;
	private final int highlightColor;
	private final EntityType<?> defaultType;
	public static final DispenseItemBehavior DEFAULT_DISPENSE_BEHAVIOR = new DefaultDispenseItemBehavior() {
		@Override
		protected ItemStack execute(BlockSource source, ItemStack egg) {
			Direction direction = source.state().getValue(DispenserBlock.FACING);
			EntityType<?> entitytype = ((SpawnEggItem)egg.getItem()).getType(egg);

			try {
				entitytype.spawn(source.level(), egg, null, source.pos().relative(direction), EntitySpawnReason.DISPENSER, direction != Direction.UP, false);
			} catch (Exception var6) {
				LOGGER.error("Error while dispensing spawn egg from dispenser at {}", source.pos(), var6);
				return ItemStack.EMPTY;
			}

			egg.shrink(1);
			source.level().gameEvent(null, GameEvent.ENTITY_PLACE, source.pos());
			return egg;
		}
	};

	public SpawnEggItem(EntityType<? extends Mob> arg, int i, int j, Item.Properties arg2) {
		super(arg2);
		this.defaultType = arg;
		this.backgroundColor = i;
		this.highlightColor = j;
		BY_ID.put(arg, this);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Level level = arg.getLevel();
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		} else {
			ItemStack itemstack = arg.getItemInHand();
			BlockPos blockpos = arg.getClickedPos();
			Direction direction = arg.getClickedFace();
			BlockState blockstate = level.getBlockState(blockpos);
			if (level.getBlockEntity(blockpos) instanceof Spawner spawner) {
				EntityType<?> entitytype1 = this.getType(itemstack);
				spawner.setEntityId(entitytype1, level.getRandom());
				level.sendBlockUpdated(blockpos, blockstate, blockstate, 3);
				level.gameEvent(arg.getPlayer(), GameEvent.BLOCK_CHANGE, blockpos);
				itemstack.shrink(1);
				return InteractionResult.SUCCESS;
			} else {
				BlockPos blockpos1;
				if (blockstate.getCollisionShape(level, blockpos).isEmpty()) {
					blockpos1 = blockpos;
				} else {
					blockpos1 = blockpos.relative(direction);
				}

				EntityType<?> entitytype = this.getType(itemstack);
				if (entitytype.spawn(
						(ServerLevel)level,
						itemstack,
						arg.getPlayer(),
						blockpos1,
						EntitySpawnReason.SPAWN_ITEM_USE,
						true,
						!Objects.equals(blockpos, blockpos1) && direction == Direction.UP
					)
					!= null) {
					itemstack.shrink(1);
					level.gameEvent(arg.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
				}

				return InteractionResult.SUCCESS;
			}
		}
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemstack = arg2.getItemInHand(arg3);
		BlockHitResult blockhitresult = getPlayerPOVHitResult(arg, arg2, ClipContext.Fluid.SOURCE_ONLY);
		if (blockhitresult.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		} else if (arg.isClientSide) {
			return InteractionResult.SUCCESS;
		} else {
			BlockPos blockpos = blockhitresult.getBlockPos();
			if (!(arg.getBlockState(blockpos).getBlock() instanceof LiquidBlock)) {
				return InteractionResult.PASS;
			} else if (arg.mayInteract(arg2, blockpos) && arg2.mayUseItemAt(blockpos, blockhitresult.getDirection(), itemstack)) {
				EntityType<?> entitytype = this.getType(itemstack);
				Entity entity = entitytype.spawn((ServerLevel)arg, itemstack, arg2, blockpos, EntitySpawnReason.SPAWN_ITEM_USE, false, false);
				if (entity == null) {
					return InteractionResult.PASS;
				} else {
					itemstack.consume(1, arg2);
					arg2.awardStat(Stats.ITEM_USED.get(this));
					arg.gameEvent(arg2, GameEvent.ENTITY_PLACE, entity.position());
					return InteractionResult.SUCCESS;
				}
			} else {
				return InteractionResult.FAIL;
			}
		}
	}

	public boolean spawnsEntity(ItemStack arg, EntityType<?> arg2) {
		return Objects.equals(this.getType(arg), arg2);
	}

	public int getColor(int i) {
		return i == 0 ? this.backgroundColor : this.highlightColor;
	}

	@Nullable
	public static SpawnEggItem byId(@Nullable EntityType<?> arg) {
		return (SpawnEggItem)BY_ID.get(arg);
	}

	public static Iterable<SpawnEggItem> eggs() {
		return Iterables.unmodifiableIterable(BY_ID.values());
	}

	public EntityType<?> getType(ItemStack arg) {
		CustomData customdata = arg.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
		return !customdata.isEmpty() ? (EntityType)customdata.read(ENTITY_TYPE_FIELD_CODEC).result().orElse(this.defaultType) : this.defaultType;
	}

	@Override
	public FeatureFlagSet requiredFeatures() {
		return this.defaultType.requiredFeatures();
	}

	public Optional<Mob> spawnOffspringFromSpawnEgg(Player arg, Mob arg2, EntityType<? extends Mob> arg3, ServerLevel arg4, Vec3 arg5, ItemStack arg6) {
		if (!this.spawnsEntity(arg6, arg3)) {
			return Optional.empty();
		} else {
			Mob mob;
			if (arg2 instanceof AgeableMob) {
				mob = ((AgeableMob)arg2).getBreedOffspring(arg4, (AgeableMob)arg2);
			} else {
				mob = (Mob)arg3.create(arg4, EntitySpawnReason.SPAWN_ITEM_USE);
			}

			if (mob == null) {
				return Optional.empty();
			} else {
				mob.setBaby(true);
				if (!mob.isBaby()) {
					return Optional.empty();
				} else {
					mob.moveTo(arg5.x(), arg5.y(), arg5.z(), 0.0F, 0.0F);
					arg4.addFreshEntityWithPassengers(mob);
					mob.setCustomName(arg6.get(DataComponents.CUSTOM_NAME));
					arg6.consume(1, arg);
					return Optional.of(mob);
				}
			}
		}
	}

	@Nullable
	protected DispenseItemBehavior createDispenseBehavior() {
		return DEFAULT_DISPENSE_BEHAVIOR;
	}

	@SubscribeEvent(
		priority = EventPriority.LOWEST
	)
	private static void registerDispenseBehavior(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> eggs().forEach(egg -> {
			if (!DispenserBlock.DISPENSER_REGISTRY.containsKey(egg)) {
				DispenseItemBehavior beh = egg.createDispenseBehavior();
				if (beh != null) {
					DispenserBlock.registerBehavior(egg, beh);
				}
			}
		}));
	}
}
