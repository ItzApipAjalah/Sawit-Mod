package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class BrushItem extends Item {
	public static final int ANIMATION_DURATION = 10;
	private static final int USE_DURATION = 200;

	public BrushItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Player player = arg.getPlayer();
		if (player != null && this.calculateHitResult(player).getType() == HitResult.Type.BLOCK) {
			player.startUsingItem(arg.getHand());
		}

		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack arg) {
		return ItemUseAnimation.BRUSH;
	}

	@Override
	public int getUseDuration(ItemStack arg, LivingEntity arg2) {
		return 200;
	}

	@Override
	public void onUseTick(Level arg, LivingEntity arg2, ItemStack arg3, int j) {
		if (j >= 0 && arg2 instanceof Player player) {
			HitResult hitresult = this.calculateHitResult(player);
			if (hitresult instanceof BlockHitResult blockhitresult && hitresult.getType() == HitResult.Type.BLOCK) {
				int i = this.getUseDuration(arg3, arg2) - j + 1;
				boolean flag = i % 10 == 5;
				if (flag) {
					BlockPos blockpos = blockhitresult.getBlockPos();
					BlockState blockstate = arg.getBlockState(blockpos);
					HumanoidArm humanoidarm = arg2.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
					if (blockstate.shouldSpawnTerrainParticles() && blockstate.getRenderShape() != RenderShape.INVISIBLE) {
						this.spawnDustParticles(arg, blockhitresult, blockstate, arg2.getViewVector(0.0F), humanoidarm);
					}

					SoundEvent soundevent;
					if (blockstate.getBlock() instanceof BrushableBlock brushableblock) {
						soundevent = brushableblock.getBrushSound();
					} else {
						soundevent = SoundEvents.BRUSH_GENERIC;
					}

					arg.playSound(player, blockpos, soundevent, SoundSource.BLOCKS);
					if (arg instanceof ServerLevel serverlevel && arg.getBlockEntity(blockpos) instanceof BrushableBlockEntity brushableblockentity) {
						boolean flag1 = brushableblockentity.brush(arg.getGameTime(), serverlevel, player, blockhitresult.getDirection(), arg3);
						if (flag1) {
							EquipmentSlot equipmentslot = arg3.equals(player.getItemBySlot(EquipmentSlot.OFFHAND)) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
							arg3.hurtAndBreak(1, player, equipmentslot);
						}
					}
				}

				return;
			}

			arg2.releaseUsingItem();
		} else {
			arg2.releaseUsingItem();
		}
	}

	private HitResult calculateHitResult(Player arg) {
		return ProjectileUtil.getHitResultOnViewVector(arg, EntitySelector.CAN_BE_PICKED, arg.blockInteractionRange());
	}

	private void spawnDustParticles(Level arg, BlockHitResult arg2, BlockState arg3, Vec3 arg4, HumanoidArm arg5) {
		double d0 = 3.0;
		int i = arg5 == HumanoidArm.RIGHT ? 1 : -1;
		int j = arg.getRandom().nextInt(7, 12);
		BlockParticleOption blockparticleoption = new BlockParticleOption(ParticleTypes.BLOCK, arg3);
		Direction direction = arg2.getDirection();
		BrushItem.DustParticlesDelta brushitem$dustparticlesdelta = BrushItem.DustParticlesDelta.fromDirection(arg4, direction);
		Vec3 vec3 = arg2.getLocation();

		for (int k = 0; k < j; k++) {
			arg.addParticle(
				blockparticleoption,
				vec3.x - (direction == Direction.WEST ? 1.0E-6F : 0.0F),
				vec3.y,
				vec3.z - (direction == Direction.NORTH ? 1.0E-6F : 0.0F),
				brushitem$dustparticlesdelta.xd() * i * 3.0 * arg.getRandom().nextDouble(),
				0.0,
				brushitem$dustparticlesdelta.zd() * i * 3.0 * arg.getRandom().nextDouble()
			);
		}
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_BRUSH_ACTIONS.contains(itemAbility);
	}

	record DustParticlesDelta(double xd, double yd, double zd) {
		private static final double ALONG_SIDE_DELTA = 1.0;
		private static final double OUT_FROM_SIDE_DELTA = 0.1;

		public static BrushItem.DustParticlesDelta fromDirection(Vec3 arg, Direction arg2) {
			double d0 = 0.0;

			return switch (arg2) {
				case DOWN, UP -> new BrushItem.DustParticlesDelta(arg.z(), 0.0, -arg.x());
				case NORTH -> new BrushItem.DustParticlesDelta(1.0, 0.0, -0.1);
				case SOUTH -> new BrushItem.DustParticlesDelta(-1.0, 0.0, 0.1);
				case WEST -> new BrushItem.DustParticlesDelta(-0.1, 0.0, -1.0);
				case EAST -> new BrushItem.DustParticlesDelta(0.1, 0.0, 1.0);
			};
		}
	}
}
