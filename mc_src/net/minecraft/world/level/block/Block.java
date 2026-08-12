package net.minecraft.world.level.block;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.neoforged.neoforge.registries.GameData;
import org.slf4j.Logger;

public class Block extends BlockBehaviour implements ItemLike, IBlockExtension {
	public static final MapCodec<Block> CODEC = simpleCodec(Block::new);
	private static final Logger LOGGER = LogUtils.getLogger();
	private final Holder.Reference<Block> builtInRegistryHolder = BuiltInRegistries.BLOCK.createIntrusiveHolder(this);
	public static final IdMapper<BlockState> BLOCK_STATE_REGISTRY = GameData.getBlockStateIDMap();
	private static final LoadingCache<VoxelShape, Boolean> SHAPE_FULL_BLOCK_CACHE = CacheBuilder.newBuilder()
		.maximumSize(512L)
		.weakKeys()
		.build(new CacheLoader<VoxelShape, Boolean>() {
			public Boolean load(VoxelShape arg) {
				return !Shapes.joinIsNotEmpty(Shapes.block(), arg, BooleanOp.NOT_SAME);
			}
		});
	public static final int UPDATE_NEIGHBORS = 1;
	public static final int UPDATE_CLIENTS = 2;
	public static final int UPDATE_INVISIBLE = 4;
	public static final int UPDATE_IMMEDIATE = 8;
	public static final int UPDATE_KNOWN_SHAPE = 16;
	public static final int UPDATE_SUPPRESS_DROPS = 32;
	public static final int UPDATE_MOVE_BY_PISTON = 64;
	public static final int UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE = 128;
	public static final int UPDATE_NONE = 4;
	public static final int UPDATE_ALL = 3;
	public static final int UPDATE_ALL_IMMEDIATE = 11;
	public static final float INDESTRUCTIBLE = -1.0F;
	public static final float INSTANT = 0.0F;
	public static final int UPDATE_LIMIT = 512;
	protected final StateDefinition<Block, BlockState> stateDefinition;
	private BlockState defaultBlockState;
	@Nullable
	private Item item;
	private static final int CACHE_SIZE = 256;
	private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.ShapePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
		Object2ByteLinkedOpenHashMap<Block.ShapePairKey> object2bytelinkedopenhashmap = new Object2ByteLinkedOpenHashMap<Block.ShapePairKey>(256, 0.25F) {
			@Override
			protected void rehash(int i) {
			}
		};
		object2bytelinkedopenhashmap.defaultReturnValue((byte)127);
		return object2bytelinkedopenhashmap;
	});
	@Nullable
	private static List<ItemEntity> capturedDrops = null;

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	public static int getId(@Nullable BlockState arg) {
		if (arg == null) {
			return 0;
		} else {
			int i = BLOCK_STATE_REGISTRY.getId(arg);
			return i == -1 ? 0 : i;
		}
	}

	public static BlockState stateById(int i) {
		BlockState blockstate = BLOCK_STATE_REGISTRY.byId(i);
		return blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
	}

	public static Block byItem(@Nullable Item arg) {
		return arg instanceof BlockItem ? ((BlockItem)arg).getBlock() : Blocks.AIR;
	}

	public static BlockState pushEntitiesUp(BlockState arg, BlockState arg2, LevelAccessor arg3, BlockPos arg4) {
		VoxelShape voxelshape = Shapes.joinUnoptimized(arg.getCollisionShape(arg3, arg4), arg2.getCollisionShape(arg3, arg4), BooleanOp.ONLY_SECOND)
			.move(arg4.getX(), arg4.getY(), arg4.getZ());
		if (voxelshape.isEmpty()) {
			return arg2;
		} else {
			for (Entity entity : arg3.getEntities(null, voxelshape.bounds())) {
				double d0 = Shapes.collide(Direction.Axis.Y, entity.getBoundingBox().move(0.0, 1.0, 0.0), List.of(voxelshape), -1.0);
				entity.teleportRelative(0.0, 1.0 + d0, 0.0);
			}

			return arg2;
		}
	}

	public static VoxelShape box(double d, double e, double f, double g, double h, double i) {
		return Shapes.box(d / 16.0, e / 16.0, f / 16.0, g / 16.0, h / 16.0, i / 16.0);
	}

	public static BlockState updateFromNeighbourShapes(BlockState arg, LevelAccessor arg2, BlockPos arg3) {
		BlockState blockstate = arg;
		BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

		for (Direction direction : UPDATE_SHAPE_ORDER) {
			blockpos$mutableblockpos.setWithOffset(arg3, direction);
			blockstate = blockstate.updateShape(arg2, arg2, arg3, direction, blockpos$mutableblockpos, arg2.getBlockState(blockpos$mutableblockpos), arg2.getRandom());
		}

		return blockstate;
	}

	public static void updateOrDestroy(BlockState arg, BlockState arg2, LevelAccessor arg3, BlockPos arg4, int i) {
		updateOrDestroy(arg, arg2, arg3, arg4, i, 512);
	}

	public static void updateOrDestroy(BlockState arg, BlockState arg2, LevelAccessor arg3, BlockPos arg4, int i, int j) {
		if (arg2 != arg) {
			if (arg2.isAir()) {
				if (!arg3.isClientSide()) {
					arg3.destroyBlock(arg4, (i & 32) == 0, null, j);
				}
			} else {
				arg3.setBlock(arg4, arg2, i & -33, j);
			}
		}
	}

	public Block(BlockBehaviour.Properties arg) {
		super(arg);
		StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(this);
		this.createBlockStateDefinition(builder);
		this.stateDefinition = builder.create(Block::defaultBlockState, BlockState::new);
		this.registerDefaultState(this.stateDefinition.any());
		if (SharedConstants.IS_RUNNING_IN_IDE) {
		}
	}

	public static boolean isExceptionForConnection(BlockState arg) {
		return arg.getBlock() instanceof LeavesBlock
			|| arg.is(Blocks.BARRIER)
			|| arg.is(Blocks.CARVED_PUMPKIN)
			|| arg.is(Blocks.JACK_O_LANTERN)
			|| arg.is(Blocks.MELON)
			|| arg.is(Blocks.PUMPKIN)
			|| arg.is(BlockTags.SHULKER_BOXES);
	}

	@Deprecated
	public static boolean shouldRenderFace(BlockState arg, BlockState arg2, Direction arg3) {
		return shouldRenderFace(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, arg, arg2, arg3);
	}

	public static boolean shouldRenderFace(BlockGetter level, BlockPos pos, BlockState arg, BlockState arg2, Direction arg3) {
		VoxelShape voxelshape = arg2.getFaceOcclusionShape(arg3.getOpposite());
		if (voxelshape == Shapes.block()) {
			return false;
		} else if (arg.skipRendering(arg2, arg3)) {
			return false;
		} else if (arg2.hidesNeighborFace(level, pos.relative(arg3), arg, arg3.getOpposite()) && arg.supportsExternalFaceHiding()) {
			return false;
		} else if (voxelshape == Shapes.empty()) {
			return true;
		} else {
			VoxelShape voxelshape1 = arg.getFaceOcclusionShape(arg3);
			if (voxelshape1 == Shapes.empty()) {
				return true;
			} else {
				Block.ShapePairKey block$shapepairkey = new Block.ShapePairKey(voxelshape1, voxelshape);
				Object2ByteLinkedOpenHashMap<Block.ShapePairKey> object2bytelinkedopenhashmap = (Object2ByteLinkedOpenHashMap<Block.ShapePairKey>)OCCLUSION_CACHE.get();
				byte b0 = object2bytelinkedopenhashmap.getAndMoveToFirst(block$shapepairkey);
				if (b0 != 127) {
					return b0 != 0;
				} else {
					boolean flag = Shapes.joinIsNotEmpty(voxelshape1, voxelshape, BooleanOp.ONLY_FIRST);
					if (object2bytelinkedopenhashmap.size() == 256) {
						object2bytelinkedopenhashmap.removeLastByte();
					}

					object2bytelinkedopenhashmap.putAndMoveToFirst(block$shapepairkey, (byte)(flag ? 1 : 0));
					return flag;
				}
			}
		}
	}

	public static boolean canSupportRigidBlock(BlockGetter arg, BlockPos arg2) {
		return arg.getBlockState(arg2).isFaceSturdy(arg, arg2, Direction.UP, SupportType.RIGID);
	}

	public static boolean canSupportCenter(LevelReader arg, BlockPos arg2, Direction arg3) {
		BlockState blockstate = arg.getBlockState(arg2);
		return arg3 == Direction.DOWN && blockstate.is(BlockTags.UNSTABLE_BOTTOM_CENTER) ? false : blockstate.isFaceSturdy(arg, arg2, arg3, SupportType.CENTER);
	}

	public static boolean isFaceFull(VoxelShape arg, Direction arg2) {
		VoxelShape voxelshape = arg.getFaceShape(arg2);
		return isShapeFullBlock(voxelshape);
	}

	public static boolean isShapeFullBlock(VoxelShape arg) {
		return SHAPE_FULL_BLOCK_CACHE.getUnchecked(arg);
	}

	public void animateTick(BlockState arg, Level arg2, BlockPos arg3, RandomSource arg4) {
	}

	public void destroy(LevelAccessor arg, BlockPos arg2, BlockState arg3) {
	}

	public static List<ItemStack> getDrops(BlockState arg, ServerLevel arg2, BlockPos arg3, @Nullable BlockEntity arg4) {
		LootParams.Builder lootparams$builder = new LootParams.Builder(arg2)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(arg3))
			.withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
			.withOptionalParameter(LootContextParams.BLOCK_ENTITY, arg4);
		return arg.getDrops(lootparams$builder);
	}

	public static List<ItemStack> getDrops(BlockState arg, ServerLevel arg2, BlockPos arg3, @Nullable BlockEntity arg4, @Nullable Entity arg5, ItemStack arg6) {
		LootParams.Builder lootparams$builder = new LootParams.Builder(arg2)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(arg3))
			.withParameter(LootContextParams.TOOL, arg6)
			.withOptionalParameter(LootContextParams.THIS_ENTITY, arg5)
			.withOptionalParameter(LootContextParams.BLOCK_ENTITY, arg4);
		return arg.getDrops(lootparams$builder);
	}

	public static void dropResources(BlockState arg, Level arg2, BlockPos arg3) {
		if (arg2 instanceof ServerLevel) {
			beginCapturingDrops();
			getDrops(arg, (ServerLevel)arg2, arg3, null).forEach(arg3x -> popResource(arg2, arg3, arg3x));
			List<ItemEntity> captured = stopCapturingDrops();
			CommonHooks.handleBlockDrops((ServerLevel)arg2, arg3, arg, null, captured, null, ItemStack.EMPTY);
		}
	}

	public static void dropResources(BlockState arg, LevelAccessor arg2, BlockPos arg3, @Nullable BlockEntity arg4) {
		if (arg2 instanceof ServerLevel) {
			beginCapturingDrops();
			getDrops(arg, (ServerLevel)arg2, arg3, arg4).forEach(arg3x -> popResource((ServerLevel)arg2, arg3, arg3x));
			List<ItemEntity> captured = stopCapturingDrops();
			CommonHooks.handleBlockDrops((ServerLevel)arg2, arg3, arg, arg4, captured, null, ItemStack.EMPTY);
		}
	}

	public static void dropResources(BlockState arg, Level arg2, BlockPos arg3, @Nullable BlockEntity arg4, @Nullable Entity arg5, ItemStack arg6) {
		if (arg2 instanceof ServerLevel) {
			beginCapturingDrops();
			getDrops(arg, (ServerLevel)arg2, arg3, arg4, arg5, arg6).forEach(arg3x -> popResource(arg2, arg3, arg3x));
			List<ItemEntity> captured = stopCapturingDrops();
			CommonHooks.handleBlockDrops((ServerLevel)arg2, arg3, arg, arg4, captured, arg5, arg6);
		}
	}

	public static void popResource(Level arg, BlockPos arg2, ItemStack arg3) {
		double d0 = EntityType.ITEM.getHeight() / 2.0;
		double d1 = arg2.getX() + 0.5 + Mth.nextDouble(arg.random, -0.25, 0.25);
		double d2 = arg2.getY() + 0.5 + Mth.nextDouble(arg.random, -0.25, 0.25) - d0;
		double d3 = arg2.getZ() + 0.5 + Mth.nextDouble(arg.random, -0.25, 0.25);
		popResource(arg, () -> new ItemEntity(arg, d1, d2, d3, arg3), arg3);
	}

	public static void popResourceFromFace(Level arg, BlockPos arg2, Direction arg3, ItemStack arg4) {
		int i = arg3.getStepX();
		int j = arg3.getStepY();
		int k = arg3.getStepZ();
		double d0 = EntityType.ITEM.getWidth() / 2.0;
		double d1 = EntityType.ITEM.getHeight() / 2.0;
		double d2 = arg2.getX() + 0.5 + (i == 0 ? Mth.nextDouble(arg.random, -0.25, 0.25) : i * (0.5 + d0));
		double d3 = arg2.getY() + 0.5 + (j == 0 ? Mth.nextDouble(arg.random, -0.25, 0.25) : j * (0.5 + d1)) - d1;
		double d4 = arg2.getZ() + 0.5 + (k == 0 ? Mth.nextDouble(arg.random, -0.25, 0.25) : k * (0.5 + d0));
		double d5 = i == 0 ? Mth.nextDouble(arg.random, -0.1, 0.1) : i * 0.1;
		double d6 = j == 0 ? Mth.nextDouble(arg.random, 0.0, 0.1) : j * 0.1 + 0.1;
		double d7 = k == 0 ? Mth.nextDouble(arg.random, -0.1, 0.1) : k * 0.1;
		popResource(arg, () -> new ItemEntity(arg, d2, d3, d4, arg4, d5, d6, d7), arg4);
	}

	private static void popResource(Level arg, Supplier<ItemEntity> supplier, ItemStack arg2) {
		if (arg instanceof ServerLevel serverLevel
			&& !arg2.isEmpty()
			&& serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
			&& !arg.restoringBlockSnapshots) {
			ItemEntity itementity = (ItemEntity)supplier.get();
			itementity.setDefaultPickUpDelay();
			if (capturedDrops != null) {
				capturedDrops.add(itementity);
			} else {
				arg.addFreshEntity(itementity);
			}
		}
	}

	public void popExperience(ServerLevel arg, BlockPos arg2, int i) {
		if (arg.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !arg.restoringBlockSnapshots) {
			ExperienceOrb.award(arg, Vec3.atCenterOf(arg2), i);
		}
	}

	@Deprecated
	public float getExplosionResistance() {
		return this.explosionResistance;
	}

	public void wasExploded(ServerLevel arg, BlockPos arg2, Explosion arg3) {
	}

	public void stepOn(Level arg, BlockPos arg2, BlockState arg3, Entity arg4) {
	}

	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext arg) {
		return this.defaultBlockState();
	}

	public void playerDestroy(Level arg, Player arg2, BlockPos arg3, BlockState arg4, @Nullable BlockEntity arg5, ItemStack arg6) {
		arg2.awardStat(Stats.BLOCK_MINED.get(this));
		arg2.causeFoodExhaustion(0.005F);
		dropResources(arg4, arg, arg3, arg5, arg2, arg6);
	}

	public void setPlacedBy(Level arg, BlockPos arg2, BlockState arg3, @Nullable LivingEntity arg4, ItemStack arg5) {
	}

	public boolean isPossibleToRespawnInThis(BlockState arg) {
		return !arg.isSolid() && !arg.liquid();
	}

	public MutableComponent getName() {
		return Component.translatable(this.getDescriptionId());
	}

	public void fallOn(Level arg, BlockState arg2, BlockPos arg3, Entity arg4, float f) {
		arg4.causeFallDamage(f, 1.0F, arg4.damageSources().fall());
	}

	public void updateEntityMovementAfterFallOn(BlockGetter arg, Entity arg2) {
		arg2.setDeltaMovement(arg2.getDeltaMovement().multiply(1.0, 0.0, 1.0));
		arg2.setDeltaMovement(arg2.getDeltaMovement().multiply(1.0, 0.0, 1.0));
	}

	@Deprecated
	public ItemStack getCloneItemStack(LevelReader arg, BlockPos arg2, BlockState arg3) {
		return new ItemStack(this);
	}

	public float getFriction() {
		return this.friction;
	}

	public float getSpeedFactor() {
		return this.speedFactor;
	}

	public float getJumpFactor() {
		return this.jumpFactor;
	}

	protected void spawnDestroyParticles(Level arg, Player arg2, BlockPos arg3, BlockState arg4) {
		arg.levelEvent(arg2, 2001, arg3, getId(arg4));
	}

	public BlockState playerWillDestroy(Level arg, BlockPos arg2, BlockState arg3, Player arg4) {
		this.spawnDestroyParticles(arg, arg4, arg2, arg3);
		if (arg3.is(BlockTags.GUARDED_BY_PIGLINS) && arg instanceof ServerLevel serverlevel) {
			PiglinAi.angerNearbyPiglins(serverlevel, arg4, false);
		}

		arg.gameEvent(GameEvent.BLOCK_DESTROY, arg2, GameEvent.Context.of(arg4, arg3));
		return arg3;
	}

	public void handlePrecipitation(BlockState arg, Level arg2, BlockPos arg3, Biome.Precipitation arg4) {
	}

	@Deprecated
	public boolean dropFromExplosion(Explosion arg) {
		return true;
	}

	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> arg) {
	}

	public StateDefinition<Block, BlockState> getStateDefinition() {
		return this.stateDefinition;
	}

	protected final void registerDefaultState(BlockState arg) {
		this.defaultBlockState = arg;
	}

	public final BlockState defaultBlockState() {
		return this.defaultBlockState;
	}

	public final BlockState withPropertiesOf(BlockState arg) {
		BlockState blockstate = this.defaultBlockState();

		for (Property<?> property : arg.getBlock().getStateDefinition().getProperties()) {
			if (blockstate.hasProperty(property)) {
				blockstate = copyProperty(arg, blockstate, property);
			}
		}

		return blockstate;
	}

	private static <T extends Comparable<T>> BlockState copyProperty(BlockState arg, BlockState arg2, Property<T> arg3) {
		return arg2.setValue(arg3, arg.getValue(arg3));
	}

	@Override
	public Item asItem() {
		if (this.item == null) {
			this.item = Item.byBlock(this);
		}

		return this.item;
	}

	public boolean hasDynamicShape() {
		return this.dynamicShape;
	}

	public String toString() {
		return "Block{" + BuiltInRegistries.BLOCK.wrapAsHolder(this).getRegisteredName() + "}";
	}

	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
	}

	@Override
	protected Block asBlock() {
		return this;
	}

	protected ImmutableMap<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> function) {
		return (ImmutableMap<BlockState, VoxelShape>)this.stateDefinition
			.getPossibleStates()
			.stream()
			.collect(ImmutableMap.toImmutableMap(Function.identity(), function));
	}

	private static void beginCapturingDrops() {
		capturedDrops = new ArrayList();
	}

	private static List<ItemEntity> stopCapturingDrops() {
		List<ItemEntity> drops = capturedDrops;
		capturedDrops = null;
		return drops;
	}

	@Deprecated
	public Holder.Reference<Block> builtInRegistryHolder() {
		return this.builtInRegistryHolder;
	}

	protected void tryDropExperience(ServerLevel arg, BlockPos arg2, ItemStack arg3, IntProvider arg4) {
		int i = EnchantmentHelper.processBlockExperience(arg, arg3, arg4.sample(arg.getRandom()));
		if (i > 0) {
			this.popExperience(arg, arg2, i);
		}
	}

	record ShapePairKey(VoxelShape first, VoxelShape second) {
		public boolean equals(Object object) {
			return object instanceof Block.ShapePairKey block$shapepairkey && this.first == block$shapepairkey.first && this.second == block$shapepairkey.second;
		}

		public int hashCode() {
			return System.identityHashCode(this.first) * 31 + System.identityHashCode(this.second);
		}
	}
}
