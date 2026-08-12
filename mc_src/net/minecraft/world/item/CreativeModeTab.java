package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.EventHooks;

public class CreativeModeTab {
	private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
	private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
	static final ResourceLocation DEFAULT_BACKGROUND = createTextureLocation("items");
	private final Component displayName;
	ResourceLocation backgroundTexture = DEFAULT_BACKGROUND;
	boolean canScroll = true;
	boolean showTitle = true;
	boolean alignedRight = false;
	private final CreativeModeTab.Row row;
	private final int column;
	private final CreativeModeTab.Type type;
	@Nullable
	private ItemStack iconItemStack;
	private Collection<ItemStack> displayItems = ItemStackLinkedSet.createTypeAndComponentsSet();
	private Set<ItemStack> displayItemsSearchTab = ItemStackLinkedSet.createTypeAndComponentsSet();
	private final Supplier<ItemStack> iconGenerator;
	private final CreativeModeTab.DisplayItemsGenerator displayItemsGenerator;
	@Nullable
	private final ResourceLocation scrollerSpriteLocation;
	private final boolean hasSearchBar;
	private final int searchBarWidth;
	private final ResourceLocation tabsImage;
	private final int labelColor;
	private final int slotColor;
	public final List<ResourceLocation> tabsBefore;
	public final List<ResourceLocation> tabsAfter;

	CreativeModeTab(
		CreativeModeTab.Row arg,
		int i,
		CreativeModeTab.Type arg2,
		Component arg3,
		Supplier<ItemStack> supplier,
		CreativeModeTab.DisplayItemsGenerator arg4,
		ResourceLocation scrollerSpriteLocation,
		boolean hasSearchBar,
		int searchBarWidth,
		ResourceLocation tabsImage,
		int labelColor,
		int slotColor,
		List<ResourceLocation> tabsBefore,
		List<ResourceLocation> tabsAfter
	) {
		this.row = arg;
		this.column = i;
		this.displayName = arg3;
		this.iconGenerator = supplier;
		this.displayItemsGenerator = arg4;
		this.type = arg2;
		this.scrollerSpriteLocation = scrollerSpriteLocation;
		this.hasSearchBar = hasSearchBar;
		this.searchBarWidth = searchBarWidth;
		this.tabsImage = tabsImage;
		this.labelColor = labelColor;
		this.slotColor = slotColor;
		this.tabsBefore = List.copyOf(tabsBefore);
		this.tabsAfter = List.copyOf(tabsAfter);
	}

	protected CreativeModeTab(CreativeModeTab.Builder builder) {
		this(
			builder.row,
			builder.column,
			builder.type,
			builder.displayName,
			builder.iconGenerator,
			builder.displayItemsGenerator,
			builder.spriteScrollerLocation,
			builder.hasSearchBar,
			builder.searchBarWidth,
			builder.tabsImage,
			builder.labelColor,
			builder.slotColor,
			builder.tabsBefore,
			builder.tabsAfter
		);
	}

	public static CreativeModeTab.Builder builder() {
		return new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0);
	}

	public static ResourceLocation createTextureLocation(String string) {
		return ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tab_" + string + ".png");
	}

	@Deprecated
	public static CreativeModeTab.Builder builder(CreativeModeTab.Row arg, int i) {
		return new CreativeModeTab.Builder(arg, i);
	}

	public Component getDisplayName() {
		return this.displayName;
	}

	public ItemStack getIconItem() {
		if (this.iconItemStack == null) {
			this.iconItemStack = (ItemStack)this.iconGenerator.get();
		}

		return this.iconItemStack;
	}

	public ResourceLocation getBackgroundTexture() {
		return this.backgroundTexture;
	}

	public boolean showTitle() {
		return this.showTitle;
	}

	public boolean canScroll() {
		return this.canScroll;
	}

	public int column() {
		return this.column;
	}

	public CreativeModeTab.Row row() {
		return this.row;
	}

	public boolean hasAnyItems() {
		return !this.displayItems.isEmpty();
	}

	public boolean shouldDisplay() {
		return this.type != CreativeModeTab.Type.CATEGORY || this.hasAnyItems();
	}

	public boolean isAlignedRight() {
		return this.alignedRight;
	}

	public CreativeModeTab.Type getType() {
		return this.type;
	}

	public void buildContents(CreativeModeTab.ItemDisplayParameters arg) {
		CreativeModeTab.ItemDisplayBuilder creativemodetab$itemdisplaybuilder = new CreativeModeTab.ItemDisplayBuilder(this, arg.enabledFeatures);
		ResourceKey<CreativeModeTab> resourcekey = (ResourceKey<CreativeModeTab>)BuiltInRegistries.CREATIVE_MODE_TAB
			.getResourceKey(this)
			.orElseThrow(() -> new IllegalStateException("Unregistered creative tab: " + this));
		EventHooks.onCreativeModeTabBuildContents(this, resourcekey, this.displayItemsGenerator, arg, creativemodetab$itemdisplaybuilder);
		this.displayItems = creativemodetab$itemdisplaybuilder.tabContents;
		this.displayItemsSearchTab = creativemodetab$itemdisplaybuilder.searchTabContents;
	}

	public Collection<ItemStack> getDisplayItems() {
		return this.displayItems;
	}

	public Collection<ItemStack> getSearchTabDisplayItems() {
		return this.displayItemsSearchTab;
	}

	public boolean contains(ItemStack arg) {
		return this.displayItemsSearchTab.contains(arg);
	}

	public boolean hasSearchBar() {
		return this.hasSearchBar;
	}

	public int getSearchBarWidth() {
		return this.searchBarWidth;
	}

	public ResourceLocation getTabsImage() {
		return this.tabsImage;
	}

	public int getLabelColor() {
		return this.labelColor;
	}

	public int getSlotColor() {
		return this.slotColor;
	}

	public ResourceLocation getScrollerSprite() {
		if (this.scrollerSpriteLocation == null) {
			return this.canScroll() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
		} else {
			return this.scrollerSpriteLocation;
		}
	}

	public static class Builder {
		private static final CreativeModeTab.DisplayItemsGenerator EMPTY_GENERATOR = (arg, arg2) -> {};
		private static final ResourceLocation CREATIVE_INVENTORY_TABS_IMAGE = ResourceLocation.withDefaultNamespace(
			"textures/gui/container/creative_inventory/tabs.png"
		);
		private static final ResourceLocation CREATIVE_ITEM_SEARCH_BACKGROUND = CreativeModeTab.createTextureLocation("item_search");
		private final CreativeModeTab.Row row;
		private final int column;
		private Component displayName = Component.empty();
		private Supplier<ItemStack> iconGenerator = () -> ItemStack.EMPTY;
		private CreativeModeTab.DisplayItemsGenerator displayItemsGenerator = EMPTY_GENERATOR;
		private boolean canScroll = true;
		private boolean showTitle = true;
		private boolean alignedRight = false;
		private CreativeModeTab.Type type = CreativeModeTab.Type.CATEGORY;
		private ResourceLocation backgroundTexture = CreativeModeTab.DEFAULT_BACKGROUND;
		@org.jetbrains.annotations.Nullable
		private ResourceLocation spriteScrollerLocation;
		private boolean hasSearchBar = false;
		private int searchBarWidth = 89;
		private ResourceLocation tabsImage = CREATIVE_INVENTORY_TABS_IMAGE;
		private int labelColor = 4210752;
		private int slotColor = -2130706433;
		private Function<CreativeModeTab.Builder, CreativeModeTab> tabFactory = CreativeModeTab::new;
		private final List<ResourceLocation> tabsBefore = new ArrayList();
		private final List<ResourceLocation> tabsAfter = new ArrayList();

		public Builder(CreativeModeTab.Row arg, int i) {
			this.row = arg;
			this.column = i;
		}

		public CreativeModeTab.Builder title(Component arg) {
			this.displayName = arg;
			return this;
		}

		public CreativeModeTab.Builder icon(Supplier<ItemStack> supplier) {
			this.iconGenerator = supplier;
			return this;
		}

		public CreativeModeTab.Builder displayItems(CreativeModeTab.DisplayItemsGenerator arg) {
			this.displayItemsGenerator = arg;
			return this;
		}

		public CreativeModeTab.Builder alignedRight() {
			this.alignedRight = true;
			return this;
		}

		public CreativeModeTab.Builder hideTitle() {
			this.showTitle = false;
			return this;
		}

		public CreativeModeTab.Builder noScrollBar() {
			this.canScroll = false;
			return this;
		}

		protected CreativeModeTab.Builder type(CreativeModeTab.Type arg) {
			this.type = arg;
			return arg == CreativeModeTab.Type.SEARCH ? this.withSearchBar() : this;
		}

		public CreativeModeTab.Builder backgroundTexture(ResourceLocation arg) {
			this.backgroundTexture = arg;
			return this;
		}

		public CreativeModeTab.Builder withSearchBar() {
			this.hasSearchBar = true;
			return this.backgroundTexture == CreativeModeTab.DEFAULT_BACKGROUND ? this.backgroundTexture(CREATIVE_ITEM_SEARCH_BACKGROUND) : this;
		}

		public CreativeModeTab.Builder withSearchBar(int searchBarWidth) {
			this.searchBarWidth = searchBarWidth;
			return this.withSearchBar();
		}

		public CreativeModeTab.Builder withScrollBarSpriteLocation(ResourceLocation scrollBarSpriteLocation) {
			this.spriteScrollerLocation = scrollBarSpriteLocation;
			return this;
		}

		public CreativeModeTab.Builder withTabsImage(ResourceLocation tabsImage) {
			this.tabsImage = tabsImage;
			return this;
		}

		public CreativeModeTab.Builder withLabelColor(int labelColor) {
			this.labelColor = labelColor;
			return this;
		}

		public CreativeModeTab.Builder withSlotColor(int slotColor) {
			this.slotColor = slotColor;
			return this;
		}

		public CreativeModeTab.Builder withTabFactory(Function<CreativeModeTab.Builder, CreativeModeTab> tabFactory) {
			this.tabFactory = tabFactory;
			return this;
		}

		public CreativeModeTab.Builder withTabsBefore(ResourceLocation... tabs) {
			this.tabsBefore.addAll(List.of(tabs));
			return this;
		}

		public CreativeModeTab.Builder withTabsAfter(ResourceLocation... tabs) {
			this.tabsAfter.addAll(List.of(tabs));
			return this;
		}

		@SafeVarargs
		public final CreativeModeTab.Builder withTabsBefore(ResourceKey<CreativeModeTab>... tabs) {
			Stream.of(tabs).map(ResourceKey::location).forEach(this.tabsBefore::add);
			return this;
		}

		@SafeVarargs
		public final CreativeModeTab.Builder withTabsAfter(ResourceKey<CreativeModeTab>... tabs) {
			Stream.of(tabs).map(ResourceKey::location).forEach(this.tabsAfter::add);
			return this;
		}

		public CreativeModeTab.Builder displayItems(Collection<? extends Holder<? extends ItemLike>> collection) {
			return this.displayItems(
				(CreativeModeTab.DisplayItemsGenerator)((p, o) -> collection.stream()
					.map(Holder::value)
					.map(ItemLike::asItem)
					.filter(i -> i != Items.AIR)
					.filter(i -> i.isEnabled(p.enabledFeatures()))
					.forEach(o::accept))
			);
		}

		public CreativeModeTab build() {
			if ((this.type == CreativeModeTab.Type.HOTBAR || this.type == CreativeModeTab.Type.INVENTORY) && this.displayItemsGenerator != EMPTY_GENERATOR) {
				throw new IllegalStateException("Special tabs can't have display items");
			} else {
				CreativeModeTab creativemodetab = (CreativeModeTab)this.tabFactory.apply(this);
				creativemodetab.alignedRight = this.alignedRight;
				creativemodetab.showTitle = this.showTitle;
				creativemodetab.canScroll = this.canScroll;
				creativemodetab.backgroundTexture = this.backgroundTexture;
				return creativemodetab;
			}
		}
	}

	@FunctionalInterface
	public interface DisplayItemsGenerator {
		void accept(CreativeModeTab.ItemDisplayParameters arg, CreativeModeTab.Output arg2);
	}

	static class ItemDisplayBuilder implements CreativeModeTab.Output {
		public final Collection<ItemStack> tabContents = ItemStackLinkedSet.createTypeAndComponentsSet();
		public final Set<ItemStack> searchTabContents = ItemStackLinkedSet.createTypeAndComponentsSet();
		private final CreativeModeTab tab;
		private final FeatureFlagSet featureFlagSet;

		public ItemDisplayBuilder(CreativeModeTab arg, FeatureFlagSet arg2) {
			this.tab = arg;
			this.featureFlagSet = arg2;
		}

		@Override
		public void accept(ItemStack arg, CreativeModeTab.TabVisibility arg2) {
			if (arg.getCount() != 1) {
				throw new IllegalArgumentException("Stack size must be exactly 1");
			} else {
				boolean flag = this.tabContents.contains(arg) && arg2 != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY;
				if (flag) {
					throw new IllegalStateException(
						"Accidentally adding the same item stack twice " + arg.getDisplayName().getString() + " to a Creative Mode Tab: " + this.tab.getDisplayName().getString()
					);
				} else {
					if (arg.getItem().isEnabled(this.featureFlagSet)) {
						switch (arg2) {
							case PARENT_AND_SEARCH_TABS:
								this.tabContents.add(arg);
								this.searchTabContents.add(arg);
								break;
							case PARENT_TAB_ONLY:
								this.tabContents.add(arg);
								break;
							case SEARCH_TAB_ONLY:
								this.searchTabContents.add(arg);
						}
					}
				}
			}
		}
	}

	public record ItemDisplayParameters(FeatureFlagSet enabledFeatures, boolean hasPermissions, HolderLookup.Provider holders) {
		public boolean needsUpdate(FeatureFlagSet arg, boolean bl, HolderLookup.Provider arg2) {
			return !this.enabledFeatures.equals(arg) || this.hasPermissions != bl || this.holders != arg2;
		}
	}

	public interface Output {
		void accept(ItemStack arg, CreativeModeTab.TabVisibility arg2);

		default void accept(ItemStack arg) {
			this.accept(arg, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		}

		default void accept(ItemLike arg, CreativeModeTab.TabVisibility arg2) {
			this.accept(new ItemStack(arg), arg2);
		}

		default void accept(ItemLike arg) {
			this.accept(new ItemStack(arg), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		}

		default void acceptAll(Collection<ItemStack> collection, CreativeModeTab.TabVisibility arg) {
			collection.forEach(arg2 -> this.accept(arg2, arg));
		}

		default void acceptAll(Collection<ItemStack> collection) {
			this.acceptAll(collection, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		}
	}

	public static enum Row {
		TOP,
		BOTTOM;
	}

	public static enum TabVisibility {
		PARENT_AND_SEARCH_TABS,
		PARENT_TAB_ONLY,
		SEARCH_TAB_ONLY;
	}

	public static enum Type {
		CATEGORY,
		INVENTORY,
		HOTBAR,
		SEARCH;
	}
}
