package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.CartEvents;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * eject — lets passengers out of every cart.
 *   line 3: optional offset "dx dy dz" in blocks from the ejecting cart (default: 1.5 blocks to the side the sign text faces).
 *   line 4: "all" or "mobs" to eject non-players too (default: players only).
 */
public class EjectAction implements SignAction {
    @Override
    public String[] names() { return new String[]{"eject"}; }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        boolean all = sign.arg(3).toLowerCase().contains("all") || sign.arg(3).toLowerCase().contains("mobs");
        // an offset on line 3 replaces the default (1.5 blocks out to the sign's text side)
        double dx = 1.5 * sign.facing.getFrontOffsetX(), dy = 0, dz = 1.5 * sign.facing.getFrontOffsetZ();
        String[] offset = sign.arg(2).trim().split("\\s+");
        if (offset.length >= 3) {
            try { dx = Double.parseDouble(offset[0]); dy = Double.parseDouble(offset[1]); dz = Double.parseDouble(offset[2]); }
            catch (NumberFormatException ignored) {}
        }
        Vec3d offsetVec = new Vec3d(dx, dy, dz);

        CartEvents.ejecting = true;
        try {
            List<Entity> passengers = new ArrayList<>(train.passengers());
            for (Entity passenger : passengers) {
                if (!all && !(passenger instanceof EntityPlayer)) continue;
                
                Entity from = passenger.getRidingEntity() != null ? passenger.getRidingEntity() : cart;   // each passenger's own cart
                double x = from.posX + offsetVec.x, y = from.posY + offsetVec.y, z = from.posZ + offsetVec.z;
                passenger.dismountRidingEntity();
                if (passenger instanceof EntityPlayer) ((EntityPlayer) passenger).setPositionAndUpdate(x, y, z);
                else passenger.setPosition(x, y, z);
            }
        } finally {
            CartEvents.ejecting = false;
        }
    }
}
