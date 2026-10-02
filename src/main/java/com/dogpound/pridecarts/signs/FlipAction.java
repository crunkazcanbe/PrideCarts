package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;

/**
 * flip — send the train back the way it came at the same speed.
 *   line 3: empty or "continue" = keep going the same direction
 *   line 4: empty or "reverse" = go back the way it came
 */
public class FlipAction implements SignAction {
    @Override
    public String[] names() {
        return new String[]{"flip", "reverse"};
    }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        String dirWord = sign.arg(3).trim().toLowerCase();
        EnumFacing moving = train.moving();
        if (moving == null) return;
        
        EnumFacing dir;
        if (dirWord.isEmpty() || dirWord.equals("reverse") || dirWord.equals("back")) {
            dir = moving.getOpposite();                                // a flip sign's whole job: go back the way it came
        } else {
            dir = Train.direction(dirWord, moving, sign.facing);
        }
        
        double speed = Math.max(train.speed(), 0.1);
        train.launch(dir, speed);
    }
}
