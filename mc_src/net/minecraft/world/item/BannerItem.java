package net.minecraft.world.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.apache.commons.lang3.Validate;

public class BannerItem extends StandingAndWallBlockItem {
	public BannerItem(Block arg, Block arg2, Item.Properties arg3) {
		super(arg, arg2, Direction.DOWN, arg3);
		Validate.isInstanceOf(AbstractBannerBlock.class, arg);
		Validate.isInstanceOf(AbstractBannerBlock.class, arg2);
	}

	public static void appendHoverTextFromBannerBlockEntityTag(ItemStack arg, List<Component> list) {
		BannerPatternLayers bannerPatternLayers = arg.get(DataComponents.BANNER_PATTERNS);
		if (bannerPatternLayers != null) {
			for (int i = 0; i < Math.min(bannerPatternLayers.layers().size(), 6); i++) {
				BannerPatternLayers.Layer layer = (BannerPatternLayers.Layer)bannerPatternLayers.layers().get(i);
				list.add(layer.description().withStyle(ChatFormatting.GRAY));
			}
		}
	}

	public DyeColor getColor() {
		return ((AbstractBannerBlock)this.getBlock()).getColor();
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		appendHoverTextFromBannerBlockEntityTag(arg, list);
	}
}
