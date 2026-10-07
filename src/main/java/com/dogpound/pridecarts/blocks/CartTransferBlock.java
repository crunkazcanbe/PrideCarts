package com.dogpound.pridecarts.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

/**
 * Cart Loader (storage behind -> cart in front) and Cart Unloader (cart -> storage). Works on its own whenever a cart
 * with an inventory stands in front (a redstone signal INTO the block pauses it). Its redstone output turns on when the
 * job is done (cart full / storage empty, or cart empty), so a powered rail or launcher sign can send the cart on.
 */
public class CartTransferBlock extends CartBlock {
    private final boolean load;
    static final int PER_TICK = 16;               // items moved every 4 game ticks (= 80 items a second)

    public CartTransferBlock(String name, boolean load) {
        super(name, load ? "Fills the minecart in front from the chest behind" : "Empties the minecart in front into the chest behind",
                "Redstone out turns on when it's done", "Powering it pauses it", "A sign on its side with a cart's name = only that cart");
        this.load = load;
    }

    static IItemHandler handler(Object o, net.minecraft.util.EnumFacing side) {
        if (o instanceof net.minecraftforge.common.capabilities.ICapabilityProvider) {
            net.minecraftforge.common.capabilities.ICapabilityProvider p = (net.minecraftforge.common.capabilities.ICapabilityProvider) o;
            if (p.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) return p.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
        }
        if (o instanceof IInventory) return new InvWrapper((IInventory) o);
        return null;
    }

    @Override
    protected boolean work(World w, BlockPos pos, IBlockState s) {
        if (w.isBlockPowered(pos)) return false;
        TileEntity te = w.getTileEntity(back(pos, s));
        IItemHandler store = te == null ? null : handler(te, s.getValue(FACING));
        if (store == null) return false;
        for (EntityMinecart cart : carts(w, pos, s)) {
            IItemHandler inv = handler(cart, null);
            if (inv == null || inv.getSlots() == 0) continue;
            IItemHandler from = load ? store : inv, to = load ? inv : store;
            int moved = move(from, to, PER_TICK);
            if (moved == 0) return true;      // nothing more to move: full / empty -> done
            return false;
        }
        return false;
    }

    /** move up to n items, stack by stack */
    static int move(IItemHandler from, IItemHandler to, int n) {
        int moved = 0;
        for (int i = 0; i < from.getSlots() && moved < n; i++) {
            ItemStack peek = from.extractItem(i, n - moved, true);
            if (peek.isEmpty()) continue;
            ItemStack rest = ItemHandlerHelper.insertItem(to, peek, true);
            int can = peek.getCount() - rest.getCount();
            if (can <= 0) continue;
            ItemStack got = from.extractItem(i, can, false);
            ItemStack left = ItemHandlerHelper.insertItem(to, got, false);
            if (!left.isEmpty()) ItemHandlerHelper.insertItem(from, left, false);   // never lose anything
            moved += got.getCount() - left.getCount();
        }
        return moved;
    }
}
