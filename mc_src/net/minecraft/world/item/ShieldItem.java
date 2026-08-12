package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class ShieldItem extends Item {
	public static final int EFFECTIVE_BLOCK_DELAY = 5;
	public static final float MINIMUM_DURABILITY_DAMAGE = 3.0F;

	public ShieldItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public Component getName(ItemStack arg) {
		DyeColor dyecolor = arg.get(DataComponents.BASE_COLOR);
		return (Component)(dyecolor != null ? Component.translatable(this.descriptionId + "." + dyecolor.getName()) : super.getName(arg));
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		BannerItem.appendHoverTextFromBannerBlockEntityTag(arg, list);
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack arg) {
		return ItemUseAnimation.BLOCK;
	}

	@Override
	public int getUseDuration(ItemStack arg, LivingEntity arg2) {
		return 72000;
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		arg2.startUsingItem(arg3);
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
		return ItemAbilities.DEFAULT_SHIELD_ACTIONS.contains(itemAbility);
	}
}
