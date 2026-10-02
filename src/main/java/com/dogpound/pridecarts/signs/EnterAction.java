package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

/**
 * enter — put nearby players and/or mobs into empty carts of the train.
 *   line 3: radius in blocks (default 2, max 8).
 *   line 4: "players" (default), "mobs" or "all".
 * For each cart with no passengers, pick the closest eligible entity within radius of that cart that is not already riding anything and not a minecart,
 * and call startRiding(cart).
 */
public class EnterAction implements SignAction {
    @Override
    public String[] names() {
        return new String[]{"enter"};
    }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        double radius;
        try {
            radius = Math.min(8, Math.max(0.1, Double.parseDouble(sign.arg(2))));
        } catch (NumberFormatException e) {
            radius = 2;
        }
        
        String type = sign.arg(3).toLowerCase().trim();
        boolean players = type.isEmpty() || type.equals("players") || type.equals("all");
        boolean mobs = type.equals("mobs") || type.equals("all");
        
        for (EntityMinecart c : train.carts) {
            if (!c.getPassengers().isEmpty()) continue;
            
            AxisAlignedBB bb = c.getEntityBoundingBox().grow(radius);
            List<EntityLivingBase> entities = sign.world.getEntitiesWithinAABB(EntityLivingBase.class, bb);
            
            EntityLivingBase closest = null;
            double closestDist = Double.MAX_VALUE;
            
            for (EntityLivingBase e : entities) {
                if (e.isRiding()) continue;
                if (!players && e instanceof EntityPlayer) continue;
                if (!mobs && !(e instanceof EntityPlayer)) continue;
                
                double dist = c.getDistance(e);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = e;
                }
            }
            
            if (closest != null) {
                closest.startRiding(c);
            }
        }
    }
}
