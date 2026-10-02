package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;

/**
 * launcher — change the train's speed and direction.
 *   line 3: target speed in blocks/tick, e.g. "0.6"; a leading + or - means relative to the current speed ("+0.2"); empty = 0.4.
 *   line 4: direction word (continue/back/left/right/north/east/south/west), empty = continue.
 */
public class LauncherAction implements SignAction {
    @Override
    public String[] names() { return new String[]{"launcher", "launch"}; }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        String speedStr = sign.arg(2).trim();
        double speed = 0.4;
        if (!speedStr.isEmpty()) {
            boolean relative = speedStr.charAt(0) == '+' || speedStr.charAt(0) == '-';
            try {
                speed = Double.parseDouble(speedStr);
                if (relative) speed += train.speed();
            } catch (NumberFormatException ignored) {}
        }
        speed = Train.clampSpeed(speed);
        String dirWord = sign.arg(3).trim();
        EnumFacing moving = train.moving();
        EnumFacing dir = dirWord.isEmpty() ? moving : Train.direction(dirWord, moving, sign.facing);
        train.launch(dir, speed);
    }
}
