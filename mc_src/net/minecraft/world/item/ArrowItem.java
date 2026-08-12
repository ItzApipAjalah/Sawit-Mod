package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

public class ArrowItem extends Item implements ProjectileItem {
	public ArrowItem(Item.Properties arg) {
		super(arg);
	}

	public AbstractArrow createArrow(Level arg, ItemStack arg2, LivingEntity arg3, @Nullable ItemStack arg4) {
		return new Arrow(arg, arg3, arg2.copyWithCount(1), arg4);
	}

	@Override
	public Projectile asProjectile(Level arg, Position arg2, ItemStack arg3, Direction arg4) {
		Arrow arrow = new Arrow(arg, arg2.x(), arg2.y(), arg2.z(), arg3.copyWithCount(1), null);
		arrow.pickup = AbstractArrow.Pickup.ALLOWED;
		return arrow;
	}

	public boolean isInfinite(ItemStack ammo, ItemStack bow, LivingEntity livingEntity) {
		return false;
	}
}
