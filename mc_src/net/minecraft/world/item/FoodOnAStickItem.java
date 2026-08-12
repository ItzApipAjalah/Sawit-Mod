package net.minecraft.world.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FoodOnAStickItem<T extends Entity & ItemSteerable> extends Item {
	private final EntityType<T> canInteractWith;
	private final int consumeItemDamage;

	public FoodOnAStickItem(EntityType<T> arg, int i, Item.Properties arg2) {
		super(arg2);
		this.canInteractWith = arg;
		this.consumeItemDamage = i;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemStack = arg2.getItemInHand(arg3);
		if (arg.isClientSide) {
			return InteractionResult.PASS;
		} else {
			Entity entity = arg2.getControlledVehicle();
			if (arg2.isPassenger() && entity instanceof ItemSteerable itemSteerable && entity.getType() == this.canInteractWith && itemSteerable.boost()) {
				EquipmentSlot equipmentSlot = LivingEntity.getSlotForHand(arg3);
				ItemStack itemStack2 = itemStack.hurtAndConvertOnBreak(this.consumeItemDamage, Items.FISHING_ROD, arg2, equipmentSlot);
				return InteractionResult.SUCCESS_SERVER.heldItemTransformedTo(itemStack2);
			} else {
				arg2.awardStat(Stats.ITEM_USED.get(this));
				return InteractionResult.PASS;
			}
		}
	}
}
