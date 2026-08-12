package net.minecraft.world.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BannerPattern;

public class BannerPatternItem extends Item {
	private final TagKey<BannerPattern> bannerPattern;

	public BannerPatternItem(TagKey<BannerPattern> arg, Item.Properties arg2) {
		super(arg2);
		this.bannerPattern = arg;
	}

	public TagKey<BannerPattern> getBannerPattern() {
		return this.bannerPattern;
	}
}
