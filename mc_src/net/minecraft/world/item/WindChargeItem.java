package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

public class WindChargeItem extends Item implements ProjectileItem {
	public WindChargeItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemStack = arg2.getItemInHand(arg3);
		if (arg instanceof ServerLevel serverLevel) {
			Projectile.spawnProjectileFromRotation(
				(arg3x, arg4, arg5) -> new WindCharge(arg2, arg, arg2.position().x(), arg2.getEyePosition().y(), arg2.position().z()),
				serverLevel,
				itemStack,
				arg2,
				0.0F,
				1.5F,
				1.0F
			);
		}

		arg.playSound(
			null, arg2.getX(), arg2.getY(), arg2.getZ(), SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (arg.getRandom().nextFloat() * 0.4F + 0.8F)
		);
		arg2.awardStat(Stats.ITEM_USED.get(this));
		itemStack.consume(1, arg2);
		return InteractionResult.SUCCESS;
	}

	@Override
	public Projectile asProjectile(Level arg, Position arg2, ItemStack arg3, Direction arg4) {
		RandomSource randomSource = arg.getRandom();
		double d = randomSource.triangle((double)arg4.getStepX(), 0.11485000000000001);
		double e = randomSource.triangle((double)arg4.getStepY(), 0.11485000000000001);
		double f = randomSource.triangle((double)arg4.getStepZ(), 0.11485000000000001);
		Vec3 vec3 = new Vec3(d, e, f);
		WindCharge windCharge = new WindCharge(arg, arg2.x(), arg2.y(), arg2.z(), vec3);
		windCharge.setDeltaMovement(vec3);
		return windCharge;
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
			.overrideDispenseEvent(1051)
			.build();
	}
}
