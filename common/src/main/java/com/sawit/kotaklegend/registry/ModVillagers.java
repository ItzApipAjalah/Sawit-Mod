package com.sawit.kotaklegend.registry;

import com.sawit.kotaklegend.ExampleMod;
import dev.architectury.registry.level.entity.trade.TradeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ExampleMod.MOD_ID, Registries.POINT_OF_INTEREST_TYPE);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(ExampleMod.MOD_ID, Registries.VILLAGER_PROFESSION);

    public static final RegistrySupplier<PoiType> SAWIT_POI = POI_TYPES.register("sawit_poi", () -> {
        Set<BlockState> states = ImmutableSet.copyOf(ModBlocks.SAWIT_BAG.get().getStateDefinition().getPossibleStates());
        return new PoiType(states, 1, 1);
    });

    public static final RegistrySupplier<VillagerProfession> JURAGAN_SAWIT = VILLAGER_PROFESSIONS.register("juragan_sawit", () ->
            new VillagerProfession("juragan_sawit",
                    x -> x.is(SAWIT_POI.getKey()),
                    x -> x.is(SAWIT_POI.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_FARMER)
    );

    public static void register() {
        POI_TYPES.register();
        VILLAGER_PROFESSIONS.register();

        dev.architectury.event.events.common.LifecycleEvent.SETUP.register(() -> {
            try {
                java.lang.reflect.Method registerBlockStates = null;
                for (java.lang.reflect.Method m : net.minecraft.world.entity.ai.village.poi.PoiTypes.class.getDeclaredMethods()) {
                    if (m.getParameterCount() == 2 
                        && m.getParameterTypes()[0] == net.minecraft.core.Holder.class 
                        && m.getParameterTypes()[1] == java.util.Set.class) {
                        registerBlockStates = m;
                        break;
                    }
                }
                
                if (registerBlockStates != null) {
                    registerBlockStates.setAccessible(true);
                
                java.util.Optional<net.minecraft.core.Holder.Reference<PoiType>> holderOpt = net.minecraft.core.registries.BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolder(SAWIT_POI.getKey());
                if (holderOpt.isPresent()) {
                    registerBlockStates.invoke(null, holderOpt.get(), com.google.common.collect.ImmutableSet.copyOf(ModBlocks.SAWIT_BAG.get().getStateDefinition().getPossibleStates()));
                    System.out.println("Successfully registered Sawit POI block states!");
                } else {
                    System.out.println("Failed to get Sawit POI Holder!");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    static class BasicTrade implements VillagerTrades.ItemListing {
        private final ItemStack price;
        private final ItemStack forSale;
        private final int maxTrades;
        private final int xp;
        private final float priceMult;

        public BasicTrade(ItemStack price, ItemStack forSale, int maxTrades, int xp, float priceMult) {
            this.price = price;
            this.forSale = forSale;
            this.maxTrades = maxTrades;
            this.xp = xp;
            this.priceMult = priceMult;
        }

        @Override
        public net.minecraft.world.item.trading.MerchantOffer getOffer(net.minecraft.world.entity.Entity trader, net.minecraft.util.RandomSource random) {
            return new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(price.getItem(), price.getCount()), forSale, maxTrades, xp, priceMult);
        }
    }

    public static void registerTrades() {
        // Level 1
        // Buy 20 Sawit Fruits -> 1 Emerald
        TradeRegistry.registerVillagerTrade(JURAGAN_SAWIT.get(), 1, 
            new BasicTrade(new ItemStack(ModItems.SAWIT_FRUIT.get(), 20), new ItemStack(Items.EMERALD, 1), 16, 2, 0.05f)
        );
        // Sell 1 Sawit Seed <- 2 Emeralds
        TradeRegistry.registerVillagerTrade(JURAGAN_SAWIT.get(), 1, 
            new BasicTrade(new ItemStack(Items.EMERALD, 2), new ItemStack(ModItems.SAWIT_SEEDS.get(), 1), 12, 2, 0.05f)
        );

        // Level 2
        // Buy 4 Sawit Bunches -> 1 Emerald
        TradeRegistry.registerVillagerTrade(JURAGAN_SAWIT.get(), 2, 
            new BasicTrade(new ItemStack(ModItems.SAWIT_BUNCH.get(), 4), new ItemStack(Items.EMERALD, 1), 16, 10, 0.05f)
        );
        // Sell 1 Sawit Oil 1 <- 3 Emeralds
        TradeRegistry.registerVillagerTrade(JURAGAN_SAWIT.get(), 2, 
            new BasicTrade(new ItemStack(Items.EMERALD, 3), new ItemStack(ModItems.SAWIT_OIL_1.get(), 1), 12, 10, 0.05f)
        );

        // Level 3
        // Buy 2 Sawit Bags -> 1 Emerald
        TradeRegistry.registerVillagerTrade(JURAGAN_SAWIT.get(), 3, 
            new BasicTrade(new ItemStack(ModBlocks.SAWIT_BAG.get(), 2), new ItemStack(Items.EMERALD, 1), 12, 20, 0.05f)
        );
        // Sell 1 Sawit Boat <- 2 Emeralds
        TradeRegistry.registerVillagerTrade(JURAGAN_SAWIT.get(), 3, 
            new BasicTrade(new ItemStack(Items.EMERALD, 2), new ItemStack(ModItems.SAWIT_BOAT.get(), 1), 12, 20, 0.05f)
        );
    }
}
