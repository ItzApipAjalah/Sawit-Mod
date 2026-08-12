package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public class WrittenBookItem extends Item {
	public WrittenBookItem(Item.Properties arg) {
		super(arg);
	}

	@Override
	public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
		WrittenBookContent writtenBookContent = arg.get(DataComponents.WRITTEN_BOOK_CONTENT);
		if (writtenBookContent != null) {
			if (!StringUtil.isBlank(writtenBookContent.author())) {
				list.add(Component.translatable("book.byAuthor", writtenBookContent.author()).withStyle(ChatFormatting.GRAY));
			}

			list.add(Component.translatable("book.generation." + writtenBookContent.generation()).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public InteractionResult use(Level arg, Player arg2, InteractionHand arg3) {
		ItemStack itemStack = arg2.getItemInHand(arg3);
		arg2.openItemGui(itemStack, arg3);
		arg2.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResult.SUCCESS;
	}

	public static boolean resolveBookComponents(ItemStack arg, CommandSourceStack arg2, @Nullable Player arg3) {
		WrittenBookContent writtenBookContent = arg.get(DataComponents.WRITTEN_BOOK_CONTENT);
		if (writtenBookContent != null && !writtenBookContent.resolved()) {
			WrittenBookContent writtenBookContent2 = writtenBookContent.resolve(arg2, arg3);
			if (writtenBookContent2 != null) {
				arg.set(DataComponents.WRITTEN_BOOK_CONTENT, writtenBookContent2);
				return true;
			}

			arg.set(DataComponents.WRITTEN_BOOK_CONTENT, writtenBookContent.markResolved());
		}

		return false;
	}
}
