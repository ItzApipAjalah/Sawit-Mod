package net.minecraft.world.item;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class CompassItem extends Item {
	private static final Component LODESTONE_COMPASS_NAME = Component.translatable("item.minecraft.lodestone_compass");

	public CompassItem(Item.Properties arg) {
		super(arg);
	}

	@Nullable
	public static GlobalPos getSpawnPosition(Level arg) {
		return arg.dimensionType().natural() ? GlobalPos.of(arg.dimension(), arg.getSharedSpawnPos()) : null;
	}

	@Override
	public boolean isFoil(ItemStack arg) {
		return arg.has(DataComponents.LODESTONE_TRACKER) || super.isFoil(arg);
	}

	@Override
	public void inventoryTick(ItemStack arg, Level arg2, Entity arg3, int i, boolean bl) {
		if (arg2 instanceof ServerLevel serverLevel) {
			LodestoneTracker lodestoneTracker = arg.get(DataComponents.LODESTONE_TRACKER);
			if (lodestoneTracker != null) {
				LodestoneTracker lodestoneTracker2 = lodestoneTracker.tick(serverLevel);
				if (lodestoneTracker2 != lodestoneTracker) {
					arg.set(DataComponents.LODESTONE_TRACKER, lodestoneTracker2);
				}
			}
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext arg) {
		BlockPos blockPos = arg.getClickedPos();
		Level level = arg.getLevel();
		if (!level.getBlockState(blockPos).is(Blocks.LODESTONE)) {
			return super.useOn(arg);
		} else {
			level.playSound(null, blockPos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
			Player player = arg.getPlayer();
			ItemStack itemStack = arg.getItemInHand();
			boolean bl = !player.hasInfiniteMaterials() && itemStack.getCount() == 1;
			LodestoneTracker lodestoneTracker = new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), blockPos)), true);
			if (bl) {
				itemStack.set(DataComponents.LODESTONE_TRACKER, lodestoneTracker);
			} else {
				ItemStack itemStack2 = itemStack.transmuteCopy(Items.COMPASS, 1);
				itemStack.consume(1, player);
				itemStack2.set(DataComponents.LODESTONE_TRACKER, lodestoneTracker);
				if (!player.getInventory().add(itemStack2)) {
					player.drop(itemStack2, false);
				}
			}

			return InteractionResult.SUCCESS;
		}
	}

	@Override
	public Component getName(ItemStack arg) {
		return arg.has(DataComponents.LODESTONE_TRACKER) ? LODESTONE_COMPASS_NAME : super.getName(arg);
	}
}
