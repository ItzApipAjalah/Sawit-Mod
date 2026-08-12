package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CrossbowItem extends ProjectileWeaponItem {
	private static final float MAX_CHARGE_DURATION = 1.25F;
	public static final int DEFAULT_RANGE = 8;
	private boolean startSoundPlayed = false;
	private boolean midLoadSoundPlayed = false;
	private static final float START_SOUND_PERCENT = 0.2F;
	private static final float MID_SOUND_PERCENT = 0.5F;
	private static final float ARROW_POWER = 3.15F;
	private static final float FIREWORK_POWER = 1.6F;
	public static final float MOB_ARROW_POWER = 1.6F;
	private static final CrossbowItem.ChargingSounds DEFAULT_SOUNDS = new CrossbowItem.ChargingSounds(
		Optional.of(SoundEvents.CROSSBOW_LOADING_START), Optional.of(SoundEvents.CROSSBOW_LOADING_MIDDLE), Optional.of(SoundEvents.CROSSBOW_LOADING_END)
	);

	public CrossbowItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public Predicate<ItemStack> getSupportedHeldProjectiles() {
		return ARROW_OR_FIREWORK;
	}

	@Override
	public Predicate<ItemStack> getAllSupportedProjectiles() {
		return ARROW_ONLY;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemstack = arg2.getItemInHand(arg3);
		ChargedProjectiles chargedprojectiles = itemstack.get(DataComponents.CHARGED_PROJECTILES);
		if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
			this.performShooting(arg, arg2, arg3, itemstack, getShootingPower(chargedprojectiles), 1.0F, null);
			return InteractionResult.CONSUME;
		} else if (!arg2.getProjectile(itemstack).isEmpty()) {
			this.startSoundPlayed = false;
			this.midLoadSoundPlayed = false;
			arg2.startUsingItem(arg3);
			return InteractionResult.CONSUME;
		} else {
			return InteractionResult.FAIL;
		}
	}

	private static float getShootingPower(ChargedProjectiles arg) {
		return arg.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
	}

	@Override
	public boolean releaseUsing(ItemStack arg, Level arg2, LivingEntity arg3, int j) {
		int i = this.getUseDuration(arg, arg3) - j;
		float f = getPowerForTime(i, arg, arg3);
		if (f >= 1.0F && !isCharged(arg) && tryLoadProjectiles(arg3, arg)) {
			CrossbowItem.ChargingSounds crossbowitem$chargingsounds = this.getChargingSounds(arg);
			crossbowitem$chargingsounds.end()
				.ifPresent(
					arg3x -> arg2.playSound(
						null,
						arg3.getX(),
						arg3.getY(),
						arg3.getZ(),
						(SoundEvent)arg3x.value(),
						arg3.getSoundSource(),
						1.0F,
						1.0F / (arg2.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F
					)
				);
			return true;
		} else {
			return false;
		}
	}

	private static boolean tryLoadProjectiles(LivingEntity arg, ItemStack arg2) {
		List<ItemStack> list = draw(arg2, arg.getProjectile(arg2), arg);
		if (!list.isEmpty()) {
			arg2.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(list));
			return true;
		} else {
			return false;
		}
	}

	public static boolean isCharged(ItemStack arg) {
		ChargedProjectiles chargedprojectiles = arg.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
		return !chargedprojectiles.isEmpty();
	}

	@Override
	protected void shootProjectile(LivingEntity arg, Projectile arg2, int i, float g, float h, float j, @Nullable LivingEntity arg3) {
		Vector3f vector3f;
		if (arg3 != null) {
			double d0 = arg3.getX() - arg.getX();
			double d1 = arg3.getZ() - arg.getZ();
			double d2 = Math.sqrt(d0 * d0 + d1 * d1);
			double d3 = arg3.getY(0.3333333333333333) - arg2.getY() + d2 * 0.2F;
			vector3f = getProjectileShotVector(arg, new Vec3(d0, d3, d1), j);
		} else {
			Vec3 vec3 = arg.getUpVector(1.0F);
			Quaternionf quaternionf = new Quaternionf().setAngleAxis((double)(j * (float) (Math.PI / 180.0)), vec3.x, vec3.y, vec3.z);
			Vec3 vec31 = arg.getViewVector(1.0F);
			vector3f = vec31.toVector3f().rotate(quaternionf);
		}

		arg2.shoot(vector3f.x(), vector3f.y(), vector3f.z(), g, h);
		float f = getShotPitch(arg.getRandom(), i);
		arg.level().playSound(null, arg.getX(), arg.getY(), arg.getZ(), SoundEvents.CROSSBOW_SHOOT, arg.getSoundSource(), 1.0F, f);
	}

	private static Vector3f getProjectileShotVector(LivingEntity arg, Vec3 arg2, float f) {
		Vector3f vector3f = arg2.toVector3f().normalize();
		Vector3f vector3f1 = new Vector3f(vector3f).cross(new Vector3f(0.0F, 1.0F, 0.0F));
		if (vector3f1.lengthSquared() <= 1.0E-7) {
			Vec3 vec3 = arg.getUpVector(1.0F);
			vector3f1 = new Vector3f(vector3f).cross(vec3.toVector3f());
		}

		Vector3f vector3f2 = new Vector3f(vector3f).rotateAxis((float) (Math.PI / 2), vector3f1.x, vector3f1.y, vector3f1.z);
		return new Vector3f(vector3f).rotateAxis(f * (float) (Math.PI / 180.0), vector3f2.x, vector3f2.y, vector3f2.z);
	}

	@Override
	protected Projectile createProjectile(Level arg, LivingEntity arg2, ItemStack arg3, ItemStack arg4, boolean bl) {
		if (arg4.is(Items.FIREWORK_ROCKET)) {
			return new FireworkRocketEntity(arg, arg4, arg2, arg2.getX(), arg2.getEyeY() - 0.15F, arg2.getZ(), true);
		} else {
			Projectile projectile = super.createProjectile(arg, arg2, arg3, arg4, bl);
			if (projectile instanceof AbstractArrow abstractarrow) {
				abstractarrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
			}

			return projectile;
		}
	}

	@Override
	protected int getDurabilityUse(ItemStack arg) {
		return arg.is(Items.FIREWORK_ROCKET) ? 3 : 1;
	}

	public void performShooting(Level arg, LivingEntity arg2, InteractionHand arg3, ItemStack arg4, float f, float g, @Nullable LivingEntity arg5) {
		if (arg instanceof ServerLevel serverlevel) {
			if (arg2 instanceof Player player && EventHooks.onArrowLoose(arg4, arg2.level(), player, 1, true) < 0) {
				return;
			}

			ChargedProjectiles chargedprojectiles = arg4.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
			if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
				this.shoot(serverlevel, arg2, arg3, arg4, chargedprojectiles.getItems(), f, g, arg2 instanceof Player, arg5);
				if (arg2 instanceof ServerPlayer serverplayer) {
					CriteriaTriggers.SHOT_CROSSBOW.trigger(serverplayer, arg4);
					serverplayer.awardStat(Stats.ITEM_USED.get(arg4.getItem()));
				}
			}
		}
	}

	private static float getShotPitch(RandomSource arg, int i) {
		return i == 0 ? 1.0F : getRandomShotPitch((i & 1) == 1, arg);
	}

	private static float getRandomShotPitch(boolean bl, RandomSource arg) {
		float f = bl ? 0.63F : 0.43F;
		return 1.0F / (arg.nextFloat() * 0.5F + 1.8F) + f;
	}

	@Override
	public void onUseTick(Level arg, LivingEntity arg2, ItemStack arg3, int i) {
		if (!arg.isClientSide) {
			CrossbowItem.ChargingSounds crossbowitem$chargingsounds = this.getChargingSounds(arg3);
			float f = (float)(arg3.getUseDuration(arg2) - i) / getChargeDuration(arg3, arg2);
			if (f < 0.2F) {
				this.startSoundPlayed = false;
				this.midLoadSoundPlayed = false;
			}

			if (f >= 0.2F && !this.startSoundPlayed) {
				this.startSoundPlayed = true;
				crossbowitem$chargingsounds.start()
					.ifPresent(arg3x -> arg.playSound(null, arg2.getX(), arg2.getY(), arg2.getZ(), (SoundEvent)arg3x.value(), SoundSource.PLAYERS, 0.5F, 1.0F));
			}

			if (f >= 0.5F && !this.midLoadSoundPlayed) {
				this.midLoadSoundPlayed = true;
				crossbowitem$chargingsounds.mid()
					.ifPresent(arg3x -> arg.playSound(null, arg2.getX(), arg2.getY(), arg2.getZ(), (SoundEvent)arg3x.value(), SoundSource.PLAYERS, 0.5F, 1.0F));
			}
		}
	}

	@Override
	public int getUseDuration(ItemStack arg, LivingEntity arg2) {
		return getChargeDuration(arg, arg2) + 3;
	}

	public static int getChargeDuration(ItemStack arg, LivingEntity arg2) {
		float f = EnchantmentHelper.modifyCrossbowChargingTime(arg, arg2, 1.25F);
		return Mth.floor(f * 20.0F);
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack arg) {
		return ItemUseAnimation.CROSSBOW;
	}

	CrossbowItem.ChargingSounds getChargingSounds(ItemStack arg) {
		return (CrossbowItem.ChargingSounds)EnchantmentHelper.pickHighestLevel(arg, EnchantmentEffectComponents.CROSSBOW_CHARGING_SOUNDS).orElse(DEFAULT_SOUNDS);
	}

	private static float getPowerForTime(int i, ItemStack arg, LivingEntity arg2) {
		float f = (float)i / getChargeDuration(arg, arg2);
		if (f > 1.0F) {
			f = 1.0F;
		}

		return f;
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list2, TooltipFlag arg3) {
		ChargedProjectiles chargedprojectiles = arg.get(DataComponents.CHARGED_PROJECTILES);
		if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
			ItemStack itemstack = (ItemStack)chargedprojectiles.getItems().get(0);
			list2.add(Component.translatable("item.minecraft.crossbow.projectile").append(CommonComponents.SPACE).append(itemstack.getDisplayName()));
			if (arg3.isAdvanced() && itemstack.is(Items.FIREWORK_ROCKET)) {
				List<Component> list = Lists.<Component>newArrayList();
				Items.FIREWORK_ROCKET.appendHoverText(itemstack, arg2, list, arg3);
				if (!list.isEmpty()) {
					for (int i = 0; i < list.size(); i++) {
						list.set(i, Component.literal("  ").append((Component)list.get(i)).withStyle(ChatFormatting.GRAY));
					}

					list2.addAll(list);
				}
			}
		}
	}

	@Override
	public boolean useOnRelease(ItemStack arg) {
		return arg.is(this);
	}

	@Override
	public int getDefaultProjectileRange() {
		return 8;
	}

	public record ChargingSounds(Optional<Holder<SoundEvent>> start, Optional<Holder<SoundEvent>> mid, Optional<Holder<SoundEvent>> end) {
		public static final Codec<CrossbowItem.ChargingSounds> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					SoundEvent.CODEC.optionalFieldOf("start").forGetter(CrossbowItem.ChargingSounds::start),
					SoundEvent.CODEC.optionalFieldOf("mid").forGetter(CrossbowItem.ChargingSounds::mid),
					SoundEvent.CODEC.optionalFieldOf("end").forGetter(CrossbowItem.ChargingSounds::end)
				)
				.apply(instance, CrossbowItem.ChargingSounds::new)
		);
	}
}
