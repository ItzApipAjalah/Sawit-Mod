package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FireworkRocketItem extends Item implements ProjectileItem {
	public static final byte[] CRAFTABLE_DURATIONS = new byte[]{1, 2, 3};
	public static final double ROCKET_PLACEMENT_OFFSET = 0.15;

	public FireworkRocketItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		Level level = arg.getLevel();
		if (level instanceof ServerLevel serverLevel) {
			ItemStack itemStack = arg.getItemInHand();
			Vec3 vec3 = arg.getClickLocation();
			Direction direction = arg.getClickedFace();
			Projectile.spawnProjectile(
				new FireworkRocketEntity(
					level, arg.getPlayer(), vec3.x + direction.getStepX() * 0.15, vec3.y + direction.getStepY() * 0.15, vec3.z + direction.getStepZ() * 0.15, itemStack
				),
				serverLevel,
				itemStack
			);
			itemStack.shrink(1);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		if (arg2.isFallFlying()) {
			ItemStack itemStack = arg2.getItemInHand(arg3);
			if (arg instanceof ServerLevel serverLevel) {
				Projectile.spawnProjectile(new FireworkRocketEntity(arg, itemStack, arg2), serverLevel, itemStack);
				itemStack.consume(1, arg2);
				arg2.awardStat(Stats.ITEM_USED.get(this));
			}

			return InteractionResult.SUCCESS;
		} else {
			return InteractionResult.PASS;
		}
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		Fireworks fireworks = arg.get(DataComponents.FIREWORKS);
		if (fireworks != null) {
			fireworks.addToTooltip(arg2, list::add, arg3);
		}
	}

	@Override
	public Projectile asProjectile(Level arg, Position arg2, ItemStack arg3, Direction arg4) {
		return new FireworkRocketEntity(arg, arg3.copyWithCount(1), arg2.x(), arg2.y(), arg2.z(), true);
	}

	@Override
	public ProjectileItem.DispenseConfig createDispenseConfig() {
		return ProjectileItem.DispenseConfig.builder()
			.positionFunction(FireworkRocketItem::getEntityJustOutsideOfBlockPos)
			.uncertainty(1.0F)
			.power(0.5F)
			.overrideDispenseEvent(1004)
			.build();
	}

	private static Vec3 getEntityJustOutsideOfBlockPos(BlockSource arg, Direction arg2) {
		return arg.center().add(arg2.getStepX() * 0.5000099999997474, arg2.getStepY() * 0.5000099999997474, arg2.getStepZ() * 0.5000099999997474);
	}
}
