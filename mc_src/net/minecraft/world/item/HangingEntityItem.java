package net.minecraft.world.item;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class HangingEntityItem extends Item {
	private static final Component TOOLTIP_RANDOM_VARIANT = Component.translatable("painting.random").withStyle(ChatFormatting.GRAY);
	private final EntityType<? extends HangingEntity> type;

	public HangingEntityItem(EntityType<? extends HangingEntity> arg, Item.Properties arg2) {
		super(arg2);
		this.type = arg;
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		BlockPos blockPos = arg.getClickedPos();
		Direction direction = arg.getClickedFace();
		BlockPos blockPos2 = blockPos.relative(direction);
		Player player = arg.getPlayer();
		ItemStack itemStack = arg.getItemInHand();
		if (player != null && !this.mayPlace(player, direction, itemStack, blockPos2)) {
			return InteractionResult.FAIL;
		} else {
			Level level = arg.getLevel();
			HangingEntity hangingEntity;
			if (this.type == EntityType.PAINTING) {
				Optional<Painting> optional = Painting.create(level, blockPos2, direction);
				if (optional.isEmpty()) {
					return InteractionResult.CONSUME;
				}

				hangingEntity = (HangingEntity)optional.get();
			} else if (this.type == EntityType.ITEM_FRAME) {
				hangingEntity = new ItemFrame(level, blockPos2, direction);
			} else {
				if (this.type != EntityType.GLOW_ITEM_FRAME) {
					return InteractionResult.SUCCESS;
				}

				hangingEntity = new GlowItemFrame(level, blockPos2, direction);
			}

			CustomData customData = itemStack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
			if (!customData.isEmpty()) {
				EntityType.updateCustomEntityTag(level, player, hangingEntity, customData);
			}

			if (hangingEntity.survives()) {
				if (!level.isClientSide) {
					hangingEntity.playPlacementSound();
					level.gameEvent(player, GameEvent.ENTITY_PLACE, hangingEntity.position());
					level.addFreshEntity(hangingEntity);
				}

				itemStack.shrink(1);
				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.CONSUME;
			}
		}
	}

	protected boolean mayPlace(Player arg, Direction arg2, ItemStack arg3, BlockPos arg4) {
		return !arg2.getAxis().isVertical() && arg.mayUseItemAt(arg4, arg2, arg3);
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		super.appendHoverText(arg, arg2, list, arg3);
		HolderLookup.Provider provider = arg2.registries();
		if (provider != null && this.type == EntityType.PAINTING) {
			CustomData customData = arg.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
			if (!customData.isEmpty()) {
				customData.read(provider.createSerializationContext(NbtOps.INSTANCE), Painting.VARIANT_MAP_CODEC).result().ifPresentOrElse(argx -> {
					((PaintingVariant)argx.value()).title().ifPresent(list::add);
					((PaintingVariant)argx.value()).author().ifPresent(list::add);
					list.add(Component.translatable("painting.dimensions", ((PaintingVariant)argx.value()).width(), ((PaintingVariant)argx.value()).height()));
				}, () -> list.add(TOOLTIP_RANDOM_VARIANT));
			} else if (arg3.isCreative()) {
				list.add(TOOLTIP_RANDOM_VARIANT);
			}
		}
	}
}
