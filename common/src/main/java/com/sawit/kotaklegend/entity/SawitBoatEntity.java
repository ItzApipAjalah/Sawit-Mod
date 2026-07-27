package com.sawit.kotaklegend.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import com.sawit.kotaklegend.registry.ModItems;
import com.sawit.kotaklegend.registry.ModEntityTypes;

public class SawitBoatEntity extends Boat {
    public SawitBoatEntity(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
    }
    
    public SawitBoatEntity(Level level, double x, double y, double z) {
        this(ModEntityTypes.SAWIT_BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public Item getDropItem() {
        return ModItems.SAWIT_BOAT.get();
    }
}
