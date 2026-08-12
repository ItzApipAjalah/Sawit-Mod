package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

public class BowItem extends ProjectileWeaponItem {
	public static final int MAX_DRAW_DURATION = 20;
	public static final int DEFAULT_RANGE = 15;

	public BowItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public boolean releaseUsing(ItemStack arg, Level arg2, LivingEntity arg3, int j) {
		if (arg3 instanceof Player player) {
			ItemStack itemstack = player.getProjectile(arg);
			if (itemstack.isEmpty()) {
				return false;
			} else {
				int i = this.getUseDuration(arg, arg3) - j;
				i = EventHooks.onArrowLoose(arg, arg2, player, i, !itemstack.isEmpty());
				if (i < 0) {
					return false;
				} else {
					float f = getPowerForTime(i);
					if (f < 0.1) {
						return false;
					} else {
						List<ItemStack> list = draw(arg, itemstack, player);
						if (arg2 instanceof ServerLevel serverlevel && !list.isEmpty()) {
							this.shoot(serverlevel, player, player.getUsedItemHand(), arg, list, f * 3.0F, 1.0F, f == 1.0F, null);
						}

						arg2.playSound(
							null,
							player.getX(),
							player.getY(),
							player.getZ(),
							SoundEvents.ARROW_SHOOT,
							SoundSource.PLAYERS,
							1.0F,
							1.0F / (arg2.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F
						);
						player.awardStat(Stats.ITEM_USED.get(this));
						return true;
					}
				}
			}
		} else {
			return false;
		}
	}

	@Override
	protected void shootProjectile(LivingEntity arg, Projectile arg2, int i, float f, float g, float h, @Nullable LivingEntity arg3) {
		arg2.shootFromRotation(arg, arg.getXRot(), arg.getYRot() + h, 0.0F, f, g);
	}

	public static float getPowerForTime(int i) {
		float f = i / 20.0F;
		f = (f * f + f * 2.0F) / 3.0F;
		if (f > 1.0F) {
			f = 1.0F;
		}

		return f;
	}

	@Override
	public int getUseDuration(ItemStack arg, LivingEntity arg2) {
		return 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack arg) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemstack = arg2.getItemInHand(arg3);
		boolean flag = !arg2.getProjectile(itemstack).isEmpty();
		InteractionResult ret = EventHooks.onArrowNock(itemstack, arg, arg2, arg3, flag);
		if (ret != null) {
			return ret;
		} else if (!arg2.hasInfiniteMaterials() && !flag) {
			return InteractionResult.FAIL;
		} else {
			arg2.startUsingItem(arg3);
			return InteractionResult.CONSUME;
		}
	}

	@Override
	public Predicate<ItemStack> getAllSupportedProjectiles() {
		return ARROW_ONLY;
	}

	@Override
	public int getDefaultProjectileRange() {
		return 15;
	}
}
