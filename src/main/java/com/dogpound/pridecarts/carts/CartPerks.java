package com.dogpound.pridecarts.carts;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Car perks that need world events: the prisoner car's lock and the sleeper car's night skip. */
@Mod.EventBusSubscriber(modid = "pridecarts")
public final class CartPerks {
    private CartPerks() {}

    /** Prisoner car: no getting out while it's moving (creative players can). */
    @SubscribeEvent
    public static void dismount(EntityMountEvent e) {
        if (!e.isDismounting() || e.getWorldObj().isRemote) return;
        if (!(e.getEntityBeingMounted() instanceof EntitySeatCart)) return;
        EntitySeatCart cart = (EntitySeatCart) e.getEntityBeingMounted();
        if (cart.cartType() != CartType.PRISONER || !(e.getEntityMounting() instanceof EntityPlayer)) return;
        EntityPlayer p = (EntityPlayer) e.getEntityMounting();
        if (p.capabilities.isCreativeMode || p.isDead) return;
        double v = cart.motionX * cart.motionX + cart.motionZ * cart.motionZ;
        if (v > 0.0004) {
            e.setCanceled(true);
            p.sendStatusMessage(new TextComponentString("§c🔒 The prisoner car is locked while moving"), true);
        }
    }

    /** Sleeper car: at night, if every player in this world is in a sleeper car, skip to morning. */
    static void sleeperNight(World w) {
        long t = w.getWorldTime() % 24000;
        if (t < 12541 || t > 23458 || w.playerEntities.isEmpty()) return;
        for (EntityPlayer p : w.playerEntities)
            if (!(p.getRidingEntity() instanceof EntitySeatCart) || ((EntitySeatCart) p.getRidingEntity()).cartType() != CartType.SLEEPER) return;
        w.setWorldTime(w.getWorldTime() + (24000 - t));
        for (EntityPlayer p : w.playerEntities) p.sendStatusMessage(new TextComponentString("§b🌙 Good morning! You slept through the night"), true);
    }
}
