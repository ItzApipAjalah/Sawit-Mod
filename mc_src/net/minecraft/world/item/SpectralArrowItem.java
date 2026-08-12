package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.level.Level;

public class SpectralArrowItem extends ArrowItem {
	public SpectralArrowItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public AbstractArrow createArrow(Level arg, ItemStack arg2, LivingEntity arg3, @Nullable ItemStack arg4) {
		return new SpectralArrow(arg, arg3, arg2.copyWithCount(1), arg4);
	}

	@Override
	public Projectile asProjectile(Level arg, Position arg2, ItemStack arg3, Direction arg4) {
		SpectralArrow spectralArrow = new SpectralArrow(arg, arg2.x(), arg2.y(), arg2.z(), arg3.copyWithCount(1), null);
		spectralArrow.pickup = AbstractArrow.Pickup.ALLOWED;
		return spectralArrow;
	}
}
