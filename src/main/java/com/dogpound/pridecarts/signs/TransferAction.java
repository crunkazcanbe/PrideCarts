package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * transfer — moves items between chests beside the track and the train's storage carts (chest carts, hopper carts…).
 *   line 3: "load" (chests → carts, the default) or "unload" (carts → chests)
 *   line 4: optional filter: an item id ("minecraft:coal") or part of one ("ore"); empty = everything
 * Chests = any inventory block within 2 blocks of the rail. Items are only ever moved: what doesn't fit stays put.
 */
public class TransferAction implements SignAction {
    @Override public String[] names() { return new String[]{"transfer"}; }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        boolean unload = sign.arg(2).trim().toLowerCase(Locale.ROOT).startsWith("un");
        String filter = sign.arg(3).trim().toLowerCase(Locale.ROOT);
        List<IInventory> chests = new ArrayList<>();
        for (BlockPos p : BlockPos.getAllInBox(sign.rail.add(-2, -2, -2), sign.rail.add(2, 2, 2))) {
            TileEntity te = sign.world.getTileEntity(p);
            if (te instanceof IInventory) chests.add((IInventory) te);
        }
        List<IInventory> carts = new ArrayList<>();
        for (EntityMinecart c : train.carts) if (c instanceof IInventory) carts.add((IInventory) c);
        if (chests.isEmpty() || carts.isEmpty()) return;
        if (unload) for (IInventory from : carts) for (IInventory to : chests) move(from, to, filter);
        else for (IInventory from : chests) for (IInventory to : carts) move(from, to, filter);
    }

    static boolean matches(ItemStack s, String filter) {
        return filter.isEmpty() || (s.getItem().getRegistryName() != null && s.getItem().getRegistryName().toString().toLowerCase(Locale.ROOT).contains(filter));
    }

    /** move every matching stack from `from` into `to` (fill matching stacks first, then empty slots) */
    static void move(IInventory from, IInventory to, String filter) {
        boolean changed = false;
        for (int i = 0; i < from.getSizeInventory(); i++) {
            ItemStack s = from.getStackInSlot(i);
            if (s.isEmpty() || !matches(s, filter)) continue;
            int before = s.getCount();
            ItemStack rest = insert(to, s.copy());
            if (rest.getCount() != before) {
                from.setInventorySlotContents(i, rest.isEmpty() ? ItemStack.EMPTY : rest);
                changed = true;
            }
        }
        if (changed) { from.markDirty(); to.markDirty(); }
    }

    /** put a stack into an inventory; returns what didn't fit */
    static ItemStack insert(IInventory inv, ItemStack stack) {
        int limit = inv.getInventoryStackLimit();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {             // pass 0: top up same items; pass 1: empty slots
            for (int j = 0; j < inv.getSizeInventory() && !stack.isEmpty(); j++) {
                if (!inv.isItemValidForSlot(j, stack)) continue;
                ItemStack there = inv.getStackInSlot(j);
                int max = Math.min(limit, stack.getMaxStackSize());
                if (pass == 0 && !there.isEmpty() && ItemStack.areItemsEqual(there, stack) && ItemStack.areItemStackTagsEqual(there, stack) && there.getCount() < max) {
                    int n = Math.min(stack.getCount(), max - there.getCount());
                    there.grow(n);
                    stack.shrink(n);
                    inv.setInventorySlotContents(j, there);
                } else if (pass == 1 && there.isEmpty()) {
                    int n = Math.min(stack.getCount(), max);
                    ItemStack put = stack.copy();
                    put.setCount(n);
                    stack.shrink(n);
                    inv.setInventorySlotContents(j, put);
                }
            }
        }
        return stack;
    }
}
