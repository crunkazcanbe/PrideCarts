package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;

/**
 * station — stop the train on this rail, wait, then send it off.
 *   line 3: how long to wait in seconds (e.g. "5"); empty = wait until the sign's redstone changes.
 *   line 4: which way to leave + optional speed, e.g. "continue", "back 0.6", "north", "left".
 *           "none" = stay until a /train command or another sign moves it.
 * While a train waits, a lever on/next to the sign turns on (TrainCarts does the same).
 */
public class StationAction implements SignAction {
    @Override public String[] names() { return new String[]{"station"}; }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        String wait = sign.arg(2).replace("s", "").trim();
        long until = -1;
        if (!wait.isEmpty()) {
            try { until = sign.world.getTotalWorldTime() + Math.round(Double.parseDouble(wait) * 20); } catch (NumberFormatException ignored) {}
        }
        String[] leave = sign.arg(3).trim().split("\\s+");
        String dirWord = leave.length > 0 ? leave[0] : "";
        double speed = 0.4;
        if (leave.length > 1) try { speed = Double.parseDouble(leave[1]); } catch (NumberFormatException ignored) {}
        EnumFacing moving = train.moving();
        String dir = dirWord.equalsIgnoreCase("none") ? "none" : Train.direction(dirWord, moving, sign.facing).getName();
        train.stop();
        for (EntityMinecart c : train.carts) {
            // stop in the middle of the station rail
            if (c == cart) c.setPosition(sign.rail.getX() + 0.5, c.posY, sign.rail.getZ() + 0.5);
            CartData.hold(c, until, sign.pos.toLong(), sign.powered(), dir, speed);
        }
        sign.output(true);
        arrived(sign, train);
    }

    /** PrideQuests "ride a train to a station" tasks: every player aboard gets credit (by reflection — PrideQuests is optional) */
    private static java.lang.reflect.Method questHook;
    private static boolean hookTried;

    private static void arrived(ActionSign sign, Train train) {
        if (!hookTried) {
            hookTried = true;
            try { questHook = Class.forName("com.dogpound.pridequests.Engine").getMethod("trainArrived", net.minecraft.entity.player.EntityPlayerMP.class, String.class); }
            catch (Throwable ignored) {}
        }
        if (questHook == null) return;
        String name = String.join(" ", sign.lines).replaceAll("\\[.*?]", "").trim();
        for (EntityMinecart c : train.carts)
            for (net.minecraft.entity.Entity e : c.getPassengers())
                if (e instanceof net.minecraft.entity.player.EntityPlayerMP)
                    try { questHook.invoke(null, e, name); } catch (Throwable ignored) {}
    }
}
