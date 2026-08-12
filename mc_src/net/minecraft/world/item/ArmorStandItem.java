package net.minecraft.world.item;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArmorStandItem extends Item {
	public ArmorStandItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Direction direction = arg.getClickedFace();
		if (direction == Direction.DOWN) {
			return InteractionResult.FAIL;
		} else {
			Level level = arg.getLevel();
			BlockPlaceContext blockPlaceContext = new BlockPlaceContext(arg);
			BlockPos blockPos = blockPlaceContext.getClickedPos();
			ItemStack itemStack = arg.getItemInHand();
			Vec3 vec3 = Vec3.atBottomCenterOf(blockPos);
			AABB aABB = EntityType.ARMOR_STAND.getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());
			if (level.noCollision(null, aABB) && level.getEntities(null, aABB).isEmpty()) {
				if (level instanceof ServerLevel serverLevel) {
					Consumer<ArmorStand> consumer = EntityType.createDefaultStackConfig(serverLevel, itemStack, arg.getPlayer());
					ArmorStand armorStand = EntityType.ARMOR_STAND.create(serverLevel, consumer, blockPos, EntitySpawnReason.SPAWN_ITEM_USE, true, true);
					if (armorStand == null) {
						return InteractionResult.FAIL;
					}

					float f = Mth.floor((Mth.wrapDegrees(arg.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
					armorStand.moveTo(armorStand.getX(), armorStand.getY(), armorStand.getZ(), f, 0.0F);
					serverLevel.addFreshEntityWithPassengers(armorStand);
					level.playSound(null, armorStand.getX(), armorStand.getY(), armorStand.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
					armorStand.gameEvent(GameEvent.ENTITY_PLACE, arg.getPlayer());
				}

				itemStack.shrink(1);
				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.FAIL;
			}
		}
	}
}
