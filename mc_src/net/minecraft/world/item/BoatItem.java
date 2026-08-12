package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BoatItem extends Item {
	private final EntityType<? extends AbstractBoat> entityType;

	public BoatItem(EntityType<? extends AbstractBoat> arg, Item.Properties arg2) {
		super(arg2);
		this.entityType = arg;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemStack = arg2.getItemInHand(arg3);
		HitResult hitResult = getPlayerPOVHitResult(arg, arg2, ClipContext.Fluid.ANY);
		if (hitResult.getType() == HitResult.Type.MISS) {
			return InteractionResult.PASS;
		} else {
			Vec3 vec3 = arg2.getViewVector(1.0F);
			double d = 5.0;
			List<Entity> list = arg.getEntities(arg2, arg2.getBoundingBox().expandTowards(vec3.scale(5.0)).inflate(1.0), EntitySelector.CAN_BE_PICKED);
			if (!list.isEmpty()) {
				Vec3 vec32 = arg2.getEyePosition();

				for (Entity entity : list) {
					AABB aABB = entity.getBoundingBox().inflate(entity.getPickRadius());
					if (aABB.contains(vec32)) {
						return InteractionResult.PASS;
					}
				}
			}

			if (hitResult.getType() == HitResult.Type.BLOCK) {
				AbstractBoat abstractBoat = this.getBoat(arg, hitResult, itemStack, arg2);
				if (abstractBoat == null) {
					return InteractionResult.FAIL;
				} else {
					abstractBoat.setYRot(arg2.getYRot());
					if (!arg.noCollision(abstractBoat, abstractBoat.getBoundingBox())) {
						return InteractionResult.FAIL;
					} else {
						if (!arg.isClientSide) {
							arg.addFreshEntity(abstractBoat);
							arg.gameEvent(arg2, GameEvent.ENTITY_PLACE, hitResult.getLocation());
							itemStack.consume(1, arg2);
						}

						arg2.awardStat(Stats.ITEM_USED.get(this));
						return InteractionResult.SUCCESS;
					}
				}
			} else {
				return InteractionResult.PASS;
			}
		}
	}

	@Nullable
	private AbstractBoat getBoat(Level arg, HitResult arg2, ItemStack arg3, Player arg4) {
		AbstractBoat abstractBoat = this.entityType.create(arg, EntitySpawnReason.SPAWN_ITEM_USE);
		if (abstractBoat != null) {
			Vec3 vec3 = arg2.getLocation();
			abstractBoat.setInitialPos(vec3.x, vec3.y, vec3.z);
			if (arg instanceof ServerLevel serverLevel) {
				EntityType.createDefaultStackConfig(serverLevel, arg3, arg4).accept(abstractBoat);
			}
		}

		return abstractBoat;
	}
}
