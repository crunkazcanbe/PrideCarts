package com.dogpound.pridecarts;

import com.dogpound.pridecarts.signs.ActionSign;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The rail network as a graph, for routing trains to destinations. Works on any rail block (vanilla, Railcraft, other
 * mods' BlockRailBase). A "junction" is a normal (flexible) rail touching 3-4 other rails: its shape can be switched.
 * Routes are cached per (junction, entry side, destination) and forgotten whenever a rail or sign is placed/broken.
 */
public final class RailGraph {
    private static final Map<String, EnumFacing> cache = new HashMap<>();
    private static final int MAX_NODES = 30000;

    private RailGraph() {}

    public static void forget() { cache.clear(); }

    // ------------------------------------------------------------------ rail geometry
    static boolean isRail(World w, BlockPos p) { return BlockRailBase.isRailBlock(w, p); }

    static EnumRailDirection shape(World w, BlockPos p) {
        IBlockState s = w.getBlockState(p);
        return ((BlockRailBase) s.getBlock()).getRailDirection(w, p, s, null);
    }

    /** the two sides a rail shape connects */
    static EnumFacing[] ends(EnumRailDirection d) {
        switch (d) {
            case NORTH_SOUTH: case ASCENDING_NORTH: case ASCENDING_SOUTH: return new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH};
            case EAST_WEST: case ASCENDING_EAST: case ASCENDING_WEST: return new EnumFacing[]{EnumFacing.EAST, EnumFacing.WEST};
            case SOUTH_EAST: return new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.EAST};
            case SOUTH_WEST: return new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.WEST};
            case NORTH_WEST: return new EnumFacing[]{EnumFacing.NORTH, EnumFacing.WEST};
            default: return new EnumFacing[]{EnumFacing.NORTH, EnumFacing.EAST};     // NORTH_EAST
        }
    }

    /** the shape joining two sides (null when impossible) */
    public static EnumRailDirection join(EnumFacing a, EnumFacing b) {
        Set<EnumFacing> s = EnumSet.of(a, b);
        if (s.equals(EnumSet.of(EnumFacing.NORTH, EnumFacing.SOUTH))) return EnumRailDirection.NORTH_SOUTH;
        if (s.equals(EnumSet.of(EnumFacing.EAST, EnumFacing.WEST))) return EnumRailDirection.EAST_WEST;
        if (s.equals(EnumSet.of(EnumFacing.SOUTH, EnumFacing.EAST))) return EnumRailDirection.SOUTH_EAST;
        if (s.equals(EnumSet.of(EnumFacing.SOUTH, EnumFacing.WEST))) return EnumRailDirection.SOUTH_WEST;
        if (s.equals(EnumSet.of(EnumFacing.NORTH, EnumFacing.WEST))) return EnumRailDirection.NORTH_WEST;
        if (s.equals(EnumSet.of(EnumFacing.NORTH, EnumFacing.EAST))) return EnumRailDirection.NORTH_EAST;
        return null;
    }

    /** the rail you reach leaving `p` through side `f` (same level, one up, or one down), or null */
    static BlockPos next(World w, BlockPos p, EnumFacing f) {
        BlockPos n = p.offset(f);
        if (isRail(w, n)) return n;
        if (isRail(w, n.up())) return n.up();
        if (isRail(w, n.down())) return n.down();
        return null;
    }

    /** a flexible rail with 3+ rails around it: its shape can be switched */
    public static boolean isJunction(World w, BlockPos p) {
        if (!isRail(w, p)) return false;
        IBlockState s = w.getBlockState(p);
        if (!((BlockRailBase) s.getBlock()).isFlexibleRail(w, p)) return false;
        return sidesWithRail(w, p).size() >= 3;
    }

    static List<EnumFacing> sidesWithRail(World w, BlockPos p) {
        List<EnumFacing> out = new ArrayList<>();
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) if (next(w, p, f) != null) out.add(f);
        return out;
    }

    /** the sides a train can leave this rail by, given the side it came in from */
    static List<EnumFacing> exits(World w, BlockPos p, EnumFacing cameFrom) {
        List<EnumFacing> out = new ArrayList<>();
        if (isJunction(w, p)) {
            for (EnumFacing f : sidesWithRail(w, p)) if (f != cameFrom && join(cameFrom, f) != null) out.add(f);
        } else {
            for (EnumFacing f : ends(shape(w, p))) if (f != cameFrom) out.add(f);
        }
        return out;
    }

    // ------------------------------------------------------------------ routing
    /**
     * Which way to leave junction `j` (entered from side `cameFrom`) to reach the rail above a destination sign named
     * `dest`, by the shortest rail distance. null = no route found.
     */
    public static EnumFacing route(World w, BlockPos j, EnumFacing cameFrom, String dest) {
        String key = w.provider.getDimension() + "|" + j.toLong() + "|" + cameFrom + "|" + dest.toLowerCase();
        if (cache.containsKey(key)) return cache.get(key);
        EnumFacing best = search(w, j, cameFrom, dest);
        cache.put(key, best);
        return best;
    }

    private static final class Step {
        final BlockPos pos; final EnumFacing cameFrom, firstExit;
        Step(BlockPos p, EnumFacing c, EnumFacing f) { pos = p; cameFrom = c; firstExit = f; }
    }

    private static EnumFacing search(World w, BlockPos j, EnumFacing cameFrom, String dest) {
        ArrayDeque<Step> q = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        for (EnumFacing f : exits(w, j, cameFrom)) {
            BlockPos n = next(w, j, f);
            if (n != null) q.add(new Step(n, f.getOpposite(), f));
        }
        int visited = 0;
        while (!q.isEmpty() && visited++ < MAX_NODES) {
            Step s = q.poll();
            if (!w.isBlockLoaded(s.pos)) continue;                          // never load chunks just to route
            if (!seen.add(s.pos.toLong() + "|" + s.cameFrom)) continue;
            if (hasDestination(w, s.pos, dest)) return s.firstExit;
            for (EnumFacing f : exits(w, s.pos, s.cameFrom)) {
                BlockPos n = next(w, s.pos, f);
                if (n != null) q.add(new Step(n, f.getOpposite(), s.firstExit));
            }
        }
        return null;
    }

    static boolean hasDestination(World w, BlockPos rail, String dest) {
        for (ActionSign s : ActionSign.at(w, rail))
            if ((s.action.equals("destination") || s.action.equals("dest")) && s.arg(2).equalsIgnoreCase(dest)) return true;
        return false;
    }

    /** point junction `j` so a train from side `cameFrom` leaves by `exit` (flexible rails only) */
    public static boolean setJunction(World w, BlockPos j, EnumFacing cameFrom, EnumFacing exit) {
        EnumRailDirection want = join(cameFrom, exit);
        if (want == null) return false;
        IBlockState s = w.getBlockState(j);
        BlockRailBase b = (BlockRailBase) s.getBlock();
        if (!b.isFlexibleRail(w, j) || !b.getShapeProperty().getAllowedValues().contains(want)) return false;
        if (s.getValue(b.getShapeProperty()) != want) w.setBlockState(j, s.withProperty(b.getShapeProperty(), want), 2);
        return true;
    }
}
