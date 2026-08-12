package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.UseCooldown;

public class ItemCooldowns {
	private final Map<ResourceLocation, ItemCooldowns.CooldownInstance> cooldowns = Maps.<ResourceLocation, ItemCooldowns.CooldownInstance>newHashMap();
	private int tickCount;

	public boolean isOnCooldown(ItemStack arg) {
		return this.getCooldownPercent(arg, 0.0F) > 0.0F;
	}

	public float getCooldownPercent(ItemStack arg, float f) {
		ResourceLocation resourceLocation = this.getCooldownGroup(arg);
		ItemCooldowns.CooldownInstance cooldownInstance = (ItemCooldowns.CooldownInstance)this.cooldowns.get(resourceLocation);
		if (cooldownInstance != null) {
			float g = cooldownInstance.endTime - cooldownInstance.startTime;
			float h = cooldownInstance.endTime - (this.tickCount + f);
			return Mth.clamp(h / g, 0.0F, 1.0F);
		} else {
			return 0.0F;
		}
	}

	public void tick() {
		this.tickCount++;
		if (!this.cooldowns.isEmpty()) {
			Iterator<Entry<ResourceLocation, ItemCooldowns.CooldownInstance>> iterator = this.cooldowns.entrySet().iterator();

			while (iterator.hasNext()) {
				Entry<ResourceLocation, ItemCooldowns.CooldownInstance> entry = (Entry<ResourceLocation, ItemCooldowns.CooldownInstance>)iterator.next();
				if (((ItemCooldowns.CooldownInstance)entry.getValue()).endTime <= this.tickCount) {
					iterator.remove();
					this.onCooldownEnded((ResourceLocation)entry.getKey());
				}
			}
		}
	}

	public ResourceLocation getCooldownGroup(ItemStack arg) {
		UseCooldown useCooldown = arg.get(DataComponents.USE_COOLDOWN);
		ResourceLocation resourceLocation = BuiltInRegistries.ITEM.getKey(arg.getItem());
		return useCooldown == null ? resourceLocation : (ResourceLocation)useCooldown.cooldownGroup().orElse(resourceLocation);
	}

	public void addCooldown(ItemStack arg, int i) {
		this.addCooldown(this.getCooldownGroup(arg), i);
	}

	public void addCooldown(ResourceLocation arg, int i) {
		this.cooldowns.put(arg, new ItemCooldowns.CooldownInstance(this.tickCount, this.tickCount + i));
		this.onCooldownStarted(arg, i);
	}

	public void removeCooldown(ResourceLocation arg) {
		this.cooldowns.remove(arg);
		this.onCooldownEnded(arg);
	}

	protected void onCooldownStarted(ResourceLocation arg, int i) {
	}

	protected void onCooldownEnded(ResourceLocation arg) {
	}

	record CooldownInstance(int startTime, int endTime) {
	}
}
