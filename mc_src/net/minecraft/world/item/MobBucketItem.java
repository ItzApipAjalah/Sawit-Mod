package net.minecraft.world.item;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;

public class MobBucketItem extends BucketItem {
	private static final MapCodec<TropicalFish.Variant> VARIANT_FIELD_CODEC = TropicalFish.Variant.CODEC.fieldOf("BucketVariantTag");
	private final EntityType<?> type;
	private final SoundEvent emptySound;

	public MobBucketItem(EntityType<?> arg, Fluid arg2, SoundEvent arg3, Item.Properties arg4) {
		super(arg2, arg4);
		this.type = arg;
		this.emptySound = arg3;
	}

	@Override
	public void checkExtraContent(@Nullable Player arg, Level arg2, ItemStack arg3, BlockPos arg4) {
		if (arg2 instanceof ServerLevel) {
			this.spawn((ServerLevel)arg2, arg3, arg4);
			arg2.gameEvent(arg, GameEvent.ENTITY_PLACE, arg4);
		}
	}

	@Override
	protected void playEmptySound(@Nullable Player arg, LevelAccessor arg2, BlockPos arg3) {
		arg2.playSound(arg, arg3, this.emptySound, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	private void spawn(ServerLevel arg, ItemStack arg2, BlockPos arg3) {
		Entity entity = this.type.create(arg, EntityType.createDefaultStackConfig(arg, arg2, null), arg3, EntitySpawnReason.BUCKET, true, false);
		if (entity instanceof Bucketable bucketable) {
			CustomData customData = arg2.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
			bucketable.loadFromBucketTag(customData.copyTag());
			bucketable.setFromBucket(true);
		}

		if (entity != null) {
			arg.addFreshEntityWithPassengers(entity);
		}
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		if (this.type == EntityType.TROPICAL_FISH) {
			CustomData customData = arg.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY);
			if (customData.isEmpty()) {
				return;
			}

			Optional<TropicalFish.Variant> optional = customData.read(VARIANT_FIELD_CODEC).result();
			if (optional.isPresent()) {
				TropicalFish.Variant variant = (TropicalFish.Variant)optional.get();
				ChatFormatting[] chatFormattings = new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY};
				String string = "color.minecraft." + variant.baseColor();
				String string2 = "color.minecraft." + variant.patternColor();
				int i = TropicalFish.COMMON_VARIANTS.indexOf(variant);
				if (i != -1) {
					list.add(Component.translatable(TropicalFish.getPredefinedName(i)).withStyle(chatFormattings));
					return;
				}

				list.add(variant.pattern().displayName().plainCopy().withStyle(chatFormattings));
				MutableComponent mutableComponent = Component.translatable(string);
				if (!string.equals(string2)) {
					mutableComponent.append(", ").append(Component.translatable(string2));
				}

				mutableComponent.withStyle(chatFormattings);
				list.add(mutableComponent);
			}
		}
	}
}
