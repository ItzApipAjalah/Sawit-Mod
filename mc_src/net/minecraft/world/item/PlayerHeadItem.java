package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

public class PlayerHeadItem extends StandingAndWallBlockItem {
	public PlayerHeadItem(Block arg, Block arg2, Item.Properties arg3) {
		super(arg, arg2, Direction.DOWN, arg3);
	}

	@Override
	public Component getName(ItemStack arg) {
		ResolvableProfile resolvableProfile = arg.get(DataComponents.PROFILE);
		return (Component)(resolvableProfile != null && resolvableProfile.name().isPresent()
			? Component.translatable(this.descriptionId + ".named", resolvableProfile.name().get())
			: super.getName(arg));
	}

	@Override
	public void verifyComponentsAfterLoad(ItemStack arg) {
		ResolvableProfile resolvableProfile = arg.get(DataComponents.PROFILE);
		if (resolvableProfile != null && !resolvableProfile.isResolved()) {
			resolvableProfile.resolve().thenAcceptAsync(arg2 -> arg.set(DataComponents.PROFILE, arg2), SkullBlockEntity.CHECKED_MAIN_THREAD_EXECUTOR);
		}
	}
}
