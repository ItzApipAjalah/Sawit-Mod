package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public abstract class ProjectileWeaponItem extends Item {
	public static final Predicate<ItemStack> ARROW_ONLY = arg -> arg.is(ItemTags.ARROWS);
	public static final Predicate<ItemStack> ARROW_OR_FIREWORK = ARROW_ONLY.or(arg -> arg.is(Items.FIREWORK_ROCKET));

	public ProjectileWeaponItem(Item.Properties arg) {
		super(arg);
	}

	@Deprecated
	public Predicate<ItemStack> getSupportedHeldProjectiles() {
		return this.getAllSupportedProjectiles();
	}

	@Deprecated
	public abstract Predicate<ItemStack> getAllSupportedProjectiles();

	public Predicate<ItemStack> getSupportedHeldProjectiles(ItemStack stack) {
		return this.getAllSupportedProjectiles(stack).or(this.getSupportedHeldProjectiles());
	}

	public Predicate<ItemStack> getAllSupportedProjectiles(ItemStack stack) {
		return this.getAllSupportedProjectiles();
	}

	public static ItemStack getHeldProjectile(LivingEntity arg, Predicate<ItemStack> predicate) {
		if (predicate.test(arg.getItemInHand(InteractionHand.OFF_HAND))) {
			return arg.getItemInHand(InteractionHand.OFF_HAND);
		} else {
			return predicate.test(arg.getItemInHand(InteractionHand.MAIN_HAND)) ? arg.getItemInHand(InteractionHand.MAIN_HAND) : ItemStack.EMPTY;
		}
	}

	public abstract int getDefaultProjectileRange();

	protected void shoot(
		ServerLevel arg, LivingEntity arg2, InteractionHand arg3, ItemStack arg4, List<ItemStack> list, float g, float h, boolean bl, @Nullable LivingEntity arg5
	) {
		float f = EnchantmentHelper.processProjectileSpread(arg, arg4, arg2, 0.0F);
		float f1 = list.size() == 1 ? 0.0F : 2.0F * f / (list.size() - 1);
		float f2 = (list.size() - 1) % 2 * f1 / 2.0F;
		float f3 = 1.0F;

		for (int i = 0; i < list.size(); i++) {
			ItemStack itemstack = (ItemStack)list.get(i);
			if (!itemstack.isEmpty()) {
				float f4 = f2 + f3 * ((i + 1) / 2) * f1;
				f3 = -f3;
				int j = i;
				Projectile.spawnProjectile(
					this.createProjectile(arg, arg2, arg4, itemstack, bl), arg, itemstack, arg3x -> this.shootProjectile(arg2, arg3x, j, g, h, f4, arg5)
				);
				arg4.hurtAndBreak(this.getDurabilityUse(itemstack), arg2, LivingEntity.getSlotForHand(arg3));
				if (arg4.isEmpty()) {
					break;
				}
			}
		}
	}

	protected int getDurabilityUse(ItemStack arg) {
		return 1;
	}

	protected abstract void shootProjectile(LivingEntity arg, Projectile arg2, int i, float f, float g, float h, @Nullable LivingEntity arg3);

	protected Projectile createProjectile(Level arg, LivingEntity arg2, ItemStack arg3, ItemStack arg4, boolean bl) {
		ArrowItem arrowitem = arg4.getItem() instanceof ArrowItem arrowitem1 ? arrowitem1 : (ArrowItem)Items.ARROW;
		AbstractArrow abstractarrow = arrowitem.createArrow(arg, arg4, arg2, arg3);
		if (bl) {
			abstractarrow.setCritArrow(true);
		}

		return this.customArrow(abstractarrow, arg4, arg3);
	}

	protected static List<ItemStack> draw(ItemStack arg, ItemStack arg2, LivingEntity arg3) {
		if (arg2.isEmpty()) {
			return List.of();
		} else {
			int i = arg3.level() instanceof ServerLevel serverlevel ? EnchantmentHelper.processProjectileCount(serverlevel, arg, arg3, 1) : 1;
			List<ItemStack> list = new ArrayList(i);
			ItemStack itemstack1 = arg2.copy();

			for (int j = 0; j < i; j++) {
				ItemStack itemstack = useAmmo(arg, j == 0 ? arg2 : itemstack1, arg3, j > 0);
				if (!itemstack.isEmpty()) {
					list.add(itemstack);
				}
			}

			return list;
		}
	}

	protected static ItemStack useAmmo(ItemStack arg, ItemStack arg2, LivingEntity arg3, boolean bl) {
		int i = bl
				|| !(arg3.level() instanceof ServerLevel serverlevel && !arg3.hasInfiniteMaterials())
				|| arg2.getItem() instanceof ArrowItem ai && ai.isInfinite(arg2, arg, arg3)
			? 0
			: EnchantmentHelper.processAmmoUse(serverlevel, arg, arg2, 1);
		if (i > arg2.getCount()) {
			return ItemStack.EMPTY;
		} else if (i == 0) {
			ItemStack itemstack1 = arg2.copyWithCount(1);
			itemstack1.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
			return itemstack1;
		} else {
			ItemStack itemstack = arg2.split(i);
			if (arg2.isEmpty() && arg3 instanceof Player player) {
				player.getInventory().removeItem(arg2);
			}

			return itemstack;
		}
	}

	public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
		return arrow;
	}

	public ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack projectileWeaponItem) {
		return Items.ARROW.getDefaultInstance();
	}
}
