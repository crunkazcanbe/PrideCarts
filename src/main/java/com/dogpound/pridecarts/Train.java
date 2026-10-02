package com.dogpound.pridecarts;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A train = carts that move together. With Railcraft installed that's Railcraft's own linked carts (so the two mods never
 * fight over cart physics); without it, every cart is its own train. Properties set on a train go on every cart.
 */
public final class Train {
    private static Boolean railcraft;

    public final List<EntityMinecart> carts;

    private Train(List<EntityMinecart> carts) { this.carts = carts; }

    public static Train of(EntityMinecart cart) {
        if (railcraft == null) railcraft = Loader.isModLoaded("railcraft");
        if (railcraft) {
            try {
                List<EntityMinecart> l = RailcraftLinks.train(cart);
                if (!l.isEmpty()) return new Train(l);
            } catch (Throwable t) {
                railcraft = false;                                     // API changed: fall back to single carts
                PrideCarts.LOG.warn("Railcraft linking unavailable ({}); trains are single carts", t.toString());
            }
        }
        return new Train(Collections.singletonList(cart));
    }

    /** just this one cart (for [cart] signs) */
    public static Train single(EntityMinecart cart) { return new Train(Collections.singletonList(cart)); }

    /** the same for every cart of one train: the lowest entity id in it */
    public int key() { int k = Integer.MAX_VALUE; for (EntityMinecart c : carts) k = Math.min(k, c.getEntityId()); return k; }

    public EntityMinecart head() { return carts.get(0); }

    public void set(String prop, String value) { for (EntityMinecart c : carts) CartData.set(c, prop, value); }

    public String get(String prop) { return CartData.get(head(), prop); }

    public boolean hasTag(String tag) { for (EntityMinecart c : carts) if (CartData.hasTag(c, tag)) return true; return false; }

    public List<Entity> passengers() {
        List<Entity> out = new ArrayList<>();
        for (EntityMinecart c : carts) out.addAll(c.getPassengers());
        return out;
    }

    public double speed() {
        EntityMinecart c = head();
        return Math.sqrt(c.motionX * c.motionX + c.motionZ * c.motionZ);
    }

    /** the direction the train is rolling (null when still) */
    public EnumFacing moving() {
        EntityMinecart c = head();
        if (Math.abs(c.motionX) < 1e-3 && Math.abs(c.motionZ) < 1e-3) return null;
        return EnumFacing.getFacingFromVector((float) c.motionX, 0, (float) c.motionZ);
    }

    public void stop() {
        for (EntityMinecart c : carts) { c.motionX = 0; c.motionZ = 0; }
    }

    /** set speed (blocks/tick) along a direction; null direction = keep going the way it's going (or its last way) */
    public void launch(EnumFacing dir, double speed) {
        if (dir == null) dir = moving();
        if (dir == null) dir = EnumFacing.fromAngle(head().rotationYaw + 90);   // a cart's yaw is 90° off its travel
        speed = Math.max(0, speed);
        for (EntityMinecart c : carts) {
            if (speed > c.getCurrentCartSpeedCapOnRail()) c.setCurrentCartSpeedCapOnRail((float) Math.min(speed, c.getMaxCartSpeedOnRail()));
            c.motionX = dir.getFrontOffsetX() * speed;
            c.motionZ = dir.getFrontOffsetZ() * speed;
        }
    }

    /** "continue", "forward", "back", "left", "right", or a compass direction — relative to where the train is going */
    public static EnumFacing direction(String word, EnumFacing moving, EnumFacing signFacing) {
        EnumFacing fwd = moving != null ? moving : signFacing == null ? EnumFacing.NORTH : signFacing.getOpposite();
        switch (word == null ? "" : word.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "": case "continue": case "c": case "forward": case "f": return fwd;
            case "back": case "b": case "reverse": return fwd.getOpposite();
            case "left": case "l": return fwd.rotateYCCW();
            case "right": case "r": return fwd.rotateY();
            case "north": case "n": return EnumFacing.NORTH;
            case "east": case "e": return EnumFacing.EAST;
            case "south": case "s": return EnumFacing.SOUTH;
            case "west": case "w": return EnumFacing.WEST;
            default: return fwd;
        }
    }

    public static double clampSpeed(double v) { return MathHelper.clamp(v, 0, 3.0); }
}
