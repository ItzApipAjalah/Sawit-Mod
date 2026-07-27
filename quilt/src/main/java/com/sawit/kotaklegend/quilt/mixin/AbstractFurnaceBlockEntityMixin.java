package com.sawit.kotaklegend.quilt.mixin;

import com.sawit.kotaklegend.item.SawitOilItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public class AbstractFurnaceBlockEntityMixin {

    @Inject(method = "canBurn", at = @At("HEAD"), cancellable = true)
    private static void sawit_canBurn(RegistryAccess registryAccess, @Nullable net.minecraft.world.item.crafting.RecipeHolder<?> recipe, NonNullList<ItemStack> inventory, int maxStackSize, CallbackInfoReturnable<Boolean> cir) {
        if (recipe != null) {
            ItemStack fuelStack = inventory.get(1);
            if (!fuelStack.isEmpty() && fuelStack.getItem() instanceof SawitOilItem) {
                // If it's Jelantah, we definitely can't burn
                if ((((SawitOilItem) fuelStack.getItem()).isJelantah)) {
                    cir.setReturnValue(false);
                    return;
                }
                
                // If it is Sawit Oil, it can only cook EDIBLE items (food)
                ItemStack result = recipe.value().getResultItem(registryAccess);
                if (!result.isEmpty() && !result.getItem().isEdible()) {
                    cir.setReturnValue(false); // Cancel burning if it's not food
                }
            }
        }
    }
}

