package com.dogpound.pridecarts.carts;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * PrideCarts' own cart coupling (works without Railcraft): every cart has two coupler slots, each holding the other
 * cart's UUID and the link kind (chain = tight, rope = slack). Links are saved in the cart's Forge data, so vanilla and
 * other mods' carts can be coupled too. Once a tick each link acts like a damped spring: too far apart pulls both carts
 * together, too close pushes them apart, and their speeds along the link are evened out so a train moves as one.
 */
@Mod.EventBusSubscriber(modid = "pridecarts")
public final class CartLinks {
    private CartLinks() {}

    public static final String KEY = "PrideCartsLinks";
    /** Longest train one set of couplings may make. */
    public static final int MAX_TRAIN = 10;

    public enum Kind {
        CHAIN(1.35, 1.5, 4.5), ROPE(1.6, 3.0, 6.0);
        /** rest length, slack (no force until this long), snap distance */
        public final double rest, slack, snap;
        Kind(double rest, double slack, double snap) { this.rest = rest; this.slack = slack; this.snap = snap; }
    }

    // ---------------------------------------------------------------- storage

    private static NBTTagCompound tag(EntityMinecart c) {
        NBTTagCompound root = c.getEntityData();
        if (!root.hasKey(KEY)) root.setTag(KEY, new NBTTagCompound());
        return root.getCompoundTag(KEY);
    }

    /** UUID in slot 0/1, or null. */
    public static UUID linked(EntityMinecart c, int slot) {
        NBTTagCompound t = tag(c);
        return t.hasUniqueId("l" + slot) ? t.getUniqueId("l" + slot) : null;
    }

    public static Kind kind(EntityMinecart c, int slot) {
        return tag(c).getBoolean("rope" + slot) ? Kind.ROPE : Kind.CHAIN;
    }

    private static void set(EntityMinecart c, int slot, UUID other, Kind k) {
        NBTTagCompound t = tag(c);
        if (other == null) { t.removeTag("l" + slot + "Most"); t.removeTag("l" + slot + "Least"); t.removeTag("rope" + slot); }
        else { t.setUniqueId("l" + slot, other); t.setBoolean("rope" + slot, k == Kind.ROPE); }
        if (c instanceof IPrideCart) ((IPrideCart) c).linksChanged();
    }

    private static int freeSlot(EntityMinecart c) {
        return linked(c, 0) == null ? 0 : linked(c, 1) == null ? 1 : -1;
    }

    private static int slotOf(EntityMinecart c, UUID other) {
        for (int s = 0; s < 2; s++) if (other.equals(linked(c, s))) return s;
        return -1;
    }

    // ---------------------------------------------------------------- coupling

    /** Couple a and b. Returns a message for the player (null = success). */
    public static String couple(EntityMinecart a, EntityMinecart b, Kind k) {
        if (a == b) return "That's the same cart";
        if (slotOf(a, b.getUniqueID()) >= 0) return "Those two are already coupled";
        if (a.getDistance(b) > k.slack + 1.5) return "Too far apart: bring the carts closer";
        int sa = freeSlot(a), sb = freeSlot(b);
        if (sa < 0 || sb < 0) return "A cart has no free coupler (two per cart)";
        if (train(a).size() + train(b).size() > MAX_TRAIN) return "Trains can be at most " + MAX_TRAIN + " carts long";
        set(a, sa, b.getUniqueID(), k);
        set(b, sb, a.getUniqueID(), k);
        return null;
    }

    /** Break every link of this cart; returns how many couplings came off (to drop as items). */
    public static List<Kind> uncoupleAll(EntityMinecart c) {
        List<Kind> out = new ArrayList<>();
        for (int s = 0; s < 2; s++) {
            UUID o = linked(c, s);
            if (o == null) continue;
            out.add(kind(c, s));
            Entity other = find(c.world, o);
            if (other instanceof EntityMinecart) {
                int os = slotOf((EntityMinecart) other, c.getUniqueID());
                if (os >= 0) set((EntityMinecart) other, os, null, null);
            }
            set(c, s, null, null);
        }
        return out;
    }

    private static Entity find(World w, UUID id) {
        return w instanceof WorldServer ? ((WorldServer) w).getEntityFromUuid(id) : null;
    }

    /** All carts coupled to this one, this one first, walking both directions. Server only. */
    public static List<EntityMinecart> train(EntityMinecart start) {
        List<EntityMinecart> out = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        List<EntityMinecart> todo = new ArrayList<>();
        todo.add(start);
        while (!todo.isEmpty() && out.size() < 64) {
            EntityMinecart c = todo.remove(todo.size() - 1);
            if (!seen.add(c.getUniqueID())) continue;
            out.add(c);
            for (int s = 0; s < 2; s++) {
                UUID o = linked(c, s);
                Entity e = o == null ? null : find(c.world, o);
                if (e instanceof EntityMinecart && !seen.contains(o)) todo.add((EntityMinecart) e);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- physics

    @SubscribeEvent
    public static void tick(MinecartUpdateEvent e) {
        EntityMinecart a = e.getMinecart();
        if (a.world.isRemote) return;
        for (int s = 0; s < 2; s++) {
            UUID o = linked(a, s);
            if (o == null) continue;
            Entity other = find(a.world, o);
            if (!(other instanceof EntityMinecart) || other.isDead) continue;   // other end unloaded: wait for it
            EntityMinecart b = (EntityMinecart) other;
            // each pair once per tick: the cart with the smaller UUID does the work
            if (a.getUniqueID().compareTo(b.getUniqueID()) > 0) continue;
            spring(a, b, kind(a, s));
        }
    }

    private static void spring(EntityMinecart a, EntityMinecart b, Kind k) {
        Vec3d d = new Vec3d(b.posX - a.posX, 0, b.posZ - a.posZ);
        double dist = d.lengthVector();
        if (dist > k.snap) {                                        // pulled apart: the coupling snaps
            for (Kind dropped : uncoupleAll(a)) a.entityDropItem(CartRegistry.coupler(dropped), 0.5f);
            a.world.playSound(null, a.posX, a.posY, a.posZ, net.minecraft.init.SoundEvents.ENTITY_ITEM_BREAK,
                    net.minecraft.util.SoundCategory.NEUTRAL, 0.8f, 0.7f);
            return;
        }
        if (dist < 1e-4) return;
        Vec3d n = d.scale(1 / dist);
        // a stiff link with a little play, not a spring: inside [min, max] the carts roll freely; outside it, the part of
        // their motion that pulls apart (or pushes together) is cancelled and a small push fixes the gap. No overshoot,
        // so no bouncing (2026-10-04: "the carts are jumping all over the place").
        double min = k.rest * 0.85, max = k.slack;
        double rel = (b.motionX - a.motionX) * n.x + (b.motionZ - a.motionZ) * n.z;   // > 0: moving apart
        double imp = 0;
        if (dist > max) imp = (rel > 0 ? rel : 0) * 0.5 + (dist - max) * 0.12;
        else if (dist < min) imp = (rel < 0 ? rel : 0) * 0.5 - (min - dist) * 0.12;
        else imp = rel * 0.04;                                                // gently even out speeds in the slack
        imp = Math.max(-0.2, Math.min(0.2, imp));
        a.motionX += n.x * imp; a.motionZ += n.z * imp;
        b.motionX -= n.x * imp; b.motionZ -= n.z * imp;
    }

    /** For the client sync: {entity id slot 0, entity id slot 1, rope bits}. */
    public static int[] resolve(Object cartObj) {
        EntityMinecart c = (EntityMinecart) cartObj;
        int[] out = new int[3];
        for (int s = 0; s < 2; s++) {
            UUID o = linked(c, s);
            Entity e = o == null ? null : find(c.world, o);
            out[s] = e == null ? 0 : e.getEntityId();
            if (o != null && kind(c, s) == Kind.ROPE) out[2] |= 1 << s;
        }
        return out;
    }

    /** Coupler item name for chat. */
    public static String name(Kind k) {
        return k == Kind.ROPE ? "Cart Rope" : "Cart Chain";
    }

    public static ItemStack drop(Kind k) {
        return CartRegistry.coupler(k);
    }
}
