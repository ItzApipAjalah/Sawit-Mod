package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MaceItem extends Item {
	private static final int DEFAULT_ATTACK_DAMAGE = 5;
	private static final float DEFAULT_ATTACK_SPEED = -3.4F;
	public static final float SMASH_ATTACK_FALL_THRESHOLD = 1.5F;
	private static final float SMASH_ATTACK_HEAVY_THRESHOLD = 5.0F;
	public static final float SMASH_ATTACK_KNOCKBACK_RADIUS = 3.5F;
	private static final float SMASH_ATTACK_KNOCKBACK_POWER = 0.7F;

	public MaceItem(Item.Properties arg) {
		super(arg);
	}

	public static ItemAttributeModifiers createAttributes() {
		return ItemAttributeModifiers.builder()
			.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
			.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.4F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
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
	public boolean hurtEnemy(ItemStack arg, LivingEntity arg2, LivingEntity arg3) {
		if (canSmashAttack(arg3)) {
			ServerLevel serverLevel = (ServerLevel)arg3.level();
			arg3.setDeltaMovement(arg3.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
			if (arg3 instanceof ServerPlayer serverPlayer) {
				serverPlayer.currentImpulseImpactPos = this.calculateImpactPosition(serverPlayer);
				serverPlayer.setIgnoreFallDamageFromCurrentImpulse(true);
				serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
			}

			if (arg2.onGround()) {
				if (arg3 instanceof ServerPlayer serverPlayer) {
					serverPlayer.setSpawnExtraParticlesOnFall(true);
				}

				SoundEvent soundEvent = arg3.fallDistance > 5.0F ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
				serverLevel.playSound(null, arg3.getX(), arg3.getY(), arg3.getZ(), soundEvent, arg3.getSoundSource(), 1.0F, 1.0F);
			} else {
				serverLevel.playSound(null, arg3.getX(), arg3.getY(), arg3.getZ(), SoundEvents.MACE_SMASH_AIR, arg3.getSoundSource(), 1.0F, 1.0F);
			}

			knockback(serverLevel, arg3, arg2);
		}

		return true;
	}

	private Vec3 calculateImpactPosition(ServerPlayer arg) {
		return arg.isIgnoringFallDamageFromCurrentImpulse() && arg.currentImpulseImpactPos != null && arg.currentImpulseImpactPos.y <= arg.position().y
			? arg.currentImpulseImpactPos
			: arg.position();
	}

	@Override
	public void postHurtEnemy(ItemStack arg, LivingEntity arg2, LivingEntity arg3) {
		arg.hurtAndBreak(1, arg3, EquipmentSlot.MAINHAND);
		if (canSmashAttack(arg3)) {
			arg3.resetFallDistance();
		}
	}

	@Override
	public float getAttackDamageBonus(Entity arg, float f, DamageSource arg2) {
		if (arg2.getDirectEntity() instanceof LivingEntity livingEntity) {
			if (!canSmashAttack(livingEntity)) {
				return 0.0F;
			} else {
				float g = 3.0F;
				float h = 8.0F;
				float i = livingEntity.fallDistance;
				float j;
				if (i <= 3.0F) {
					j = 4.0F * i;
				} else if (i <= 8.0F) {
					j = 12.0F + 2.0F * (i - 3.0F);
				} else {
					j = 22.0F + i - 8.0F;
				}

				return livingEntity.level() instanceof ServerLevel serverLevel
					? j + EnchantmentHelper.modifyFallBasedDamage(serverLevel, livingEntity.getWeaponItem(), arg, arg2, 0.0F) * i
					: j;
			}
		} else {
			return 0.0F;
		}
	}

	private static void knockback(Level arg, Entity arg2, Entity arg3) {
		arg.levelEvent(2013, arg3.getOnPos(), 750);
		arg.getEntitiesOfClass(LivingEntity.class, arg3.getBoundingBox().inflate(3.5), knockbackPredicate(arg2, arg3)).forEach(arg3x -> {
			Vec3 vec3 = arg3x.position().subtract(arg3.position());
			double d = getKnockbackPower(arg2, arg3x, vec3);
			Vec3 vec32 = vec3.normalize().scale(d);
			if (d > 0.0) {
				arg3x.push(vec32.x, 0.7F, vec32.z);
				if (arg3x instanceof ServerPlayer serverPlayer) {
					serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
				}
			}
		});
	}

	private static Predicate<LivingEntity> knockbackPredicate(Entity arg, Entity arg2) {
		return arg3 -> {
			boolean bl = !arg3.isSpectator();
			boolean bl2 = arg3 != arg && arg3 != arg2;
			boolean bl3 = !arg.isAlliedTo(arg3);
			boolean bl4 = !(arg3 instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame() && arg.getUUID().equals(tamableAnimal.getOwnerUUID()));
			boolean bl5 = !(arg3 instanceof ArmorStand armorStand && armorStand.isMarker());
			boolean bl6 = arg2.distanceToSqr(arg3) <= Math.pow(3.5, 2.0);
			return bl && bl2 && bl3 && bl4 && bl5 && bl6;
		};
	}

	private static double getKnockbackPower(Entity arg, LivingEntity arg2, Vec3 arg3) {
		return (3.5 - arg3.length()) * 0.7F * (arg.fallDistance > 5.0F ? 2 : 1) * (1.0 - arg2.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
	}

	public static boolean canSmashAttack(LivingEntity arg) {
		return arg.fallDistance > 1.5F && !arg.isFallFlying();
	}

	@Nullable
	@Override
	public DamageSource getDamageSource(LivingEntity arg) {
		return canSmashAttack(arg) ? arg.damageSources().mace(arg) : super.getDamageSource(arg);
	}
}
