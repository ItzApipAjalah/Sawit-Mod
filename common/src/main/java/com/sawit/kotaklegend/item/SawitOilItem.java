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
        super(properties.component(net.minecraft.core.component.DataComponents.CONSUMABLE, net.minecraft.world.item.component.Consumable.builder().consumeSeconds(1.6f).animation(net.minecraft.world.item.ItemUseAnimation.DRINK).sound(net.minecraft.sounds.SoundEvents.GENERIC_DRINK).build()));
        this.uses = uses;
        this.isJelantah = isJelantah;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (isJelantah) {
            tooltipComponents.add(Component.translatable("tooltip.sawitmod.jelantah_oil").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.sawitmod.sawit_oil_uses", uses).withStyle(ChatFormatting.GOLD));
            tooltipComponents.add(Component.translatable("tooltip.sawitmod.sawit_oil_info").withStyle(ChatFormatting.YELLOW));
        }
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
    }

    @Override
    public net.minecraft.world.item.ItemUseAnimation getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return 32;
    }

    @Override
    public net.minecraft.world.InteractionResult use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
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
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    entityLiving.kill(serverLevel);
                }
            } else {
                entityLiving.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(com.sawit.kotaklegend.registry.ModEffects.KOLESTROL.get()), 6000, 0));
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
