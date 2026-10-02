package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * mutex — only one train at a time inside a zone (crossings, single-track stretches). Put one at every way in.
 *   line 3: the zone's name (signs with the same name share one zone; empty = just this sign's spot)
 *   line 4: zone radius in blocks around the sign (default 8)
 * A train entering a busy zone stops here and waits. The zone frees itself once its train is farther than twice the radius.
 */
public class MutexAction implements SignAction {
    /** zone key → {owner train key, sign x, sign y, sign z, radius} */
    private static final Map<String, int[]> OWNER = new HashMap<>();

    @Override public String[] names() { return new String[]{"mutex"}; }

    static String zone(ActionSign sign) {
        String name = sign.arg(2).trim();
        return sign.world.provider.getDimension() + ":" + (name.isEmpty() ? sign.pos.toLong() : name.toLowerCase());
    }

    static int radius(ActionSign sign) {
        try { return Math.max(1, Math.min(64, Integer.parseInt(sign.arg(3).trim()))); } catch (NumberFormatException e) { return 8; }
    }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        String key = zone(sign);
        int r = radius(sign), me = train.key();
        if (claim(sign.world, key, me, sign.pos, r)) return;             // free (or ours): go on through
        EnumFacing dir = train.moving();
        double speed = train.speed() > 0.05 ? train.speed() : 0.4;
        if (dir == null) dir = Train.direction("", null, sign.facing);
        train.stop();
        for (EntityMinecart c : train.carts) {
            CartData.hold(c, -1, sign.pos.toLong(), sign.powered(), dir.getName(), speed);
            NBTTagCompound h = CartData.hold(c);
            if (h != null) h.setString("mutex", key);                     // CartEvents asks free() every tick
        }
    }

    /** CartEvents: is the zone free for this train now? (claims it when it is) */
    public static boolean free(World w, String key, int myTrain, BlockPos signPos, int radius) {
        return claim(w, key, myTrain, signPos, radius);
    }

    private static boolean claim(World w, String key, int me, BlockPos signPos, int radius) {
        int[] o = OWNER.get(key);
        if (o != null && o[0] != me && stillInside(w, o)) return false;
        OWNER.put(key, new int[]{me, signPos.getX(), signPos.getY(), signPos.getZ(), radius});
        return true;
    }

    /** the owning train still has a cart within twice the radius of the sign that claimed the zone */
    private static boolean stillInside(World w, int[] o) {
        int reach = o[4] * 2;
        AxisAlignedBB box = new AxisAlignedBB(new BlockPos(o[1], o[2], o[3])).grow(reach);
        for (EntityMinecart c : w.getEntitiesWithinAABB(EntityMinecart.class, box))
            if (!c.isDead && Train.of(c).key() == o[0]) return true;
        return false;
    }
}
