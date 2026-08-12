package net.minecraft.world.item.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedEntry;

public class EnchantmentInstance extends WeightedEntry.IntrusiveBase {
	public final Holder<Enchantment> enchantment;
	public final int level;

	public EnchantmentInstance(Holder<Enchantment> arg, int i) {
		super(((Enchantment)arg.value()).getWeight());
		this.enchantment = arg;
		this.level = i;
	}
}
