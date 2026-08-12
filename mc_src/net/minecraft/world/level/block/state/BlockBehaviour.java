package net.minecraft.world.level.block.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

public abstract class BlockBehaviour implements FeatureElement {
	protected static final Direction[] UPDATE_SHAPE_ORDER = new Direction[]{
		Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP
	};
	protected final boolean hasCollision;
	protected final float explosionResistance;
	protected final boolean isRandomlyTicking;
	protected final SoundType soundType;
	protected final float friction;
	protected final float speedFactor;
	protected final float jumpFactor;
	protected final boolean dynamicShape;
	protected final FeatureFlagSet requiredFeatures;
	protected final BlockBehaviour.Properties properties;
	protected final Optional<ResourceKey<LootTable>> drops;
	protected final String descriptionId;

	public BlockBehaviour(BlockBehaviour.Properties arg) {
		this.hasCollision = arg.hasCollision;
		this.drops = arg.effectiveDrops();
		this.descriptionId = arg.effectiveDescriptionId();
		this.explosionResistance = arg.explosionResistance;
		this.isRandomlyTicking = arg.isRandomlyTicking;
		this.soundType = arg.soundType;
		this.friction = arg.friction;
		this.speedFactor = arg.speedFactor;
		this.jumpFactor = arg.jumpFactor;
		this.dynamicShape = arg.dynamicShape;
		this.requiredFeatures = arg.requiredFeatures;
		this.properties = arg;
	}

	public BlockBehaviour.Properties properties() {
		return this.properties;
	}

	protected abstract MapCodec<? extends Block> codec();

	public static <B extends Block> RecordCodecBuilder<B, BlockBehaviour.Properties> propertiesCodec() {
		return BlockBehaviour.Properties.CODEC.fieldOf("properties").forGetter(BlockBehaviour::properties);
	}

	public static <B extends Block> MapCodec<B> simpleCodec(Function<BlockBehaviour.Properties, B> function) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec()).apply(instance, function));
	}

	protected void updateIndirectNeighbourShapes(BlockState arg, LevelAccessor arg2, BlockPos arg3, int i, int j) {
	}

	protected boolean isPathfindable(BlockState arg, PathComputationType arg2) {
		switch (arg2) {
			case LAND:
				return !arg.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
			case WATER:
				return arg.getFluidState().is(FluidTags.WATER);
			case AIR:
				return !arg.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
			default:
				return false;
		}
	}

	protected BlockState updateShape(
		BlockState arg, LevelReader arg2, ScheduledTickAccess arg3, BlockPos arg4, Direction arg5, BlockPos arg6, BlockState arg7, RandomSource arg8
	) {
		return arg;
	}

	protected boolean skipRendering(BlockState arg, BlockState arg2, Direction arg3) {
		return false;
	}

	protected void neighborChanged(BlockState arg, Level arg2, BlockPos arg3, Block arg4, @Nullable Orientation arg5, boolean bl) {
	}

	protected void onPlace(BlockState arg, Level arg2, BlockPos arg3, BlockState arg4, boolean bl) {
	}

	protected void onRemove(BlockState arg, Level arg2, BlockPos arg3, BlockState arg4, boolean bl) {
		if (arg.hasBlockEntity() && !arg.is(arg4.getBlock())) {
			arg2.removeBlockEntity(arg3);
		}
	}

	protected void onExplosionHit(BlockState arg, ServerLevel arg2, BlockPos arg3, Explosion arg4, BiConsumer<ItemStack, BlockPos> biConsumer) {
		if (!arg.isAir() && arg4.getBlockInteraction() != Explosion.BlockInteraction.TRIGGER_BLOCK) {
			Block block = arg.getBlock();
			boolean flag = arg4.getIndirectSourceEntity() instanceof Player;
			if (arg.canDropFromExplosion(arg2, arg3, arg4)) {
				BlockEntity blockentity = arg.hasBlockEntity() ? arg2.getBlockEntity(arg3) : null;
				LootParams.Builder lootparams$builder = new LootParams.Builder(arg2)
					.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(arg3))
					.withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
					.withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockentity)
					.withOptionalParameter(LootContextParams.THIS_ENTITY, arg4.getDirectSourceEntity());
				if (arg4.getBlockInteraction() == Explosion.BlockInteraction.DESTROY_WITH_DECAY) {
					lootparams$builder.withParameter(LootContextParams.EXPLOSION_RADIUS, arg4.radius());
				}

				arg.spawnAfterBreak(arg2, arg3, ItemStack.EMPTY, flag);
				arg.getDrops(lootparams$builder).forEach(arg2x -> biConsumer.accept(arg2x, arg3));
			}

			arg.onBlockExploded(arg2, arg3, arg4);
		}
	}

	protected InteractionResult useWithoutItem(BlockState arg, Level arg2, BlockPos arg3, Player arg4, BlockHitResult arg5) {
		return InteractionResult.PASS;
	}

	protected InteractionResult useItemOn(ItemStack arg, BlockState arg2, Level arg3, BlockPos arg4, Player arg5, InteractionHand arg6, BlockHitResult arg7) {
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	protected boolean triggerEvent(BlockState arg, Level arg2, BlockPos arg3, int i, int j) {
		return false;
	}

	protected RenderShape getRenderShape(BlockState arg) {
		return RenderShape.MODEL;
	}

	protected boolean useShapeForLightOcclusion(BlockState arg) {
		return false;
	}

	protected boolean isSignalSource(BlockState arg) {
		return false;
	}

	protected FluidState getFluidState(BlockState arg) {
		return Fluids.EMPTY.defaultFluidState();
	}

	protected boolean hasAnalogOutputSignal(BlockState arg) {
		return false;
	}

	protected float getMaxHorizontalOffset() {
		return 0.25F;
	}

	protected float getMaxVerticalOffset() {
		return 0.2F;
	}

	@Override
	public FeatureFlagSet requiredFeatures() {
		return this.requiredFeatures;
	}

	protected BlockState rotate(BlockState arg, Rotation arg2) {
		return arg;
	}

	protected BlockState mirror(BlockState arg, Mirror arg2) {
		return arg;
	}

	protected boolean canBeReplaced(BlockState arg, BlockPlaceContext arg2) {
		return arg.canBeReplaced() && (arg2.getItemInHand().isEmpty() || !arg2.getItemInHand().is(this.asItem()));
	}

	protected boolean canBeReplaced(BlockState arg, Fluid arg2) {
		return arg.canBeReplaced() || !arg.isSolid();
	}

	protected List<ItemStack> getDrops(BlockState arg, LootParams.Builder arg2) {
		if (this.drops.isEmpty()) {
			return Collections.emptyList();
		} else {
			LootParams lootparams = arg2.withParameter(LootContextParams.BLOCK_STATE, arg).create(LootContextParamSets.BLOCK);
			ServerLevel serverlevel = lootparams.getLevel();
			LootTable loottable = serverlevel.getServer().reloadableRegistries().getLootTable((ResourceKey<LootTable>)this.drops.get());
			return loottable.getRandomItems(lootparams);
		}
	}

	protected long getSeed(BlockState arg, BlockPos arg2) {
		return Mth.getSeed(arg2);
	}

	protected VoxelShape getOcclusionShape(BlockState arg) {
		return arg.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
	}

	protected VoxelShape getBlockSupportShape(BlockState arg, BlockGetter arg2, BlockPos arg3) {
		return this.getCollisionShape(arg, arg2, arg3, CollisionContext.empty());
	}

	protected VoxelShape getInteractionShape(BlockState arg, BlockGetter arg2, BlockPos arg3) {
		return Shapes.empty();
	}

	protected int getLightBlock(BlockState arg) {
		if (arg.isSolidRender()) {
			return 15;
		} else {
			return arg.propagatesSkylightDown() ? 0 : 1;
		}
	}

	@Nullable
	protected MenuProvider getMenuProvider(BlockState arg, Level arg2, BlockPos arg3) {
		return null;
	}

	protected boolean canSurvive(BlockState arg, LevelReader arg2, BlockPos arg3) {
		return true;
	}

	protected float getShadeBrightness(BlockState arg, BlockGetter arg2, BlockPos arg3) {
		return arg.isCollisionShapeFullBlock(arg2, arg3) ? 0.2F : 1.0F;
	}

	protected int getAnalogOutputSignal(BlockState arg, Level arg2, BlockPos arg3) {
		return 0;
	}

	protected VoxelShape getShape(BlockState arg, BlockGetter arg2, BlockPos arg3, CollisionContext arg4) {
		return Shapes.block();
	}

	protected VoxelShape getCollisionShape(BlockState arg, BlockGetter arg2, BlockPos arg3, CollisionContext arg4) {
		return this.hasCollision ? arg.getShape(arg2, arg3) : Shapes.empty();
	}

	protected boolean isCollisionShapeFullBlock(BlockState arg, BlockGetter arg2, BlockPos arg3) {
		return Block.isShapeFullBlock(arg.getCollisionShape(arg2, arg3));
	}

	protected VoxelShape getVisualShape(BlockState arg, BlockGetter arg2, BlockPos arg3, CollisionContext arg4) {
		return this.getCollisionShape(arg, arg2, arg3, arg4);
	}

	protected void randomTick(BlockState arg, ServerLevel arg2, BlockPos arg3, RandomSource arg4) {
	}

	protected void tick(BlockState arg, ServerLevel arg2, BlockPos arg3, RandomSource arg4) {
	}

	protected float getDestroyProgress(BlockState arg, Player arg2, BlockGetter arg3, BlockPos arg4) {
		float f = arg.getDestroySpeed(arg3, arg4);
		if (f == -1.0F) {
			return 0.0F;
		} else {
			int i = EventHooks.doPlayerHarvestCheck(arg2, arg, arg3, arg4) ? 30 : 100;
			return arg2.getDestroySpeed(arg, arg4) / f / i;
		}
	}

	protected void spawnAfterBreak(BlockState arg, ServerLevel arg2, BlockPos arg3, ItemStack arg4, boolean bl) {
	}

	protected void attack(BlockState arg, Level arg2, BlockPos arg3, Player arg4) {
	}

	protected int getSignal(BlockState arg, BlockGetter arg2, BlockPos arg3, Direction arg4) {
		return 0;
	}

	protected void entityInside(BlockState arg, Level arg2, BlockPos arg3, Entity arg4) {
	}

	protected VoxelShape getEntityInsideCollisionShape(BlockState arg, Level arg2, BlockPos arg3) {
		return Shapes.block();
	}

	protected int getDirectSignal(BlockState arg, BlockGetter arg2, BlockPos arg3, Direction arg4) {
		return 0;
	}

	public final Optional<ResourceKey<LootTable>> getLootTable() {
		return this.drops;
	}

	public final String getDescriptionId() {
		return this.descriptionId;
	}

	protected void onProjectileHit(Level arg, BlockState arg2, BlockHitResult arg3, Projectile arg4) {
	}

	protected boolean propagatesSkylightDown(BlockState arg) {
		return !Block.isShapeFullBlock(arg.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) && arg.getFluidState().isEmpty();
	}

	protected boolean isRandomlyTicking(BlockState arg) {
		return this.isRandomlyTicking;
	}

	@Deprecated
	protected SoundType getSoundType(BlockState arg) {
		return this.soundType;
	}

	public abstract Item asItem();

	protected abstract Block asBlock();

	public MapColor defaultMapColor() {
		return (MapColor)this.properties.mapColor.apply(this.asBlock().defaultBlockState());
	}

	public float defaultDestroyTime() {
		return this.properties.destroyTime;
	}

	protected boolean isAir(BlockState state) {
		return state.isAir;
	}

	public abstract static class BlockStateBase extends StateHolder<Block, BlockState> {
		private static final Direction[] DIRECTIONS = Direction.values();
		private static final VoxelShape[] EMPTY_OCCLUSION_SHAPES = Util.make(new VoxelShape[DIRECTIONS.length], args -> Arrays.fill(args, Shapes.empty()));
		private static final VoxelShape[] FULL_BLOCK_OCCLUSION_SHAPES = Util.make(new VoxelShape[DIRECTIONS.length], args -> Arrays.fill(args, Shapes.block()));
		private final int lightEmission;
		private final boolean useShapeForLightOcclusion;
		private final boolean isAir;
		private final boolean ignitedByLava;
		@Deprecated
		private final boolean liquid;
		@Deprecated
		private boolean legacySolid;
		private final PushReaction pushReaction;
		private final MapColor mapColor;
		private final float destroySpeed;
		private final boolean requiresCorrectToolForDrops;
		private final boolean canOcclude;
		private final BlockBehaviour.StatePredicate isRedstoneConductor;
		private final BlockBehaviour.StatePredicate isSuffocating;
		private final BlockBehaviour.StatePredicate isViewBlocking;
		private final BlockBehaviour.StatePredicate hasPostProcess;
		private final BlockBehaviour.StatePredicate emissiveRendering;
		@Nullable
		private final BlockBehaviour.OffsetFunction offsetFunction;
		private final boolean spawnTerrainParticles;
		private final NoteBlockInstrument instrument;
		private final boolean replaceable;
		@Nullable
		private BlockBehaviour.BlockStateBase.Cache cache;
		private FluidState fluidState = Fluids.EMPTY.defaultFluidState();
		private boolean isRandomlyTicking;
		private boolean solidRender;
		private VoxelShape occlusionShape;
		private VoxelShape[] occlusionShapesByFace;
		private boolean propagatesSkylightDown;
		private int lightBlock;

		protected BlockStateBase(Block arg, Reference2ObjectArrayMap<Property<?>, Comparable<?>> reference2ObjectArrayMap, MapCodec<BlockState> mapCodec) {
			super(arg, reference2ObjectArrayMap, mapCodec);
			BlockBehaviour.Properties blockbehaviour$properties = arg.properties;
			this.lightEmission = blockbehaviour$properties.lightEmission.applyAsInt(this.asState());
			this.useShapeForLightOcclusion = arg.useShapeForLightOcclusion(this.asState());
			this.isAir = blockbehaviour$properties.isAir;
			this.ignitedByLava = blockbehaviour$properties.ignitedByLava;
			this.liquid = blockbehaviour$properties.liquid;
			this.pushReaction = blockbehaviour$properties.pushReaction;
			this.mapColor = (MapColor)blockbehaviour$properties.mapColor.apply(this.asState());
			this.destroySpeed = blockbehaviour$properties.destroyTime;
			this.requiresCorrectToolForDrops = blockbehaviour$properties.requiresCorrectToolForDrops;
			this.canOcclude = blockbehaviour$properties.canOcclude;
			this.isRedstoneConductor = blockbehaviour$properties.isRedstoneConductor;
			this.isSuffocating = blockbehaviour$properties.isSuffocating;
			this.isViewBlocking = blockbehaviour$properties.isViewBlocking;
			this.hasPostProcess = blockbehaviour$properties.hasPostProcess;
			this.emissiveRendering = blockbehaviour$properties.emissiveRendering;
			this.offsetFunction = blockbehaviour$properties.offsetFunction;
			this.spawnTerrainParticles = blockbehaviour$properties.spawnTerrainParticles;
			this.instrument = blockbehaviour$properties.instrument;
			this.replaceable = blockbehaviour$properties.replaceable;
		}

		private boolean calculateSolid() {
			if (this.owner.properties.forceSolidOn) {
				return true;
			} else if (this.owner.properties.forceSolidOff) {
				return false;
			} else if (this.cache == null) {
				return false;
			} else {
				VoxelShape voxelshape = this.cache.collisionShape;
				if (voxelshape.isEmpty()) {
					return false;
				} else {
					AABB aabb = voxelshape.bounds();
					return aabb.getSize() >= 0.7291666666666666 ? true : aabb.getYsize() >= 1.0;
				}
			}
		}

		public void initCache() {
			this.fluidState = this.owner.getFluidState(this.asState());
			this.isRandomlyTicking = this.owner.isRandomlyTicking(this.asState());
			if (!this.getBlock().hasDynamicShape()) {
				this.cache = new BlockBehaviour.BlockStateBase.Cache(this.asState());
			}

			this.legacySolid = this.calculateSolid();
			this.occlusionShape = this.canOcclude ? this.owner.getOcclusionShape(this.asState()) : Shapes.empty();
			this.solidRender = Block.isShapeFullBlock(this.occlusionShape);
			if (this.occlusionShape.isEmpty()) {
				this.occlusionShapesByFace = EMPTY_OCCLUSION_SHAPES;
			} else if (this.solidRender) {
				this.occlusionShapesByFace = FULL_BLOCK_OCCLUSION_SHAPES;
			} else {
				this.occlusionShapesByFace = new VoxelShape[DIRECTIONS.length];

				for (Direction direction : DIRECTIONS) {
					this.occlusionShapesByFace[direction.ordinal()] = this.occlusionShape.getFaceShape(direction);
				}
			}

			this.propagatesSkylightDown = this.owner.propagatesSkylightDown(this.asState());
			this.lightBlock = this.owner.getLightBlock(this.asState());
		}

		public Block getBlock() {
			return this.owner;
		}

		public Holder<Block> getBlockHolder() {
			return this.owner.builtInRegistryHolder();
		}

		@Deprecated
		public boolean blocksMotion() {
			Block block = this.getBlock();
			return block != Blocks.COBWEB && block != Blocks.BAMBOO_SAPLING && this.isSolid();
		}

		@Deprecated
		public boolean isSolid() {
			return this.legacySolid;
		}

		public boolean isValidSpawn(BlockGetter arg, BlockPos arg2, EntityType<?> arg3) {
			return this.getBlock().properties.isValidSpawn.test(this.asState(), arg, arg2, arg3);
		}

		public boolean propagatesSkylightDown() {
			return this.propagatesSkylightDown;
		}

		public int getLightBlock() {
			return this.lightBlock;
		}

		public VoxelShape getFaceOcclusionShape(Direction arg) {
			return this.occlusionShapesByFace[arg.ordinal()];
		}

		public VoxelShape getOcclusionShape() {
			return this.occlusionShape;
		}

		public boolean hasLargeCollisionShape() {
			return this.cache == null || this.cache.largeCollisionShape;
		}

		public boolean useShapeForLightOcclusion() {
			return this.useShapeForLightOcclusion;
		}

		@Deprecated
		public int getLightEmission() {
			return this.lightEmission;
		}

		public boolean isAir() {
			return this.getBlock().isAir((BlockState)this);
		}

		public boolean ignitedByLava() {
			return this.ignitedByLava;
		}

		@Deprecated
		public boolean liquid() {
			return this.liquid;
		}

		public MapColor getMapColor(BlockGetter arg, BlockPos arg2) {
			return this.getBlock().getMapColor(this.asState(), arg, arg2, this.mapColor);
		}

		@Deprecated
		public BlockState rotate(Rotation arg) {
			return this.getBlock().rotate(this.asState(), arg);
		}

		public BlockState mirror(Mirror arg) {
			return this.getBlock().mirror(this.asState(), arg);
		}

		public RenderShape getRenderShape() {
			return this.getBlock().getRenderShape(this.asState());
		}

		public boolean emissiveRendering(BlockGetter arg, BlockPos arg2) {
			return this.emissiveRendering.test(this.asState(), arg, arg2);
		}

		public float getShadeBrightness(BlockGetter arg, BlockPos arg2) {
			return this.getBlock().getShadeBrightness(this.asState(), arg, arg2);
		}

		public boolean isRedstoneConductor(BlockGetter arg, BlockPos arg2) {
			return this.isRedstoneConductor.test(this.asState(), arg, arg2);
		}

		public boolean isSignalSource() {
			return this.getBlock().isSignalSource(this.asState());
		}

		public int getSignal(BlockGetter arg, BlockPos arg2, Direction arg3) {
			return this.getBlock().getSignal(this.asState(), arg, arg2, arg3);
		}

		public boolean hasAnalogOutputSignal() {
			return this.getBlock().hasAnalogOutputSignal(this.asState());
		}

		public int getAnalogOutputSignal(Level arg, BlockPos arg2) {
			return this.getBlock().getAnalogOutputSignal(this.asState(), arg, arg2);
		}

		public float getDestroySpeed(BlockGetter arg, BlockPos arg2) {
			return this.destroySpeed;
		}

		public float getDestroyProgress(Player arg, BlockGetter arg2, BlockPos arg3) {
			return this.getBlock().getDestroyProgress(this.asState(), arg, arg2, arg3);
		}

		public int getDirectSignal(BlockGetter arg, BlockPos arg2, Direction arg3) {
			return this.getBlock().getDirectSignal(this.asState(), arg, arg2, arg3);
		}

		public PushReaction getPistonPushReaction() {
			PushReaction reaction = this.getBlock().getPistonPushReaction(this.asState());
			return reaction != null ? reaction : this.pushReaction;
		}

		public boolean isSolidRender() {
			return this.solidRender;
		}

		public boolean canOcclude() {
			return this.canOcclude;
		}

		public boolean skipRendering(BlockState arg, Direction arg2) {
			return this.getBlock().skipRendering(this.asState(), arg, arg2);
		}

		public VoxelShape getShape(BlockGetter arg, BlockPos arg2) {
			return this.getShape(arg, arg2, CollisionContext.empty());
		}

		public VoxelShape getShape(BlockGetter arg, BlockPos arg2, CollisionContext arg3) {
			return this.getBlock().getShape(this.asState(), arg, arg2, arg3);
		}

		public VoxelShape getCollisionShape(BlockGetter arg, BlockPos arg2) {
			return this.cache != null ? this.cache.collisionShape : this.getCollisionShape(arg, arg2, CollisionContext.empty());
		}

		public VoxelShape getCollisionShape(BlockGetter arg, BlockPos arg2, CollisionContext arg3) {
			return this.getBlock().getCollisionShape(this.asState(), arg, arg2, arg3);
		}

		public VoxelShape getBlockSupportShape(BlockGetter arg, BlockPos arg2) {
			return this.getBlock().getBlockSupportShape(this.asState(), arg, arg2);
		}

		public VoxelShape getVisualShape(BlockGetter arg, BlockPos arg2, CollisionContext arg3) {
			return this.getBlock().getVisualShape(this.asState(), arg, arg2, arg3);
		}

		public VoxelShape getInteractionShape(BlockGetter arg, BlockPos arg2) {
			return this.getBlock().getInteractionShape(this.asState(), arg, arg2);
		}

		public final boolean entityCanStandOn(BlockGetter arg, BlockPos arg2, Entity arg3) {
			return this.entityCanStandOnFace(arg, arg2, arg3, Direction.UP);
		}

		public final boolean entityCanStandOnFace(BlockGetter arg, BlockPos arg2, Entity arg3, Direction arg4) {
			return Block.isFaceFull(this.getCollisionShape(arg, arg2, CollisionContext.of(arg3)), arg4);
		}

		public Vec3 getOffset(BlockPos arg) {
			BlockBehaviour.OffsetFunction blockbehaviour$offsetfunction = this.offsetFunction;
			return blockbehaviour$offsetfunction != null ? blockbehaviour$offsetfunction.evaluate(this.asState(), arg) : Vec3.ZERO;
		}

		public boolean hasOffsetFunction() {
			return this.offsetFunction != null;
		}

		public boolean triggerEvent(Level arg, BlockPos arg2, int i, int j) {
			return this.getBlock().triggerEvent(this.asState(), arg, arg2, i, j);
		}

		public void handleNeighborChanged(Level arg, BlockPos arg2, Block arg3, @Nullable Orientation arg4, boolean bl) {
			DebugPackets.sendNeighborsUpdatePacket(arg, arg2);
			this.getBlock().neighborChanged(this.asState(), arg, arg2, arg3, arg4, bl);
		}

		public final void updateNeighbourShapes(LevelAccessor arg, BlockPos arg2, int i) {
			this.updateNeighbourShapes(arg, arg2, i, 512);
		}

		public final void updateNeighbourShapes(LevelAccessor arg, BlockPos arg2, int i, int j) {
			BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

			for (Direction direction : BlockBehaviour.UPDATE_SHAPE_ORDER) {
				blockpos$mutableblockpos.setWithOffset(arg2, direction);
				arg.neighborShapeChanged(direction.getOpposite(), blockpos$mutableblockpos, arg2, this.asState(), i, j);
			}
		}

		public final void updateIndirectNeighbourShapes(LevelAccessor arg, BlockPos arg2, int i) {
			this.updateIndirectNeighbourShapes(arg, arg2, i, 512);
		}

		public void updateIndirectNeighbourShapes(LevelAccessor arg, BlockPos arg2, int i, int j) {
			this.getBlock().updateIndirectNeighbourShapes(this.asState(), arg, arg2, i, j);
		}

		public void onPlace(Level arg, BlockPos arg2, BlockState arg3, boolean bl) {
			this.getBlock().onPlace(this.asState(), arg, arg2, arg3, bl);
		}

		public void onRemove(Level arg, BlockPos arg2, BlockState arg3, boolean bl) {
			this.getBlock().onRemove(this.asState(), arg, arg2, arg3, bl);
		}

		public void onExplosionHit(ServerLevel arg, BlockPos arg2, Explosion arg3, BiConsumer<ItemStack, BlockPos> biConsumer) {
			this.getBlock().onExplosionHit(this.asState(), arg, arg2, arg3, biConsumer);
		}

		public void tick(ServerLevel arg, BlockPos arg2, RandomSource arg3) {
			this.getBlock().tick(this.asState(), arg, arg2, arg3);
		}

		public void randomTick(ServerLevel arg, BlockPos arg2, RandomSource arg3) {
			this.getBlock().randomTick(this.asState(), arg, arg2, arg3);
		}

		public void entityInside(Level arg, BlockPos arg2, Entity arg3) {
			this.getBlock().entityInside(this.asState(), arg, arg2, arg3);
		}

		public VoxelShape getEntityInsideCollisionShape(Level arg, BlockPos arg2) {
			return this.getBlock().getEntityInsideCollisionShape(this.asState(), arg, arg2);
		}

		public void spawnAfterBreak(ServerLevel arg, BlockPos arg2, ItemStack arg3, boolean bl) {
			this.getBlock().spawnAfterBreak(this.asState(), arg, arg2, arg3, bl);
		}

		public List<ItemStack> getDrops(LootParams.Builder arg) {
			return this.getBlock().getDrops(this.asState(), arg);
		}

		public InteractionResult useItemOn(ItemStack arg, Level arg2, Player arg3, InteractionHand arg4, BlockHitResult arg5) {
			UseOnContext useOnContext = new UseOnContext(arg2, arg3, arg4, arg3.getItemInHand(arg4).copy(), arg5);
			UseItemOnBlockEvent e = NeoForge.EVENT_BUS.post(new UseItemOnBlockEvent(useOnContext, UseItemOnBlockEvent.UsePhase.BLOCK));
			return e.isCanceled() ? e.getCancellationResult() : this.getBlock().useItemOn(arg, this.asState(), arg2, arg5.getBlockPos(), arg3, arg4, arg5);
		}

		public InteractionResult useWithoutItem(Level arg, Player arg2, BlockHitResult arg3) {
			return this.getBlock().useWithoutItem(this.asState(), arg, arg3.getBlockPos(), arg2, arg3);
		}

		public void attack(Level arg, BlockPos arg2, Player arg3) {
			this.getBlock().attack(this.asState(), arg, arg2, arg3);
		}

		public boolean isSuffocating(BlockGetter arg, BlockPos arg2) {
			return this.isSuffocating.test(this.asState(), arg, arg2);
		}

		public boolean isViewBlocking(BlockGetter arg, BlockPos arg2) {
			return this.isViewBlocking.test(this.asState(), arg, arg2);
		}

		public BlockState updateShape(LevelReader arg, ScheduledTickAccess arg2, BlockPos arg3, Direction arg4, BlockPos arg5, BlockState arg6, RandomSource arg7) {
			return this.getBlock().updateShape(this.asState(), arg, arg2, arg3, arg4, arg5, arg6, arg7);
		}

		public boolean isPathfindable(PathComputationType arg) {
			return this.getBlock().isPathfindable(this.asState(), arg);
		}

		public boolean canBeReplaced(BlockPlaceContext arg) {
			return this.getBlock().canBeReplaced(this.asState(), arg);
		}

		public boolean canBeReplaced(Fluid arg) {
			return this.getBlock().canBeReplaced(this.asState(), arg);
		}

		public boolean canBeReplaced() {
			return this.replaceable;
		}

		public boolean canSurvive(LevelReader arg, BlockPos arg2) {
			return this.getBlock().canSurvive(this.asState(), arg, arg2);
		}

		public boolean hasPostProcess(BlockGetter arg, BlockPos arg2) {
			return this.hasPostProcess.test(this.asState(), arg, arg2);
		}

		@Nullable
		public MenuProvider getMenuProvider(Level arg, BlockPos arg2) {
			return this.getBlock().getMenuProvider(this.asState(), arg, arg2);
		}

		public boolean is(TagKey<Block> arg) {
			return this.getBlock().builtInRegistryHolder().is(arg);
		}

		public boolean is(TagKey<Block> arg, Predicate<BlockBehaviour.BlockStateBase> predicate) {
			return this.is(arg) && predicate.test(this);
		}

		public boolean is(HolderSet<Block> arg) {
			return arg.contains(this.getBlock().builtInRegistryHolder());
		}

		public boolean is(Holder<Block> arg) {
			return this.is((Block)arg.value());
		}

		public Stream<TagKey<Block>> getTags() {
			return this.getBlock().builtInRegistryHolder().tags();
		}

		public boolean hasBlockEntity() {
			return this.getBlock() instanceof EntityBlock;
		}

		@Nullable
		public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level arg, BlockEntityType<T> arg2) {
			return this.getBlock() instanceof EntityBlock ? ((EntityBlock)this.getBlock()).getTicker(arg, this.asState(), arg2) : null;
		}

		public boolean is(Block arg) {
			return this.getBlock() == arg;
		}

		public boolean is(ResourceKey<Block> arg) {
			return this.getBlock().builtInRegistryHolder().is(arg);
		}

		public FluidState getFluidState() {
			return this.fluidState;
		}

		public boolean isRandomlyTicking() {
			return this.isRandomlyTicking;
		}

		public long getSeed(BlockPos arg) {
			return this.getBlock().getSeed(this.asState(), arg);
		}

		@Deprecated
		public SoundType getSoundType() {
			return this.getBlock().getSoundType(this.asState());
		}

		public void onProjectileHit(Level arg, BlockState arg2, BlockHitResult arg3, Projectile arg4) {
			this.getBlock().onProjectileHit(arg, arg2, arg3, arg4);
		}

		public boolean isFaceSturdy(BlockGetter arg, BlockPos arg2, Direction arg3) {
			return this.isFaceSturdy(arg, arg2, arg3, SupportType.FULL);
		}

		public boolean isFaceSturdy(BlockGetter arg, BlockPos arg2, Direction arg3, SupportType arg4) {
			return this.cache != null ? this.cache.isFaceSturdy(arg3, arg4) : arg4.isSupporting(this.asState(), arg, arg2, arg3);
		}

		public boolean isCollisionShapeFullBlock(BlockGetter arg, BlockPos arg2) {
			return this.cache != null ? this.cache.isCollisionShapeFullBlock : this.getBlock().isCollisionShapeFullBlock(this.asState(), arg, arg2);
		}

		protected abstract BlockState asState();

		public boolean requiresCorrectToolForDrops() {
			return this.requiresCorrectToolForDrops;
		}

		public boolean shouldSpawnTerrainParticles() {
			return this.spawnTerrainParticles;
		}

		public NoteBlockInstrument instrument() {
			return this.instrument;
		}

		static final class Cache {
			private static final Direction[] DIRECTIONS = Direction.values();
			private static final int SUPPORT_TYPE_COUNT = SupportType.values().length;
			protected final VoxelShape collisionShape;
			protected final boolean largeCollisionShape;
			private final boolean[] faceSturdy;
			protected final boolean isCollisionShapeFullBlock;

			Cache(BlockState arg) {
				Block block = arg.getBlock();
				this.collisionShape = block.getCollisionShape(arg, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
				if (!this.collisionShape.isEmpty() && arg.hasOffsetFunction()) {
					throw new IllegalStateException(
						String.format(
							Locale.ROOT, "%s has a collision shape and an offset type, but is not marked as dynamicShape in its properties.", BuiltInRegistries.BLOCK.getKey(block)
						)
					);
				} else {
					this.largeCollisionShape = Arrays.stream(Direction.Axis.values())
						.anyMatch(argx -> this.collisionShape.min(argx) < 0.0 || this.collisionShape.max(argx) > 1.0);
					this.faceSturdy = new boolean[DIRECTIONS.length * SUPPORT_TYPE_COUNT];

					for (Direction direction : DIRECTIONS) {
						for (SupportType supporttype : SupportType.values()) {
							this.faceSturdy[getFaceSupportIndex(direction, supporttype)] = supporttype.isSupporting(arg, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction);
						}
					}

					this.isCollisionShapeFullBlock = Block.isShapeFullBlock(arg.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
				}
			}

			public boolean isFaceSturdy(Direction arg, SupportType arg2) {
				return this.faceSturdy[getFaceSupportIndex(arg, arg2)];
			}

			private static int getFaceSupportIndex(Direction arg, SupportType arg2) {
				return arg.ordinal() * SUPPORT_TYPE_COUNT + arg2.ordinal();
			}
		}
	}

	@FunctionalInterface
	public interface OffsetFunction {
		Vec3 evaluate(BlockState arg, BlockPos arg2);
	}

	public static enum OffsetType {
		NONE,
		XZ,
		XYZ;
	}

	public static class Properties {
		public static final Codec<BlockBehaviour.Properties> CODEC = Codec.unit((Supplier<BlockBehaviour.Properties>)(() -> of()));
		Function<BlockState, MapColor> mapColor = arg -> MapColor.NONE;
		boolean hasCollision = true;
		SoundType soundType = SoundType.STONE;
		ToIntFunction<BlockState> lightEmission = arg -> 0;
		float explosionResistance;
		float destroyTime;
		boolean requiresCorrectToolForDrops;
		boolean isRandomlyTicking;
		float friction = 0.6F;
		float speedFactor = 1.0F;
		float jumpFactor = 1.0F;
		@Nullable
		private ResourceKey<Block> id;
		private DependantName<Block, Optional<ResourceKey<LootTable>>> drops = arg -> Optional.of(
			ResourceKey.create(Registries.LOOT_TABLE, arg.location().withPrefix("blocks/"))
		);
		private DependantName<Block, String> descriptionId = arg -> Util.makeDescriptionId("block", arg.location());
		boolean canOcclude = true;
		boolean isAir;
		boolean ignitedByLava;
		@Deprecated
		boolean liquid;
		@Deprecated
		boolean forceSolidOff;
		boolean forceSolidOn;
		PushReaction pushReaction = PushReaction.NORMAL;
		boolean spawnTerrainParticles = true;
		NoteBlockInstrument instrument = NoteBlockInstrument.HARP;
		boolean replaceable;
		BlockBehaviour.StateArgumentPredicate<EntityType<?>> isValidSpawn = (arg, arg2, arg3, arg4) -> arg.isFaceSturdy(arg2, arg3, Direction.UP)
			&& arg.getLightEmission(arg2, arg3) < 14;
		BlockBehaviour.StatePredicate isRedstoneConductor = (arg, arg2, arg3) -> arg.isCollisionShapeFullBlock(arg2, arg3);
		BlockBehaviour.StatePredicate isSuffocating = (arg, arg2, arg3) -> arg.blocksMotion() && arg.isCollisionShapeFullBlock(arg2, arg3);
		BlockBehaviour.StatePredicate isViewBlocking = this.isSuffocating;
		BlockBehaviour.StatePredicate hasPostProcess = (arg, arg2, arg3) -> false;
		BlockBehaviour.StatePredicate emissiveRendering = (arg, arg2, arg3) -> false;
		boolean dynamicShape;
		FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
		@Nullable
		BlockBehaviour.OffsetFunction offsetFunction;

		private Properties() {
		}

		public static BlockBehaviour.Properties of() {
			return new BlockBehaviour.Properties();
		}

		public static BlockBehaviour.Properties ofFullCopy(BlockBehaviour arg) {
			BlockBehaviour.Properties blockbehaviour$properties = ofLegacyCopy(arg);
			BlockBehaviour.Properties blockbehaviour$properties1 = arg.properties;
			blockbehaviour$properties.jumpFactor = blockbehaviour$properties1.jumpFactor;
			blockbehaviour$properties.isRedstoneConductor = blockbehaviour$properties1.isRedstoneConductor;
			blockbehaviour$properties.isValidSpawn = blockbehaviour$properties1.isValidSpawn;
			blockbehaviour$properties.hasPostProcess = blockbehaviour$properties1.hasPostProcess;
			blockbehaviour$properties.isSuffocating = blockbehaviour$properties1.isSuffocating;
			blockbehaviour$properties.isViewBlocking = blockbehaviour$properties1.isViewBlocking;
			blockbehaviour$properties.drops = blockbehaviour$properties1.drops;
			blockbehaviour$properties.descriptionId = blockbehaviour$properties1.descriptionId;
			return blockbehaviour$properties;
		}

		@Deprecated
		public static BlockBehaviour.Properties ofLegacyCopy(BlockBehaviour arg) {
			BlockBehaviour.Properties blockbehaviour$properties = new BlockBehaviour.Properties();
			BlockBehaviour.Properties blockbehaviour$properties1 = arg.properties;
			blockbehaviour$properties.destroyTime = blockbehaviour$properties1.destroyTime;
			blockbehaviour$properties.explosionResistance = blockbehaviour$properties1.explosionResistance;
			blockbehaviour$properties.hasCollision = blockbehaviour$properties1.hasCollision;
			blockbehaviour$properties.isRandomlyTicking = blockbehaviour$properties1.isRandomlyTicking;
			blockbehaviour$properties.lightEmission = blockbehaviour$properties1.lightEmission;
			blockbehaviour$properties.mapColor = blockbehaviour$properties1.mapColor;
			blockbehaviour$properties.soundType = blockbehaviour$properties1.soundType;
			blockbehaviour$properties.friction = blockbehaviour$properties1.friction;
			blockbehaviour$properties.speedFactor = blockbehaviour$properties1.speedFactor;
			blockbehaviour$properties.dynamicShape = blockbehaviour$properties1.dynamicShape;
			blockbehaviour$properties.canOcclude = blockbehaviour$properties1.canOcclude;
			blockbehaviour$properties.isAir = blockbehaviour$properties1.isAir;
			blockbehaviour$properties.ignitedByLava = blockbehaviour$properties1.ignitedByLava;
			blockbehaviour$properties.liquid = blockbehaviour$properties1.liquid;
			blockbehaviour$properties.forceSolidOff = blockbehaviour$properties1.forceSolidOff;
			blockbehaviour$properties.forceSolidOn = blockbehaviour$properties1.forceSolidOn;
			blockbehaviour$properties.pushReaction = blockbehaviour$properties1.pushReaction;
			blockbehaviour$properties.requiresCorrectToolForDrops = blockbehaviour$properties1.requiresCorrectToolForDrops;
			blockbehaviour$properties.offsetFunction = blockbehaviour$properties1.offsetFunction;
			blockbehaviour$properties.spawnTerrainParticles = blockbehaviour$properties1.spawnTerrainParticles;
			blockbehaviour$properties.requiredFeatures = blockbehaviour$properties1.requiredFeatures;
			blockbehaviour$properties.emissiveRendering = blockbehaviour$properties1.emissiveRendering;
			blockbehaviour$properties.instrument = blockbehaviour$properties1.instrument;
			blockbehaviour$properties.replaceable = blockbehaviour$properties1.replaceable;
			return blockbehaviour$properties;
		}

		public BlockBehaviour.Properties mapColor(DyeColor arg) {
			this.mapColor = arg2 -> arg.getMapColor();
			return this;
		}

		public BlockBehaviour.Properties mapColor(MapColor arg) {
			this.mapColor = arg2 -> arg;
			return this;
		}

		public BlockBehaviour.Properties mapColor(Function<BlockState, MapColor> function) {
			this.mapColor = function;
			return this;
		}

		public BlockBehaviour.Properties noCollission() {
			this.hasCollision = false;
			this.canOcclude = false;
			return this;
		}

		public BlockBehaviour.Properties noOcclusion() {
			this.canOcclude = false;
			return this;
		}

		public BlockBehaviour.Properties friction(float f) {
			this.friction = f;
			return this;
		}

		public BlockBehaviour.Properties speedFactor(float f) {
			this.speedFactor = f;
			return this;
		}

		public BlockBehaviour.Properties jumpFactor(float f) {
			this.jumpFactor = f;
			return this;
		}

		public BlockBehaviour.Properties sound(SoundType arg) {
			this.soundType = arg;
			return this;
		}

		public BlockBehaviour.Properties lightLevel(ToIntFunction<BlockState> toIntFunction) {
			this.lightEmission = toIntFunction;
			return this;
		}

		public BlockBehaviour.Properties strength(float f, float g) {
			return this.destroyTime(f).explosionResistance(g);
		}

		public BlockBehaviour.Properties instabreak() {
			return this.strength(0.0F);
		}

		public BlockBehaviour.Properties strength(float f) {
			this.strength(f, f);
			return this;
		}

		public BlockBehaviour.Properties randomTicks() {
			this.isRandomlyTicking = true;
			return this;
		}

		public BlockBehaviour.Properties dynamicShape() {
			this.dynamicShape = true;
			return this;
		}

		public BlockBehaviour.Properties noLootTable() {
			this.drops = DependantName.fixed(Optional.empty());
			return this;
		}

		public BlockBehaviour.Properties overrideLootTable(Optional<ResourceKey<LootTable>> optional) {
			this.drops = DependantName.fixed(optional);
			return this;
		}

		protected Optional<ResourceKey<LootTable>> effectiveDrops() {
			return this.drops.get((ResourceKey<Block>)Objects.requireNonNull(this.id, "Block id not set"));
		}

		public BlockBehaviour.Properties ignitedByLava() {
			this.ignitedByLava = true;
			return this;
		}

		public BlockBehaviour.Properties liquid() {
			this.liquid = true;
			return this;
		}

		public BlockBehaviour.Properties forceSolidOn() {
			this.forceSolidOn = true;
			return this;
		}

		@Deprecated
		public BlockBehaviour.Properties forceSolidOff() {
			this.forceSolidOff = true;
			return this;
		}

		public BlockBehaviour.Properties pushReaction(PushReaction arg) {
			this.pushReaction = arg;
			return this;
		}

		public BlockBehaviour.Properties air() {
			this.isAir = true;
			return this;
		}

		public BlockBehaviour.Properties isValidSpawn(BlockBehaviour.StateArgumentPredicate<EntityType<?>> arg) {
			this.isValidSpawn = arg;
			return this;
		}

		public BlockBehaviour.Properties isRedstoneConductor(BlockBehaviour.StatePredicate arg) {
			this.isRedstoneConductor = arg;
			return this;
		}

		public BlockBehaviour.Properties isSuffocating(BlockBehaviour.StatePredicate arg) {
			this.isSuffocating = arg;
			return this;
		}

		public BlockBehaviour.Properties isViewBlocking(BlockBehaviour.StatePredicate arg) {
			this.isViewBlocking = arg;
			return this;
		}

		public BlockBehaviour.Properties hasPostProcess(BlockBehaviour.StatePredicate arg) {
			this.hasPostProcess = arg;
			return this;
		}

		public BlockBehaviour.Properties emissiveRendering(BlockBehaviour.StatePredicate arg) {
			this.emissiveRendering = arg;
			return this;
		}

		public BlockBehaviour.Properties requiresCorrectToolForDrops() {
			this.requiresCorrectToolForDrops = true;
			return this;
		}

		public BlockBehaviour.Properties destroyTime(float f) {
			this.destroyTime = f;
			return this;
		}

		public BlockBehaviour.Properties explosionResistance(float f) {
			this.explosionResistance = Math.max(0.0F, f);
			return this;
		}

		public BlockBehaviour.Properties offsetType(BlockBehaviour.OffsetType arg) {
			this.offsetFunction = switch (arg) {
				case NONE -> null;
				case XZ -> (argx, arg2) -> {
					Block block = argx.getBlock();
					long i = Mth.getSeed(arg2.getX(), 0, arg2.getZ());
					float f = block.getMaxHorizontalOffset();
					double d0 = Mth.clamp(((float)(i & 15L) / 15.0F - 0.5) * 0.5, (double)(-f), (double)f);
					double d1 = Mth.clamp(((float)(i >> 8 & 15L) / 15.0F - 0.5) * 0.5, (double)(-f), (double)f);
					return new Vec3(d0, 0.0, d1);
				};
				case XYZ -> (argx, arg2) -> {
					Block block = argx.getBlock();
					long i = Mth.getSeed(arg2.getX(), 0, arg2.getZ());
					double d0 = ((float)(i >> 4 & 15L) / 15.0F - 1.0) * block.getMaxVerticalOffset();
					float f = block.getMaxHorizontalOffset();
					double d1 = Mth.clamp(((float)(i & 15L) / 15.0F - 0.5) * 0.5, (double)(-f), (double)f);
					double d2 = Mth.clamp(((float)(i >> 8 & 15L) / 15.0F - 0.5) * 0.5, (double)(-f), (double)f);
					return new Vec3(d1, d0, d2);
				};
			};
			return this;
		}

		public BlockBehaviour.Properties noTerrainParticles() {
			this.spawnTerrainParticles = false;
			return this;
		}

		public BlockBehaviour.Properties requiredFeatures(FeatureFlag... args) {
			this.requiredFeatures = FeatureFlags.REGISTRY.subset(args);
			return this;
		}

		public BlockBehaviour.Properties instrument(NoteBlockInstrument arg) {
			this.instrument = arg;
			return this;
		}

		public BlockBehaviour.Properties replaceable() {
			this.replaceable = true;
			return this;
		}

		public BlockBehaviour.Properties setId(ResourceKey<Block> arg) {
			this.id = arg;
			return this;
		}

		public BlockBehaviour.Properties overrideDescription(String string) {
			this.descriptionId = DependantName.fixed(string);
			return this;
		}

		protected String effectiveDescriptionId() {
			return this.descriptionId.get((ResourceKey<Block>)Objects.requireNonNull(this.id, "Block id not set"));
		}
	}

	@FunctionalInterface
	public interface StateArgumentPredicate<A> {
		boolean test(BlockState arg, BlockGetter arg2, BlockPos arg3, A object);
	}

	@FunctionalInterface
	public interface StatePredicate {
		boolean test(BlockState arg, BlockGetter arg2, BlockPos arg3);
	}
}
