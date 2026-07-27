package com.sawit.kotaklegend.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class SawitOilItem extends Item {
    private final int uses;
    public final boolean isJelantah;

    public SawitOilItem(Properties properties, int uses, boolean isJelantah) {
        super(properties);
        this.uses = uses;
        this.isJelantah = isJelantah;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (isJelantah) {
            tooltipComponents.add(Component.translatable("tooltip.sawitmod.jelantah_oil").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.sawitmod.sawit_oil_uses", uses).withStyle(ChatFormatting.GOLD));
            tooltipComponents.add(Component.translatable("tooltip.sawitmod.sawit_oil_info").withStyle(ChatFormatting.YELLOW));
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        return net.minecraft.world.item.ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entityLiving) {
        net.minecraft.world.entity.player.Player player = entityLiving instanceof net.minecraft.world.entity.player.Player ? (net.minecraft.world.entity.player.Player) entityLiving : null;

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        if (!level.isClientSide) {
            if (this.isJelantah) {
                entityLiving.kill();
            } else {
                entityLiving.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.sawit.kotaklegend.registry.ModEffects.KOLESTROL.get(), 6000, 0));
            }
        }

        if (player != null) {
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                if (stack.isEmpty()) {
                    return new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                }
                player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
            }
        }

        return stack;
    }
}
