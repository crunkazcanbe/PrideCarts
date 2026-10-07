package com.dogpound.pridecarts.carts;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * No-code engine programming. Station Tracks get a number (right-click with an empty hand; sneak = count down),
 * and an engine carries a route: a list of steps "at station N do X". When a train with a routed engine stops
 * at a numbered station, every step for that number runs.
 * <pre>
 *   wait V     stop V seconds (instead of the usual 5)
 *   skip       don't stop here at all
 *   reverse    leave the way you came
 *   speed V    leave at notch V (0-4)
 *   horn       sound the whistle / horn
 *   unload     empty the storage cars into chests beside the station
 *   load       fill the storage cars from chests beside the station
 *   hold       wait until the station gets a redstone signal
 * </pre>
 * Steps for station 0 run at every station. A route is text: "1:wait:10;2:reverse:0;3:unload:0".
 */
public final class Routes {
    private Routes() {}

    public static final String[] ACTIONS = {"wait", "skip", "reverse", "speed", "horn", "unload", "load", "hold"};
    public static final int MAX_STEPS = 16;

    public static final class Step {
        public int station, value;
        public String action;

        public Step(int station, String action, int value) { this.station = station; this.action = action; this.value = value; }

        @Override public String toString() { return station + ":" + action + ":" + value; }
    }

    public static List<Step> parse(String s) {
        List<Step> out = new ArrayList<>();
        if (s == null) return out;
        for (String part : s.split(";")) {
            String[] f = part.trim().split(":");
            if (f.length < 2 || out.size() >= MAX_STEPS) continue;
            try {
                int st = Math.max(0, Math.min(99, Integer.parseInt(f[0])));
                String a = f[1];
                boolean ok = false;
                for (String x : ACTIONS) if (x.equals(a)) ok = true;
                if (!ok) continue;
                int v = f.length > 2 ? Math.max(0, Math.min(3600, Integer.parseInt(f[2]))) : 0;
                out.add(new Step(st, a, v));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    public static String join(List<Step> steps) {
        StringBuilder b = new StringBuilder();
        for (Step s : steps) { if (b.length() > 0) b.append(';'); b.append(s); }
        return b.toString();
    }

    // ---------------------------------------------------------------- station numbers (saved per world)

    public static final class StationIds extends WorldSavedData {
        static final String NAME = "pridecarts_stations";
        public final Map<Long, Integer> ids = new HashMap<>();

        public StationIds() { super(NAME); }
        public StationIds(String n) { super(n); }

        public static StationIds get(World w) {
            StationIds d = (StationIds) w.getPerWorldStorage().getOrLoadData(StationIds.class, NAME);
            if (d == null) { d = new StationIds(); w.getPerWorldStorage().setData(NAME, d); }
            return d;
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            ids.clear();
            NBTTagList l = nbt.getTagList("s", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < l.tagCount(); i++) ids.put(l.getCompoundTagAt(i).getLong("p"), l.getCompoundTagAt(i).getInteger("n"));
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            NBTTagList l = new NBTTagList();
            for (Map.Entry<Long, Integer> e : ids.entrySet()) { NBTTagCompound t = new NBTTagCompound(); t.setLong("p", e.getKey()); t.setInteger("n", e.getValue()); l.appendTag(t); }
            nbt.setTag("s", l);
            return nbt;
        }
    }

    public static int station(World w, BlockPos p) {
        Integer n = StationIds.get(w).ids.get(p.toLong());
        return n == null ? 0 : n;
    }

    public static int cycle(World w, BlockPos p, int delta) {
        StationIds d = StationIds.get(w);
        int n = Math.floorMod(station(w, p) + delta, 100);
        if (n == 0) d.ids.remove(p.toLong()); else d.ids.put(p.toLong(), n);
        d.markDirty();
        return n;
    }

    public static void forget(World w, BlockPos p) {
        StationIds d = StationIds.get(w);
        if (d.ids.remove(p.toLong()) != null) d.markDirty();
    }

    // ---------------------------------------------------------------- arrival

    /** The routed engine of this train, or null. */
    public static EntityEngineCart routed(EntityMinecart cart) {
        for (EntityMinecart c : Train.of(cart).carts)
            if (c instanceof EntityEngineCart && !((EntityEngineCart) c).route().isEmpty()) return (EntityEngineCart) c;
        return null;
    }

    /**
     * A train reached a Station Track. Returns how many ticks to stop (0 = don't stop, -1 = until redstone),
     * and whether to leave backwards (out[1] == 1). Runs horn/load/unload/speed now.
     */
    public static int[] arrive(World w, BlockPos pos, EntityMinecart cart) {
        EntityEngineCart e = routed(cart);
        int[] out = {100, 0};
        com.dogpound.pridecarts.CartEvents.dbg("station {} reached by cart {} (train {} cars), routed engine: {}", pos, cart.getEntityId(), Train.of(cart).carts.size(), e == null ? "none" : e.route());
        if (e == null) return out;
        int no = station(w, pos);
        com.dogpound.pridecarts.CartEvents.dbg("  station number {}", no);
        for (Step s : parse(e.route())) {
            if (s.station != 0 && s.station != no) continue;
            switch (s.action) {
                case "wait": out[0] = Math.max(1, s.value) * 20; break;
                case "skip": out[0] = 0; break;
                case "hold": out[0] = -1; break;
                case "reverse": out[1] = 1; break;
                case "speed": e.notch(Math.min(EntityEngineCart.NOTCHES, s.value)); break;
                case "horn": e.horn(); break;
                case "unload": transfer(w, pos, cart, true); break;
                case "load": transfer(w, pos, cart, false); break;
                default:
            }
        }
        return out;
    }

    /** Move items between the train's storage cars and every inventory within 2 blocks of the station track. */
    public static int transfer(World w, BlockPos pos, EntityMinecart cart, boolean unload) {
        List<IItemHandler> chests = new ArrayList<>();
        for (BlockPos p : BlockPos.getAllInBox(pos.add(-2, -2, -2), pos.add(2, 2, 2))) {
            TileEntity te = w.getTileEntity(p);
            if (te == null) continue;
            IItemHandler h = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
            if (h != null) chests.add(h);
        }
        if (chests.isEmpty()) return 0;
        int moved = 0;
        for (EntityMinecart c : Train.of(cart).carts) {
            if (!(c instanceof EntityStorageCart)) continue;
            EntityStorageCart s = (EntityStorageCart) c;
            if (unload) {
                for (int i = 0; i < s.getSizeInventory(); i++) {
                    ItemStack st = s.getStackInSlot(i);
                    if (st.isEmpty()) continue;
                    for (IItemHandler h : chests) {
                        st = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(h, st, false);
                        if (st.isEmpty()) break;
                    }
                    moved += s.getStackInSlot(i).getCount() - st.getCount();
                    s.setInventorySlotContents(i, st);
                }
            } else {
                for (IItemHandler h : chests) for (int j = 0; j < h.getSlots(); j++) {
                    ItemStack take = h.extractItem(j, 64, true);
                    if (take.isEmpty()) continue;
                    ItemStack left = CartInv.insert(s, take.copy());
                    int n = take.getCount() - left.getCount();
                    if (n > 0) { h.extractItem(j, n, false); moved += n; }
                }
            }
        }
        if (moved > 0) CartData.set(cart, "lastTransfer", String.valueOf(moved));
        return moved;
    }
}
