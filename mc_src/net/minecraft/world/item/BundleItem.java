package net.minecraft.world.item;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.math.Fraction;

public class BundleItem extends Item {
	public static final int MAX_SHOWN_GRID_ITEMS_X = 4;
	public static final int MAX_SHOWN_GRID_ITEMS_Y = 3;
	public static final int MAX_SHOWN_GRID_ITEMS = 12;
	public static final int OVERFLOWING_MAX_SHOWN_GRID_ITEMS = 11;
	private static final int FULL_BAR_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.33F, 0.33F);
	private static final int BAR_COLOR = ARGB.colorFromFloat(1.0F, 0.44F, 0.53F, 1.0F);
	private static final int TICKS_AFTER_FIRST_THROW = 10;
	private static final int TICKS_BETWEEN_THROWS = 2;
	private static final int TICKS_MAX_THROW_DURATION = 200;
	private final ResourceLocation openFrontModel;
	private final ResourceLocation openBackModel;

	public BundleItem(ResourceLocation arg, ResourceLocation arg2, Item.Properties arg3) {
		super(arg3);
		this.openFrontModel = arg;
		this.openBackModel = arg2;
	}

	public static float getFullnessDisplay(ItemStack arg) {
		BundleContents bundlecontents = arg.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
		return bundlecontents.weight().floatValue();
	}

	public ResourceLocation openFrontModel() {
		return this.openFrontModel;
	}

	public ResourceLocation openBackModel() {
		return this.openBackModel;
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack arg, Slot arg2, ClickAction arg3, Player arg4) {
		BundleContents bundlecontents = arg.get(DataComponents.BUNDLE_CONTENTS);
		if (bundlecontents != null && arg.getCount() == 1) {
			ItemStack itemstack = arg2.getItem();
			BundleContents.Mutable bundlecontents$mutable = new BundleContents.Mutable(bundlecontents);
			if (arg3 == ClickAction.PRIMARY && !itemstack.isEmpty()) {
				if (bundlecontents$mutable.tryTransfer(arg2, arg4) > 0) {
					playInsertSound(arg4);
				} else {
					playInsertFailSound(arg4);
				}

				arg.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
				this.broadcastChangesOnContainerMenu(arg4);
				return true;
			} else if (arg3 == ClickAction.SECONDARY && itemstack.isEmpty()) {
				ItemStack itemstack1 = bundlecontents$mutable.removeOne();
				if (itemstack1 != null) {
					ItemStack itemstack2 = arg2.safeInsert(itemstack1);
					if (itemstack2.getCount() > 0) {
						bundlecontents$mutable.tryInsert(itemstack2);
					} else {
						playRemoveOneSound(arg4);
					}
				}

				arg.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
				this.broadcastChangesOnContainerMenu(arg4);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack arg, ItemStack arg2, Slot arg3, ClickAction arg4, Player arg5, SlotAccess arg6) {
		if (arg.getCount() != 1) {
			return false;
		} else if (arg4 == ClickAction.PRIMARY && arg2.isEmpty()) {
			toggleSelectedItem(arg, -1);
			return false;
		} else {
			BundleContents bundlecontents = arg.get(DataComponents.BUNDLE_CONTENTS);
			if (bundlecontents == null) {
				return false;
			} else {
				BundleContents.Mutable bundlecontents$mutable = new BundleContents.Mutable(bundlecontents);
				if (arg4 == ClickAction.PRIMARY && !arg2.isEmpty()) {
					if (arg3.allowModification(arg5) && bundlecontents$mutable.tryInsert(arg2) > 0) {
						playInsertSound(arg5);
					} else {
						playInsertFailSound(arg5);
					}

					arg.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
					this.broadcastChangesOnContainerMenu(arg5);
					return true;
				} else if (arg4 == ClickAction.SECONDARY && arg2.isEmpty()) {
					if (arg3.allowModification(arg5)) {
						ItemStack itemstack = bundlecontents$mutable.removeOne();
						if (itemstack != null) {
							playRemoveOneSound(arg5);
							arg6.set(itemstack);
						}
					}

					arg.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
					this.broadcastChangesOnContainerMenu(arg5);
					return true;
				} else {
					toggleSelectedItem(arg, -1);
					return false;
				}
			}
		}
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		if (arg.isClientSide) {
			return InteractionResult.CONSUME;
		} else {
			arg2.startUsingItem(arg3);
			return InteractionResult.SUCCESS_SERVER;
		}
	}

	private void dropContent(Level arg, Player arg2, ItemStack arg3) {
		if (this.dropContent(arg3, arg2)) {
			playDropContentsSound(arg, arg2);
			arg2.awardStat(Stats.ITEM_USED.get(this));
		}
	}

	@Override
	public boolean isBarVisible(ItemStack arg) {
		BundleContents bundlecontents = arg.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
		return bundlecontents.weight().compareTo(Fraction.ZERO) > 0;
	}

	@Override
	public int getBarWidth(ItemStack arg) {
		BundleContents bundlecontents = arg.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
		return Math.min(1 + Mth.mulAndTruncate(bundlecontents.weight(), 12), 13);
	}

	@Override
	public int getBarColor(ItemStack arg) {
		BundleContents bundlecontents = arg.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
		return bundlecontents.weight().compareTo(Fraction.ONE) >= 0 ? FULL_BAR_COLOR : BAR_COLOR;
	}

	public static void toggleSelectedItem(ItemStack arg, int i) {
		BundleContents bundlecontents = arg.get(DataComponents.BUNDLE_CONTENTS);
		if (bundlecontents != null) {
			BundleContents.Mutable bundlecontents$mutable = new BundleContents.Mutable(bundlecontents);
			bundlecontents$mutable.toggleSelectedItem(i);
			arg.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
		}
	}

	public static boolean hasSelectedItem(ItemStack arg) {
		BundleContents bundlecontents = arg.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
		return bundlecontents.getSelectedItem() != -1;
	}

	public static int getSelectedItem(ItemStack arg) {
		BundleContents bundlecontents = arg.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
		return bundlecontents.getSelectedItem();
	}

	public static ItemStack getSelectedItemStack(ItemStack arg) {
		BundleContents bundlecontents = arg.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
		return bundlecontents.getItemUnsafe(bundlecontents.getSelectedItem());
	}

	public static int getNumberOfItemsToShow(ItemStack arg) {
		BundleContents bundlecontents = arg.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
		return bundlecontents.getNumberOfItemsToShow();
	}

	private boolean dropContent(ItemStack arg, Player arg2) {
		BundleContents bundlecontents = arg.get(DataComponents.BUNDLE_CONTENTS);
		if (bundlecontents != null && !bundlecontents.isEmpty()) {
			Optional<ItemStack> optional = removeOneItemFromBundle(arg, arg2, bundlecontents);
			if (optional.isPresent()) {
				arg2.drop((ItemStack)optional.get(), true);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	private static Optional<ItemStack> removeOneItemFromBundle(ItemStack arg, Player arg2, BundleContents arg3) {
		BundleContents.Mutable bundlecontents$mutable = new BundleContents.Mutable(arg3);
		ItemStack itemstack = bundlecontents$mutable.removeOne();
		if (itemstack != null) {
			playRemoveOneSound(arg2);
			arg.set(DataComponents.BUNDLE_CONTENTS, bundlecontents$mutable.toImmutable());
			return Optional.of(itemstack);
		} else {
			return Optional.empty();
		}
	}

	@Override
	public void onUseTick(Level arg, LivingEntity arg2, ItemStack arg3, int j) {
		if (!arg.isClientSide && arg2 instanceof Player player) {
			int i = this.getUseDuration(arg3, arg2);
			boolean flag = j == i;
			if (flag || j < i - 10 && j % 2 == 0) {
				this.dropContent(arg, player, arg3);
			}
		}
	}

	@Override
	public int getUseDuration(ItemStack arg, LivingEntity arg2) {
		return 200;
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack arg) {
		return !arg.has(DataComponents.HIDE_TOOLTIP) && !arg.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)
			? Optional.ofNullable(arg.get(DataComponents.BUNDLE_CONTENTS)).map(BundleTooltip::new)
			: Optional.empty();
	}

	@Override
	public void onDestroyed(ItemEntity arg) {
		BundleContents bundlecontents = arg.getItem().get(DataComponents.BUNDLE_CONTENTS);
		if (bundlecontents != null) {
			arg.getItem().set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
			ItemUtils.onContainerDestroyed(arg, bundlecontents.itemsCopy());
		}
	}

	public static List<BundleItem> getAllBundleItemColors() {
		return Stream.of(
				Items.BUNDLE,
				Items.WHITE_BUNDLE,
				Items.ORANGE_BUNDLE,
				Items.MAGENTA_BUNDLE,
				Items.LIGHT_BLUE_BUNDLE,
				Items.YELLOW_BUNDLE,
				Items.LIME_BUNDLE,
				Items.PINK_BUNDLE,
				Items.GRAY_BUNDLE,
				Items.LIGHT_GRAY_BUNDLE,
				Items.CYAN_BUNDLE,
				Items.BLACK_BUNDLE,
				Items.BROWN_BUNDLE,
				Items.GREEN_BUNDLE,
				Items.RED_BUNDLE,
				Items.BLUE_BUNDLE,
				Items.PURPLE_BUNDLE
			)
			.map(arg -> (BundleItem)arg)
			.toList();
	}

	public static Item getByColor(DyeColor arg) {
		return switch (arg) {
			case WHITE -> Items.WHITE_BUNDLE;
			case ORANGE -> Items.ORANGE_BUNDLE;
			case MAGENTA -> Items.MAGENTA_BUNDLE;
			case LIGHT_BLUE -> Items.LIGHT_BLUE_BUNDLE;
			case YELLOW -> Items.YELLOW_BUNDLE;
			case LIME -> Items.LIME_BUNDLE;
			case PINK -> Items.PINK_BUNDLE;
			case GRAY -> Items.GRAY_BUNDLE;
			case LIGHT_GRAY -> Items.LIGHT_GRAY_BUNDLE;
			case CYAN -> Items.CYAN_BUNDLE;
			case BLUE -> Items.BLUE_BUNDLE;
			case BROWN -> Items.BROWN_BUNDLE;
			case GREEN -> Items.GREEN_BUNDLE;
			case RED -> Items.RED_BUNDLE;
			case BLACK -> Items.BLACK_BUNDLE;
			case PURPLE -> Items.PURPLE_BUNDLE;
		};
	}

	private static void playRemoveOneSound(Entity arg) {
		arg.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + arg.level().getRandom().nextFloat() * 0.4F);
	}

	private static void playInsertSound(Entity arg) {
		arg.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + arg.level().getRandom().nextFloat() * 0.4F);
	}

	private static void playInsertFailSound(Entity arg) {
		arg.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
	}

	private static void playDropContentsSound(Level arg, Entity arg2) {
		arg.playSound(null, arg2.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8F, 0.8F + arg2.level().getRandom().nextFloat() * 0.4F);
	}

	private void broadcastChangesOnContainerMenu(Player arg) {
		AbstractContainerMenu abstractcontainermenu = arg.containerMenu;
		if (abstractcontainermenu != null) {
			abstractcontainermenu.slotsChanged(arg.getInventory());
		}
	}
}
