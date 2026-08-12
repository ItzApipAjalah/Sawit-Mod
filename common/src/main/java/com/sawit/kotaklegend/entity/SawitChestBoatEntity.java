package com.sawit.kotaklegend.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import com.sawit.kotaklegend.registry.ModItems;
import com.sawit.kotaklegend.registry.ModEntityTypes;

public class SawitChestBoatEntity extends ChestBoat {
    public SawitChestBoatEntity(EntityType<SawitChestBoatEntity> entityType, Level level) {
        super(entityType, level, ModItems.SAWIT_CHEST_BOAT::get);
    }
    
    public SawitChestBoatEntity(Level level, double x, double y, double z) {
        this(ModEntityTypes.SAWIT_CHEST_BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

}
