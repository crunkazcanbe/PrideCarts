package com.dogpound.pridecarts.blocks;

import com.dogpound.pridecarts.Train;
import com.dogpound.pridecarts.carts.CartRegistry;
import com.dogpound.pridecarts.carts.EntityEngineCart;
import com.dogpound.pridecarts.carts.EntityStorageCart;
import net.minecraft.block.BlockRailDetector;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Detector tracks: like the vanilla detector rail (redstone while a cart is on it), but each only reacts to some
 * carts, and the train-length one gives a stronger signal for longer trains (1 car = 1 … 10+ cars = 15).
 */
public class DetectorTrack extends BlockRailDetector {
    public enum Kind {
        ANY("Detector Track+", "Redstone while any cart is on it (and comparators read how full a cargo car is)."),
        LENGTH("Train Length Detector", "Redstone as strong as the train is long: 1 car = 1, 10 cars = 15."),
        ENGINE("Engine Detector", "Redstone only for mini engines (or trains pulled by one)."),
        PASSENGER("Passenger Detector", "Redstone only for carts with someone riding."),
        FULL("Full Cargo Detector", "Redstone only for storage cars that are full (or trains carrying one).");

        public final String title, tip;
        Kind(String title, String tip) { this.title = title; this.tip = tip; }
        public String id() { return "rail_detect_" + name().toLowerCase(); }
    }

    public final Kind kind;

    public DetectorTrack(Kind kind) {
        this.kind = kind;
        setRegistryName("pridecarts", kind.id());
        setUnlocalizedName("pridecarts." + kind.id());
        setHardness(0.7f);
        setSoundType(SoundType.METAL);
        setCreativeTab(CartRegistry.TAB);
    }

    boolean matches(EntityMinecart c) {
        switch (kind) {
            case ENGINE: for (EntityMinecart t : Train.of(c).carts) if (t instanceof EntityEngineCart) return true; return false;
            case PASSENGER: return !c.getPassengers().isEmpty();
            case FULL: return carriesFull(c);
            default: return true;
        }
    }

    /** does this cart's train carry a completely full storage car? */
    public static boolean carriesFull(EntityMinecart c) {
                for (EntityMinecart t : Train.of(c).carts) {
                    if (!(t instanceof EntityStorageCart)) continue;
                    EntityStorageCart s = (EntityStorageCart) t;
                    boolean full = true;
                    for (int i = 0; i < s.getSizeInventory(); i++) { ItemStack st = s.getStackInSlot(i); if (st.isEmpty() || st.getCount() < st.getMaxStackSize()) { full = false; break; } }
                    if (full) return true;
                }
                return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends EntityMinecart> List<T> findMinecarts(World w, BlockPos pos, Class<T> cls, com.google.common.base.Predicate<Entity>... filter) {
        List<T> all = super.findMinecarts(w, pos, cls, filter);
        List<T> out = new ArrayList<>();
        for (T c : all) if (matches(c)) out.add(c);
        return out;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess w, BlockPos pos, EnumFacing side) {
        if (!state.getValue(POWERED)) return 0;
        if (kind != Kind.LENGTH || !(w instanceof World)) return 15;
        int n = 1;
        for (EntityMinecart c : ((World) w).getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(pos).shrink(0.2)))
            n = Math.max(n, Train.of(c).carts.size());
        return Math.min(15, Math.round(n * 1.5f));
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess w, BlockPos pos, EnumFacing side) {
        return side == EnumFacing.UP ? getWeakPower(state, w, pos, side) : 0;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tip, ITooltipFlag flag) { tip.add("§7" + kind.tip); }
}
