package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class TridentItem extends Item implements ProjectileItem {
	public static final int THROW_THRESHOLD_TIME = 10;
	public static final float BASE_DAMAGE = 8.0F;
	public static final float SHOOT_POWER = 2.5F;

	public TridentItem(Item.Properties arg) {
		super(arg);
	}

	public static ItemAttributeModifiers createAttributes() {
		return ItemAttributeModifiers.builder()
			.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
			.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.9F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
			.build();
	}

	public static Tool createToolProperties() {
		return new Tool(List.of(), 1.0F, 2);
	}

	@Override
	public boolean canAttackBlock(BlockState arg, Level arg2, BlockPos arg3, Player arg4) {
		return !arg4.isCreative();
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack arg) {
		return ItemUseAnimation.SPEAR;
	}

	@Override
	public int getUseDuration(ItemStack arg, LivingEntity arg2) {
		return 72000;
	}

	@Override
	public boolean releaseUsing(ItemStack arg, Level arg2, LivingEntity arg3, int j) {
		if (arg3 instanceof Player player) {
			int i = this.getUseDuration(arg, arg3) - j;
			if (i < 10) {
				return false;
			} else {
				float f = EnchantmentHelper.getTridentSpinAttackStrength(arg, player);
				if (f > 0.0F && !player.isInWaterOrRain()) {
					return false;
				} else if (arg.nextDamageWillBreak()) {
					return false;
				} else {
					Holder<SoundEvent> holder = (Holder<SoundEvent>)EnchantmentHelper.pickHighestLevel(arg, EnchantmentEffectComponents.TRIDENT_SOUND)
						.orElse(SoundEvents.TRIDENT_THROW);
					if (arg2 instanceof ServerLevel serverlevel) {
						arg.hurtWithoutBreaking(1, player);
						if (f == 0.0F) {
							ThrownTrident throwntrident = Projectile.spawnProjectileFromRotation(ThrownTrident::new, serverlevel, arg, player, 0.0F, 2.5F, 1.0F);
							if (player.hasInfiniteMaterials()) {
								throwntrident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
							} else {
								player.getInventory().removeItem(arg);
							}

							arg2.playSound(null, throwntrident, holder.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
							return true;
						}
					}

					player.awardStat(Stats.ITEM_USED.get(this));
					if (f > 0.0F) {
						float f7 = player.getYRot();
						float f1 = player.getXRot();
						float f2 = -Mth.sin(f7 * (float) (Math.PI / 180.0)) * Mth.cos(f1 * (float) (Math.PI / 180.0));
						float f3 = -Mth.sin(f1 * (float) (Math.PI / 180.0));
						float f4 = Mth.cos(f7 * (float) (Math.PI / 180.0)) * Mth.cos(f1 * (float) (Math.PI / 180.0));
						float f5 = Mth.sqrt(f2 * f2 + f3 * f3 + f4 * f4);
						f2 *= f / f5;
						f3 *= f / f5;
						f4 *= f / f5;
						player.push(f2, f3, f4);
						player.startAutoSpinAttack(20, 8.0F, arg);
						if (player.onGround()) {
							float f6 = 1.1999999F;
							player.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
						}

						arg2.playSound(null, player, holder.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
						return true;
					} else {
						return false;
					}
				}
			}
		} else {
			return false;
		}
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemstack = arg2.getItemInHand(arg3);
		if (itemstack.nextDamageWillBreak()) {
			return InteractionResult.FAIL;
		} else if (EnchantmentHelper.getTridentSpinAttackStrength(itemstack, arg2) > 0.0F && !arg2.isInWaterOrRain()) {
			return InteractionResult.FAIL;
		} else {
			arg2.startUsingItem(arg3);
			return InteractionResult.CONSUME;
		}
	}

	@Override
	public boolean hurtEnemy(ItemStack arg, LivingEntity arg2, LivingEntity arg3) {
		return true;
	}

	@Override
	public void postHurtEnemy(ItemStack arg, LivingEntity arg2, LivingEntity arg3) {
		arg.hurtAndBreak(1, arg3, EquipmentSlot.MAINHAND);
	}

	@Override
	public Projectile asProjectile(Level arg, Position arg2, ItemStack arg3, Direction arg4) {
		ThrownTrident throwntrident = new ThrownTrident(arg, arg2.x(), arg2.y(), arg2.z(), arg3.copyWithCount(1));
		throwntrident.pickup = AbstractArrow.Pickup.ALLOWED;
		return throwntrident;
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_TRIDENT_ACTIONS.contains(itemAbility);
	}
}
