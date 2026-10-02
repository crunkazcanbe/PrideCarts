package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

/**
 * waiter — keeps trains apart. When a train arrives, it looks ahead along the way it is going for another train.
 *   line 3: how far to look, in blocks (default 10, max 64).
 * If another train is within that distance, this train stops and waits; CartEvents releases it when the way is clear.
 */
public class WaiterAction implements SignAction {
    @Override
    public String[] names() { return new String[]{"waiter", "wait"}; }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        if (cart == null || train == null) return;

        int distance = parseDistance(sign.arg(2));
        EnumFacing dir = train.moving();
        if (dir == null) dir = Train.direction("", null, sign.facing);
        double speed = train.speed() > 0.05 ? train.speed() : 0.4;

        if (blocked(cart, dir, distance, train.key())) {
            train.stop();
            String dirName = dir.getName();
            for (EntityMinecart c : train.carts) {
                CartData.hold(c, -1, sign.pos.toLong(), sign.powered(), dirName, speed);
                NBTTagCompound h = CartData.hold(c);
                if (h != null) {
                    h.setBoolean("waiter", true);
                    h.setInteger("waitDist", distance);
                }
            }
        }
    }

    // CartEvents: if hold.getBoolean("waiter") is true, call clear(c, hold) and release the cart when it returns true.
    public static boolean clear(EntityMinecart c, NBTTagCompound hold) {
        if (c == null || hold == null) return true;

        int distance = hold.hasKey("waitDist") ? hold.getInteger("waitDist") : 10;
        distance = clampDistance(distance);
        if (distance <= 0) return true;

        EnumFacing dir = parseDir(hold.getString("dir"));
        if (dir == null) return true;

        return !blocked(c, dir, distance, Train.of(c).key());
    }

    private static boolean blocked(EntityMinecart c, EnumFacing dir, int distance, int key) {
        if (c == null || c.world == null || dir == null || distance <= 0) return false;

        World w = c.world;
        double x = c.posX;
        double y = c.posY;
        double z = c.posZ;

        double minX = x, maxX = x, minY = y - 2, maxY = y + 2, minZ = z, maxZ = z;
        switch (dir) {
            case NORTH:
                minZ = z - distance;
                maxZ = z;
                minX = x - 2;
                maxX = x + 2;
                break;
            case SOUTH:
                minZ = z;
                maxZ = z + distance;
                minX = x - 2;
                maxX = x + 2;
                break;
            case WEST:
                minX = x - distance;
                maxX = x;
                minZ = z - 2;
                maxZ = z + 2;
                break;
            case EAST:
                minX = x;
                maxX = x + distance;
                minZ = z - 2;
                maxZ = z + 2;
                break;
            default:
                return false;
        }

        List<EntityMinecart> carts = w.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
        for (EntityMinecart other : carts) {
            if (other == c || other.isDead) continue;
            if (Train.of(other).key() == key) continue;
            return true;
        }
        return false;
    }

    private static int parseDistance(String s) {
        if (s == null) return 10;
        s = s.trim();
        if (s.isEmpty()) return 10;
        try {
            return clampDistance((int) Math.round(Double.parseDouble(s)));
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    private static int clampDistance(int d) { return Math.max(0, Math.min(64, d)); }

    private static EnumFacing parseDir(String s) {
        if (s == null) return null;
        switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "north": return EnumFacing.NORTH;
            case "east": return EnumFacing.EAST;
            case "south": return EnumFacing.SOUTH;
            case "west": return EnumFacing.WEST;
            default: return null;
        }
    }
}
