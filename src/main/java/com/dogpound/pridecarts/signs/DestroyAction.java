package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartEvents;
import net.minecraft.entity.Entity;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.DamageSource;

/**
 * destroy — remove the train from the world.
 *   line 3: "drop" = drop the cart as an item, otherwise just remove it.
 */
public class DestroyAction implements SignAction {
    @Override
    public String[] names() {
        return new String[]{"destroy", "destroyer"};
    }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        boolean drop = "drop".equalsIgnoreCase(sign.arg(2));
        CartEvents.ejecting = true;
        try {
            for (EntityMinecart c : train.carts) {
                for (Entity e : new java.util.ArrayList<>(c.getPassengers())) {   // copy: dismounting edits the list
                    if (e.isRiding()) {
                        e.dismountRidingEntity();
                    }
                }
            }
        } finally {
            CartEvents.ejecting = false;
        }
        
        for (EntityMinecart c : train.carts) {
            if (drop) {
                c.killMinecart(DamageSource.GENERIC);
            } else {
                c.setDead();
            }
        }
    }
}
