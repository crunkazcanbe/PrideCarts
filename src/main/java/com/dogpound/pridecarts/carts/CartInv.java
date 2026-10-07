package com.dogpound.pridecarts.carts;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/** Small inventory helpers for the cars (merge into stacks first, then empty slots). */
public final class CartInv {
    private CartInv() {}

    /** Put as much of {@code s} as fits; returns what is left. */
    public static ItemStack insert(IInventory inv, ItemStack s) {
        for (int pass = 0; pass < 2 && !s.isEmpty(); pass++) {
            for (int i = 0; i < inv.getSizeInventory() && !s.isEmpty(); i++) {
                if (!inv.isItemValidForSlot(i, s)) continue;
                ItemStack in = inv.getStackInSlot(i);
                if (pass == 0 && !in.isEmpty() && ItemStack.areItemsEqual(in, s) && ItemStack.areItemStackTagsEqual(in, s)) {
                    int move = Math.min(s.getCount(), Math.min(in.getMaxStackSize(), inv.getInventoryStackLimit()) - in.getCount());
                    if (move > 0) { in.grow(move); s.shrink(move); inv.markDirty(); }
                } else if (pass == 1 && in.isEmpty()) {
                    inv.setInventorySlotContents(i, s.splitStack(Math.min(s.getCount(), inv.getInventoryStackLimit())));
                }
            }
        }
        return s;
    }
}
