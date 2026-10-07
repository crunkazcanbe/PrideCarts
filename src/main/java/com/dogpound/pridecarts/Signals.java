package com.dogpound.pridecarts;

import com.dogpound.pridecarts.blocks.SmartRail;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.List;

/**
 * Block signalling: the track from a Signal Track up to the next Signal Track (or the end of the line, 400 blocks
 * at most) is one section. Only one train may be in a section; a train reaching a Signal Track waits there while the
 * section ahead holds another train. Signal Posts beside a Signal Track show the section's state (green / red).
 */
public final class Signals {
    private Signals() {}

    public static final int MAX_SECTION = 400;

    public static boolean isSignal(World w, BlockPos p) {
        IBlockState s = w.getBlockState(p);
        return s.getBlock() instanceof SmartRail && ((SmartRail) s.getBlock()).kind == SmartRail.Kind.SIGNAL;
    }

    /** true when no cart outside `own` sits on the track from `signal` (exclusive) leaving through `side` up to the next signal */
    public static boolean clear(World w, BlockPos signal, EnumFacing side, Collection<EntityMinecart> own) {
        BlockPos cur = signal;
        for (int i = 0; i < MAX_SECTION; i++) {
            BlockPos nxt = RailGraph.next(w, cur, side);
            if (nxt == null || !w.isBlockLoaded(nxt)) return true;
            List<EntityMinecart> carts = w.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(nxt).expand(0, 0.5, 0).grow(0.05));
            for (EntityMinecart c : carts) if (own == null || !own.contains(c)) return false;
            if (isSignal(w, nxt)) return true;                                        // the section ends at the next signal
            List<EnumFacing> exits = RailGraph.exits(w, nxt, side.getOpposite());
            if (exits.isEmpty()) return true;
            side = exits.get(0);
            cur = nxt;
        }
        return true;
    }

    /** which way along the track a post protects: the side of the signal track the post faces away from */
    public static boolean postClear(World w, BlockPos signalTrack, EnumFacing postFacing) {
        EnumFacing section = postFacing.getOpposite();
        if (!w.isBlockPowered(signalTrack) && clear(w, signalTrack, section, null)) {
            // a train standing on the signal track itself also makes it red
            return w.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(signalTrack).expand(0, 0.5, 0)).isEmpty();
        }
        return false;
    }
}
