package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;

import java.util.List;

/**
 * destination — mark this place as a destination for a train.
 *   line 3: the name of this destination (e.g. "Central Station").
 *   line 4: optional next destination (e.g. "North Station"); if empty, the train will keep going to the next stop in its route.
 * When a train's destination property equals this sign's line 3 (ignoring case), it has arrived.
 * If the train's route list contains this destination, it will go to the next one in the list (wrapping to the first),
 * otherwise it will clear its destination and keep going.
 */
public class DestinationAction implements SignAction {
    @Override
    public String[] names() {
        return new String[]{"destination", "dest"};
    }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        String destination = sign.arg(2);
        if (destination.isEmpty()) return;
        
        String currentDest = train.get("destination");
        if (currentDest.equalsIgnoreCase(destination)) {
            String route = train.get("route");
            List<String> routeList = CartData.list(train.head(), "route");
            String nextDest = "";
            
            if (!route.isEmpty() && routeList.contains(destination)) {
                int index = routeList.indexOf(destination);
                int nextIndex = (index + 1) % routeList.size();
                nextDest = routeList.get(nextIndex);
            }
            
            if (!nextDest.isEmpty()) {
                train.set("destination", nextDest);
            } else {
                train.set("destination", "");
            }
        }
        
        String nextDest = sign.arg(3);
        if (!nextDest.isEmpty()) {
            train.set("destination", nextDest);
        }
    }
}
