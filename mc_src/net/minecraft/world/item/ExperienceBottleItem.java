package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.level.Level;

public class ExperienceBottleItem extends Item implements ProjectileItem {
	public ExperienceBottleItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemStack = arg2.getItemInHand(arg3);
		arg.playSound(
			null,
			arg2.getX(),
			arg2.getY(),
			arg2.getZ(),
			SoundEvents.EXPERIENCE_BOTTLE_THROW,
			SoundSource.NEUTRAL,
			0.5F,
			0.4F / (arg.getRandom().nextFloat() * 0.4F + 0.8F)
		);
		if (arg instanceof ServerLevel serverLevel) {
			Projectile.spawnProjectileFromRotation(ThrownExperienceBottle::new, serverLevel, itemStack, arg2, -20.0F, 0.7F, 1.0F);
		}

		arg2.awardStat(Stats.ITEM_USED.get(this));
		itemStack.consume(1, arg2);
		return InteractionResult.SUCCESS;
	}

	@Override
	public Projectile asProjectile(Level arg, Position arg2, ItemStack arg3, Direction arg4) {
		return new ThrownExperienceBottle(arg, arg2.x(), arg2.y(), arg2.z(), arg3);
	}

	@Override
	public ProjectileItem.DispenseConfig createDispenseConfig() {
		return ProjectileItem.DispenseConfig.builder()
			.uncertainty(ProjectileItem.DispenseConfig.DEFAULT.uncertainty() * 0.5F)
			.power(ProjectileItem.DispenseConfig.DEFAULT.power() * 1.25F)
			.build();
	}
}
