package com.dogpound.pridecarts.blocks;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Redstone on while a minecart (or the named one, from a side sign) is on the rail in front. From Autowork (MIT). */
public class RailwayObserverBlock extends CartBlock {
    public RailwayObserverBlock() {
        super("railway_observer", "Redstone on while a minecart is on the rail in front",
                "A sign on its side with a cart's name = only that cart");
    }

    @Override
    protected boolean work(World w, BlockPos pos, IBlockState s) {
        if (!BlockRailBase.isRailBlock(w, front(pos, s)) && !BlockRailBase.isRailBlock(w, front(pos, s).down())) return false;
        return !carts(w, pos, s).isEmpty();
    }
}
