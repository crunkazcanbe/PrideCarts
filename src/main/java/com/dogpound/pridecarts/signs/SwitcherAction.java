package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.RailGraph;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;

import java.util.Locale;

/**
 * switcher — put it under a junction (a normal rail where 3 or 4 rails meet). It points the junction for each train.
 *   line 3: the rule for going LEFT,  line 4: the rule for going RIGHT (left/right as seen by the arriving train).
 *   A rule is:  "tag vip" · "dest spawn" · "name express" · "*" (always) — the first rule that matches wins;
 *   no match (or both lines empty) = the train is routed to its destination automatically (shortest way),
 *   and with no destination it goes straight on when it can.
 */
public class SwitcherAction implements SignAction {
    @Override public String[] names() { return new String[]{"switcher", "switch", "tag"}; }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        EnumFacing moving = train.moving();
        if (moving == null || !RailGraph.isJunction(sign.world, sign.rail)) return;
        EnumFacing cameFrom = moving.getOpposite();
        EnumFacing exit = null;
        if (matches(sign.arg(2), train)) exit = moving.rotateYCCW();          // left
        else if (matches(sign.arg(3), train)) exit = moving.rotateY();        // right
        if (exit == null) exit = routeOrStraight(sign, cameFrom, moving, train);
        RailGraph.setJunction(sign.world, sign.rail, cameFrom, exit);
    }

    static boolean matches(String rule, Train t) {
        String r = rule.trim().toLowerCase(Locale.ROOT);
        if (r.isEmpty()) return false;
        if (r.equals("*") || r.equals("always") || r.equals("default")) return true;
        String[] p = r.split("\\s+", 2);
        String v = p.length > 1 ? p[1].trim() : "";
        switch (p[0]) {
            case "tag": case "!tag": return p[0].startsWith("!") != t.hasTag(v);
            case "dest": case "destination": return t.get("destination").equalsIgnoreCase(v);
            case "name": return t.get("name").equalsIgnoreCase(v);
            default: return t.hasTag(r);                                      // a bare word = a tag
        }
    }

    static EnumFacing routeOrStraight(ActionSign sign, EnumFacing cameFrom, EnumFacing moving, Train train) {
        String dest = CartData.get(train.head(), "destination");
        if (!dest.isEmpty()) {
            EnumFacing r = RailGraph.route(sign.world, sign.rail, cameFrom, dest);
            if (r != null) return r;
        }
        return moving;                                                         // straight on
    }
}
