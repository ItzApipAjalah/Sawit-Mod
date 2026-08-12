package net.minecraft.world.item.trading;

import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;

public interface Merchant {
	void setTradingPlayer(@Nullable Player arg);

	@Nullable
	Player getTradingPlayer();

	MerchantOffers getOffers();

	void overrideOffers(MerchantOffers arg);

	void notifyTrade(MerchantOffer arg);

	void notifyTradeUpdated(ItemStack arg);

	int getVillagerXp();

	void overrideXp(int i);

	boolean showProgressBar();

	SoundEvent getNotifyTradeSound();

	default boolean canRestock() {
		return false;
	}

	default void openTradingScreen(Player arg, Component arg2, int i) {
		OptionalInt optionalInt = arg.openMenu(new SimpleMenuProvider((ix, argx, arg2x) -> new MerchantMenu(ix, argx, this), arg2));
		if (optionalInt.isPresent()) {
			MerchantOffers merchantOffers = this.getOffers();
			if (!merchantOffers.isEmpty()) {
				arg.sendMerchantOffers(optionalInt.getAsInt(), merchantOffers, i, this.getVillagerXp(), this.showProgressBar(), this.canRestock());
			}
		}
	}

	boolean isClientSide();
}
