package com.dogpound.pridecarts;

import mods.railcraft.api.carts.CartToolsAPI;
import mods.railcraft.api.carts.ILinkageManager;
import net.minecraft.entity.item.EntityMinecart;

import java.util.List;
import java.util.stream.Collectors;

/** Only loaded when Railcraft is installed. Railcraft owns the linking and the train physics; we just ask it. */
final class RailcraftLinks {
    private RailcraftLinks() {}

    static List<EntityMinecart> train(EntityMinecart cart) {
        ILinkageManager lm = CartToolsAPI.linkageManager();
        return lm.streamTrain(cart).collect(Collectors.toList());
    }

    static boolean link(EntityMinecart a, EntityMinecart b) { return CartToolsAPI.linkageManager().createLink(a, b); }

    static void unlink(EntityMinecart a) { CartToolsAPI.linkageManager().breakLinks(a); }
}
