package net.minecraft.world.item.crafting.display;

import net.minecraft.core.Registry;

public class SlotDisplays {
	public static SlotDisplay.Type<?> bootstrap(Registry<SlotDisplay.Type<?>> arg) {
		Registry.register(arg, "empty", SlotDisplay.Empty.TYPE);
		Registry.register(arg, "any_fuel", SlotDisplay.AnyFuel.TYPE);
		Registry.register(arg, "item", SlotDisplay.ItemSlotDisplay.TYPE);
		Registry.register(arg, "item_stack", SlotDisplay.ItemStackSlotDisplay.TYPE);
		Registry.register(arg, "tag", SlotDisplay.TagSlotDisplay.TYPE);
		Registry.register(arg, "smithing_trim", SlotDisplay.SmithingTrimDemoSlotDisplay.TYPE);
		Registry.register(arg, "with_remainder", SlotDisplay.WithRemainder.TYPE);
		return Registry.register(arg, "composite", SlotDisplay.Composite.TYPE);
	}
}
