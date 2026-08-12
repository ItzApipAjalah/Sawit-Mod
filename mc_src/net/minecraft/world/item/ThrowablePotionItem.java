package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;

public class ThrowablePotionItem extends PotionItem implements ProjectileItem {
	public ThrowablePotionItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemStack = arg2.getItemInHand(arg3);
		if (arg instanceof ServerLevel serverLevel) {
			Projectile.spawnProjectileFromRotation(ThrownPotion::new, serverLevel, itemStack, arg2, -20.0F, 0.5F, 1.0F);
		}

		arg2.awardStat(Stats.ITEM_USED.get(this));
		itemStack.consume(1, arg2);
		return InteractionResult.SUCCESS;
	}

	@Override
	public Projectile asProjectile(Level arg, Position arg2, ItemStack arg3, Direction arg4) {
		return new ThrownPotion(arg, arg2.x(), arg2.y(), arg2.z(), arg3);
	}

	@Override
	public ProjectileItem.DispenseConfig createDispenseConfig() {
		return ProjectileItem.DispenseConfig.builder()
			.uncertainty(ProjectileItem.DispenseConfig.DEFAULT.uncertainty() * 0.5F)
			.power(ProjectileItem.DispenseConfig.DEFAULT.power() * 1.25F)
			.build();
	}
}
