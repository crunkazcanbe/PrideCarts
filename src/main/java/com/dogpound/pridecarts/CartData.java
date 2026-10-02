package com.dogpound.pridecarts;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A cart's TrainCarts-style settings, saved on the cart itself (its NBT), so they survive restarts and travel with it.
 * Every property has a name, a default and a one-line explanation — /train set lists them, the menu will too.
 */
public final class CartData {
    public static final String KEY = "PrideCarts";

    /** name → {default, explanation}. Values are strings on the cart; typed getters below. */
    public static final Map<String, String[]> PROPS = new LinkedHashMap<>();
    static {
        prop("speedlimit", "0.4", "Top speed in blocks per tick (0.4 = vanilla; Railcraft high-speed track allows more).");
        prop("slowdown", "true", "false = the train keeps its speed instead of slowing down on flat track.");
        prop("gravity", "1.0", "How strongly slopes speed it up / slow it down (1 = normal, 0 = flat-like).");
        prop("playerenter", "true", "Players may get in.");
        prop("playerexit", "true", "Players may get out (sneak). false = locked in until a sign ejects them.");
        prop("ownersonly", "false", "Only the train's owners may get in.");
        prop("pickup", "false", "Storage carts pick up items they roll over.");
        prop("invincible", "false", "Can't be broken by players or damage.");
        prop("keepchunks", "false", "Keep the chunks around the train loaded so it keeps running with nobody near.");
        prop("mobenter", "true", "Mobs get in when the train bumps into them.");
        prop("collision", "push", "What happens to mobs in the way: push, kill, ignore (pass through), enter.");
        prop("sound", "true", "Rolling sounds.");
        prop("name", "", "The train's name (shown by /train info, used by signs and routes).");
        prop("destination", "", "Where the train is going (destination signs route it there).");
        prop("route", "", "A list of destinations, comma separated; the next one is picked when one is reached.");
        prop("tags", "", "Labels for signs to check, comma separated.");
        prop("entermessage", "", "Message shown to a player who gets in.");
        prop("owners", "", "Player names who own this train, comma separated.");
    }

    private static void prop(String name, String def, String why) { PROPS.put(name, new String[]{def, why}); }

    private CartData() {}

    public static NBTTagCompound tag(EntityMinecart c) {
        NBTTagCompound root = c.getEntityData();
        if (!root.hasKey(KEY)) root.setTag(KEY, new NBTTagCompound());
        return root.getCompoundTag(KEY);
    }

    public static String get(EntityMinecart c, String name) {
        NBTTagCompound t = tag(c);
        if (t.hasKey(name)) return t.getString(name);
        String[] d = PROPS.get(name);
        return d == null ? "" : d[0];
    }

    public static void set(EntityMinecart c, String name, String value) {
        if (value == null || value.isEmpty() || (PROPS.containsKey(name) && PROPS.get(name)[0].equals(value))) tag(c).removeTag(name);
        else tag(c).setString(name, value);
        if (name.equals("speedlimit")) applySpeed(c);
    }

    public static double num(EntityMinecart c, String name) {
        try { return Double.parseDouble(get(c, name)); } catch (NumberFormatException e) { return Double.parseDouble(PROPS.get(name)[0]); }
    }

    public static boolean bool(EntityMinecart c, String name) {
        String v = get(c, name).toLowerCase(Locale.ROOT);
        return v.equals("true") || v.equals("yes") || v.equals("on") || v.equals("1");
    }

    public static List<String> list(EntityMinecart c, String name) {
        List<String> out = new ArrayList<>();
        for (String s : get(c, name).split(",")) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    public static void setList(EntityMinecart c, String name, List<String> values) { set(c, name, String.join(",", values)); }

    public static boolean hasTag(EntityMinecart c, String tag) {
        for (String t : list(c, "tags")) if (t.equalsIgnoreCase(tag)) return true;
        return false;
    }

    /** speedlimit → Forge's per-cart rail speed cap (and the air speed so jumps don't clamp it) */
    public static void applySpeed(EntityMinecart c) {
        float lim = (float) Math.max(0.01, Math.min(num(c, "speedlimit"), 3.0));
        c.setMaxSpeedAirLateral(lim);
        c.setCurrentCartSpeedCapOnRail(Math.min(lim, c.getMaxCartSpeedOnRail()));
    }

    // ------------------------------------------------------------------ station / blocker hold (not a property)
    /** the train is held here until `until` (world time) or, with until == -1, until the sign's redstone changes */
    public static void hold(EntityMinecart c, long until, long signPos, boolean poweredAtStart, String launchDir, double launchSpeed) {
        NBTTagCompound h = new NBTTagCompound();
        h.setLong("until", until);
        h.setLong("sign", signPos);
        h.setBoolean("powered", poweredAtStart);
        h.setString("dir", launchDir == null ? "" : launchDir);
        h.setDouble("speed", launchSpeed);
        tag(c).setTag("hold", h);
    }

    public static NBTTagCompound hold(EntityMinecart c) { NBTTagCompound t = tag(c); return t.hasKey("hold") ? t.getCompoundTag("hold") : null; }

    public static void release(EntityMinecart c) { tag(c).removeTag("hold"); }

    static NBTTagList stringList(List<String> l) {
        NBTTagList t = new NBTTagList();
        for (String s : l) t.appendTag(new NBTTagString(s));
        return t;
    }
}
