package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class FireChargeItem extends Item implements ProjectileItem {
	public FireChargeItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Level level = arg.getLevel();
		BlockPos blockpos = arg.getClickedPos();
		BlockState blockstate = level.getBlockState(blockpos);
		boolean flag = false;
		BlockState blockstate2 = blockstate.getToolModifiedState(arg, ItemAbilities.FIRESTARTER_LIGHT, false);
		if (blockstate2 == null) {
			blockpos = blockpos.relative(arg.getClickedFace());
			if (BaseFireBlock.canBePlacedAt(level, blockpos, arg.getHorizontalDirection())) {
				this.playSound(level, blockpos);
				level.setBlockAndUpdate(blockpos, BaseFireBlock.getState(level, blockpos));
				level.gameEvent(arg.getPlayer(), GameEvent.BLOCK_PLACE, blockpos);
				flag = true;
			}
		} else {
			this.playSound(level, blockpos);
			level.setBlockAndUpdate(blockpos, blockstate2);
			level.gameEvent(arg.getPlayer(), GameEvent.BLOCK_CHANGE, blockpos);
			flag = true;
		}

		if (flag) {
			arg.getItemInHand().shrink(1);
			return InteractionResult.SUCCESS;
		} else {
			return InteractionResult.FAIL;
		}
	}

	private void playSound(Level arg, BlockPos arg2) {
		RandomSource randomsource = arg.getRandom();
		arg.playSound(null, arg2, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F);
	}

	@Override
	public Projectile asProjectile(Level arg, Position arg2, ItemStack arg3, Direction arg4) {
		RandomSource randomsource = arg.getRandom();
		double d0 = randomsource.triangle((double)arg4.getStepX(), 0.11485000000000001);
		double d1 = randomsource.triangle((double)arg4.getStepY(), 0.11485000000000001);
		double d2 = randomsource.triangle((double)arg4.getStepZ(), 0.11485000000000001);
		Vec3 vec3 = new Vec3(d0, d1, d2);
		SmallFireball smallfireball = new SmallFireball(arg, arg2.x(), arg2.y(), arg2.z(), vec3.normalize());
		smallfireball.setItem(arg3);
		return smallfireball;
	}

	@Override
	public void shoot(Projectile arg, double d, double e, double f, float g, float h) {
	}

	@Override
	public ProjectileItem.DispenseConfig createDispenseConfig() {
		return ProjectileItem.DispenseConfig.builder()
			.positionFunction((arg, arg2) -> DispenserBlock.getDispensePosition(arg, 1.0, Vec3.ZERO))
			.uncertainty(6.6666665F)
			.power(1.0F)
			.overrideDispenseEvent(1018)
			.build();
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_FIRECHARGE_ACTIONS.contains(itemAbility);
	}
}
