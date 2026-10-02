package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;

/**
 * skip — the train ignores the next N action signs it meets (e.g. an express that skips the next stations).
 *   line 3: how many signs to skip (default 1)
 *   line 4: optional "tag X" — only trains with that tag skip
 */
public class SkipAction implements SignAction {
    @Override public String[] names() { return new String[]{"skip"}; }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        String cond = sign.arg(3).trim();
        if (cond.toLowerCase().startsWith("tag ") && !train.hasTag(cond.substring(4).trim())) return;
        int n = 1;
        try { n = Math.max(0, Math.min(100, Integer.parseInt(sign.arg(2).trim()))); } catch (NumberFormatException ignored) {}
        for (EntityMinecart c : train.carts) CartData.tag(c).setInteger("skip", n);
    }

    /** CartEvents calls this before each sign: true = skip this one (and count it down) */
    public static boolean consume(EntityMinecart c) {
        int n = CartData.tag(c).getInteger("skip");
        if (n <= 0) return false;
        CartData.tag(c).setInteger("skip", n - 1);
        return true;
    }
}
