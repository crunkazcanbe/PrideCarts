package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One kind of action sign (line 2 of the sign). enter() runs when a train (or, for [cart] signs, each cart) rolls onto
 * the sign's rail and the sign is active + watching that direction. Register new kinds in ALL.
 */
public interface SignAction {
    /** line 2 words this action answers to, e.g. {"station"} */
    String[] names();

    /** a train reached the sign. `cart` is the cart on the sign's rail; `train` is the whole train (or just that cart for [cart]). */
    void enter(ActionSign sign, EntityMinecart cart, Train train);

    Map<String, SignAction> ALL = new LinkedHashMap<>();

    static void register(SignAction a) { for (String n : a.names()) ALL.put(n, a); }
}
