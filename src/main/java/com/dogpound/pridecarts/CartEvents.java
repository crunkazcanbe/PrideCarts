package com.dogpound.pridecarts;

import com.dogpound.pridecarts.signs.ActionSign;
import com.dogpound.pridecarts.signs.SignAction;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** The loop: each server tick, for each cart on a rail — held carts wait, new rails get their signs read, properties apply. */
public final class CartEvents {
    private static final Map<EntityMinecart, Long> lastRail = new WeakHashMap<>();
    private static final Map<EntityMinecart, Double> lastSpeed = new WeakHashMap<>();
    private static final Map<String, Long> firedTrain = new java.util.HashMap<>();   // "sign|train" → time, so [train] signs fire once per train
    /** set while a sign ejects players, so "playerexit = false" doesn't block it */
    public static boolean ejecting;

    private CartEvents() {}

    @SubscribeEvent
    public static void update(MinecartUpdateEvent e) {
        EntityMinecart c = e.getMinecart();
        if (c.world.isRemote) return;
        NBTTagCompound hold = CartData.hold(c);
        if (hold != null) { holdTick(c, hold); return; }

        BlockPos rail = e.getPos();
        boolean onRail = BlockRailBase.isRailBlock(c.world, rail);
        if (onRail) {
            Long prev = lastRail.get(c);
            if (prev == null || prev != rail.toLong()) {
                lastRail.put(c, rail.toLong());
                try { signs(c, rail); } catch (Exception ex) { PrideCarts.LOG.error("sign at {} failed", rail, ex); }
                try { autoRoute(c, rail); } catch (Exception ex) { PrideCarts.LOG.error("routing at {} failed", rail, ex); }
            }
        }
        if (!CartData.bool(c, "slowdown") && CartData.hold(c) == null) keepSpeed(c);
        else lastSpeed.remove(c);
    }

    /** a file named "pridecarts-debug" in the game folder = log every sign decision (for testing) */
    public static final boolean DEBUG = new java.io.File("pridecarts-debug").isFile();

    public static void dbg(String fmt, Object... args) { if (DEBUG) PrideCarts.LOG.info("[debug] " + fmt, args); }

    private static void signs(EntityMinecart c, BlockPos rail) {
        List<ActionSign> found = ActionSign.at(c.world, rail);
        dbg("cart {} ({}) entered rail {} motion {},{} — {} sign(s)", c.getEntityId(), c.getClass().getSimpleName(), rail, String.format("%.3f", c.motionX), String.format("%.3f", c.motionZ), found.size());
        if (found.isEmpty()) return;
        long now = c.world.getTotalWorldTime();
        for (ActionSign s : found) {
            SignAction a = SignAction.ALL.get(s.action);
            if (a == null) { dbg("  sign {} action '{}' unknown", s.pos, s.action); continue; }
            Train t = s.perCart ? Train.single(c) : Train.of(c);
            if (!s.perCart) {                                                  // [train]: once per train, not per cart
                String key = s.pos.toLong() + "|" + t.key();
                Long at = firedTrain.get(key);
                if (at != null && now - at < 100) continue;
                firedTrain.put(key, now);
            }
            dbg("  sign {} '{}' mode={} active={} watches({})={}", s.pos, s.action, s.mode, s.active(), t.moving(), s.watches(t.moving()));
            if (!s.active() || !s.watches(t.moving())) continue;
            if (!s.action.equals("skip") && com.dogpound.pridecarts.signs.SkipAction.consume(c)) continue;   // a skip sign said to ignore this one
            a.enter(s, c, t);
            dbg("  ran '{}' → motion now {},{} hold={}", s.action, String.format("%.3f", c.motionX), String.format("%.3f", c.motionZ), CartData.hold(c));
            if (CartData.hold(c) != null) break;                                // a station/blocker took it: later signs wait
        }
        if (firedTrain.size() > 4096) firedTrain.entrySet().removeIf(en -> now - en.getValue() > 200);
    }

    /** a train with a destination steers itself at junctions that have no switcher sign (TrainCarts needs a sign; we don't) */
    private static void autoRoute(EntityMinecart c, BlockPos rail) {
        if (CartData.hold(c) != null || !RailGraph.isJunction(c.world, rail)) return;
        String dest = CartData.get(c, "destination");
        if (dest.isEmpty() || !PrideCarts.autoRoute) return;
        for (ActionSign s : ActionSign.at(c.world, rail)) if (s.action.equals("switcher") || s.action.equals("switch") || s.action.equals("tag")) return;
        Train t = Train.of(c);
        if (c != t.head()) return;                                          // the front cart steers; the rest follow the rails
        EnumFacing moving = t.moving();
        if (moving == null) return;
        EnumFacing exit = RailGraph.route(c.world, rail, moving.getOpposite(), dest);
        if (exit != null) RailGraph.setJunction(c.world, rail, moving.getOpposite(), exit);
    }

    @SubscribeEvent
    public static void placed(net.minecraftforge.event.world.BlockEvent.PlaceEvent e) { if (!e.getWorld().isRemote) RailGraph.forget(); }

    @SubscribeEvent
    public static void broken(net.minecraftforge.event.world.BlockEvent.BreakEvent e) { if (!e.getWorld().isRemote) RailGraph.forget(); }

    // ------------------------------------------------------------------ stations and blockers
    private static void holdTick(EntityMinecart c, NBTTagCompound h) {
        c.motionX = 0; c.motionZ = 0;
        long until = h.getLong("until");
        BlockPos signPos = BlockPos.fromLong(h.getLong("sign"));
        boolean go;
        if (h.hasKey("signalDir")) go = !c.world.isBlockPowered(signPos) && Signals.clear(c.world, signPos, EnumFacing.getFront(h.getInteger("signalDir")), Train.of(c).carts);
        else if (h.getBoolean("blocker")) go = !blockerActive(c, signPos);
        else if (h.getBoolean("waiter")) go = com.dogpound.pridecarts.signs.WaiterAction.clear(c, h);
        else if (h.hasKey("mutex")) {
            ActionSign ms = ActionSign.read(c.world, signPos);
            int radius = 8;
            try { if (ms != null && !ms.arg(3).trim().isEmpty()) radius = Integer.parseInt(ms.arg(3).trim()); } catch (NumberFormatException ignored) {}
            go = com.dogpound.pridecarts.signs.MutexAction.free(c.world, h.getString("mutex"), Train.of(c).key(), signPos, radius);
        }
        else if (until >= 0) go = c.world.getTotalWorldTime() >= until;
        else go = (c.world.isBlockIndirectlyGettingPowered(signPos) > 0) != h.getBoolean("powered");   // redstone changed
        if (DEBUG && c.world.getTotalWorldTime() % 20 == 0)
            dbg("hold cart {} until={} now={} go={} dir={} speed={}", c.getEntityId(), until, c.world.getTotalWorldTime(), go, h.getString("dir"), h.getDouble("speed"));
        if (!go || "none".equals(h.getString("dir"))) return;
        dbg("releasing cart {} → {} at {}", c.getEntityId(), h.getString("dir"), h.getDouble("speed"));
        Train t = Train.of(c);
        EnumFacing dir = EnumFacing.byName(h.getString("dir"));
        double speed = h.getDouble("speed");
        for (EntityMinecart m : t.carts) { CartData.release(m); lastRail.put(m, m.getPosition().toLong()); }
        ActionSign.outputAt(c.world, signPos, false);
        if (speed > 0) t.launch(dir, speed);
    }

    private static boolean blockerActive(EntityMinecart c, BlockPos signPos) {
        ActionSign s = ActionSign.read(c.world, signPos);
        return s != null && s.active();                                        // sign gone or switched off: let it go
    }

    // ------------------------------------------------------------------ properties
    private static void keepSpeed(EntityMinecart c) {
        double sp = Math.sqrt(c.motionX * c.motionX + c.motionZ * c.motionZ);
        Double prev = lastSpeed.get(c);
        if (prev != null && sp > 0.01 && sp < prev) {
            double k = prev / sp;
            c.motionX *= k; c.motionZ *= k;
            sp = prev;
        }
        lastSpeed.put(c, sp);
    }

    @SubscribeEvent
    public static void join(EntityJoinWorldEvent e) {
        if (!e.getWorld().isRemote && e.getEntity() instanceof EntityMinecart) {
            CartData.applySpeed((EntityMinecart) e.getEntity());
            dbg("cart {} joined: {} at {}", e.getEntity().getEntityId(), e.getEntity().getClass().getName(), e.getEntity().getPosition());
        }
    }

    @SubscribeEvent
    public static void mount(EntityMountEvent e) {
        if (e.getWorldObj().isRemote || !(e.getEntityBeingMounted() instanceof EntityMinecart)) return;
        EntityMinecart c = (EntityMinecart) e.getEntityBeingMounted();
        Entity who = e.getEntityMounting();
        if (e.isDismounting()) {
            if (who instanceof EntityPlayer && !ejecting && !CartData.bool(c, "playerexit") && c.isEntityAlive()) {
                e.setCanceled(true);
                ((EntityPlayer) who).sendStatusMessage(new TextComponentString("§c✖ You can't get out of this train here"), true);
            }
            return;
        }
        if (who instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) who;
            boolean owner = CartData.list(c, "owners").stream().anyMatch(n -> n.equalsIgnoreCase(p.getName()));
            if (!CartData.bool(c, "playerenter") || (CartData.bool(c, "ownersonly") && !owner && !CartData.list(c, "owners").isEmpty())) {
                e.setCanceled(true);
                p.sendStatusMessage(new TextComponentString("§c✖ You can't get in this train"), true);
                return;
            }
            String msg = CartData.get(c, "entermessage");
            if (!msg.isEmpty()) p.sendMessage(new TextComponentString(msg.replace('&', '§')));
        } else if (!CartData.bool(c, "mobenter")) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent e) {
        if (e.getTarget() instanceof EntityMinecart && CartData.bool((EntityMinecart) e.getTarget(), "invincible")) e.setCanceled(true);
    }

    static List<EntityMinecart> none() { return Collections.emptyList(); }
}
