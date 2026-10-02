package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;

/**
 * blocker — while the sign is active (e.g. powered), trains arriving here stop and wait; when it switches off they
 *   carry on the way they were going, at the speed they had.
 *   line 3: optional speed to leave with instead (e.g. "0.6").
 * While a train waits, a lever on/next to the sign turns on.
 */
public class BlockerAction implements SignAction {
    @Override
    public String[] names() { return new String[]{"blocker", "block"}; }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        EnumFacing dir = train.moving();                               // read before stopping
        double speed = train.speed() > 0.05 ? train.speed() : 0.4;
        String speedStr = sign.arg(2).trim();
        if (!speedStr.isEmpty()) {
            try { speed = Double.parseDouble(speedStr); } catch (NumberFormatException ignored) {}
        }
        if (dir == null) dir = Train.direction("", null, sign.facing);
        String dirName = dir.getName();
        train.stop();
        for (EntityMinecart c : train.carts) {
            CartData.hold(c, -1, sign.pos.toLong(), sign.powered(), dirName, speed);
            CartData.hold(c).setBoolean("blocker", true);
        }
        sign.output(true);
    }
}
