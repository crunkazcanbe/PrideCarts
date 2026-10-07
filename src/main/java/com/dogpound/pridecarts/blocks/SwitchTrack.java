package com.dogpound.pridecarts.blocks;

import com.dogpound.pridecarts.Train;
import com.dogpound.pridecarts.carts.CartRegistry;
import com.dogpound.pridecarts.carts.EntityEngineCart;
import com.dogpound.pridecarts.carts.EntityStorageCart;
import com.dogpound.pridecarts.carts.EntityTankCart;
import net.minecraft.block.BlockRail;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

/**
 * Switch tracks (points). Place one where a branch meets the line, so it curves into the branch. Carts that
 * match its rule take the curve; everything else goes straight through, along the way it was already rolling.
 * Railcraft-style sorting with no signs or programming.
 */
public class SwitchTrack extends BlockRail {
    public enum Kind {
        PASSENGER("Passenger Switch", "Carts with riders take the curve; empty carts go straight."),
        ENGINE("Engine Switch", "Trains with a mini engine take the curve; loose carts go straight."),
        CARGO("Cargo Switch", "Trains with a storage car take the curve; the rest go straight."),
        TANK("Tank Switch", "Trains with a tank car take the curve; the rest go straight."),
        REDSTONE("Redstone Switch", "Powered: carts take the curve. Unpowered: straight on."),
        RANDOM("Random Switch", "Each train flips a coin: curve or straight."),
        ALTERNATE("Alternating Switch", "Every other train takes the curve."),
        FULL("Full Cargo Switch", "Trains with a full storage car take the curve (sends full trains to unload)."),
        MANUAL("Manual Turnout", "A straight and a curve in one: right-click it (empty hand) to flip between them. Redstone flips it too.");

        public final String title, tip;
        Kind(String title, String tip) { this.title = title; this.tip = tip; }
        public String id() { return "rail_switch_" + name().toLowerCase(); }
    }

    public final Kind kind;

    public SwitchTrack(Kind kind) {
        super();
        this.kind = kind;
        setRegistryName("pridecarts", kind.id());
        setUnlocalizedName("pridecarts." + kind.id());
        setHardness(0.7f);
        setSoundType(SoundType.METAL);
        setCreativeTab(CartRegistry.TAB);
    }

    private static boolean curve(EnumRailDirection d) {
        return d == EnumRailDirection.NORTH_EAST || d == EnumRailDirection.NORTH_WEST || d == EnumRailDirection.SOUTH_EAST || d == EnumRailDirection.SOUTH_WEST;
    }

    @Override
    public EnumRailDirection getRailDirection(IBlockAccess w, BlockPos pos, IBlockState state, EntityMinecart cart) {
        EnumRailDirection placed = state.getValue(SHAPE);
        if (cart == null || !(w instanceof World)) return placed;
        boolean ew = Math.abs(cart.motionX) >= Math.abs(cart.motionZ);
        if (Math.abs(cart.motionX) < 1e-3 && Math.abs(cart.motionZ) < 1e-3) return placed;
        if (!decide((World) w, pos, cart)) return ew ? EnumRailDirection.EAST_WEST : EnumRailDirection.NORTH_SOUTH;   // straight on
        // curve from the side the train came in toward the branch (a track on one of the two other sides)
        net.minecraft.util.EnumFacing entry = ew ? (cart.motionX > 0 ? net.minecraft.util.EnumFacing.WEST : net.minecraft.util.EnumFacing.EAST)
                : (cart.motionZ > 0 ? net.minecraft.util.EnumFacing.NORTH : net.minecraft.util.EnumFacing.SOUTH);
        net.minecraft.util.EnumFacing[] sides = ew ? new net.minecraft.util.EnumFacing[]{net.minecraft.util.EnumFacing.SOUTH, net.minecraft.util.EnumFacing.NORTH}
                : new net.minecraft.util.EnumFacing[]{net.minecraft.util.EnumFacing.EAST, net.minecraft.util.EnumFacing.WEST};
        for (net.minecraft.util.EnumFacing b : sides) {
            BlockPos n = pos.offset(b);
            if (isRailBlock((World) w, n) || isRailBlock((World) w, n.down()) || isRailBlock((World) w, n.up())) return join(entry, b);
        }
        return ew ? EnumRailDirection.EAST_WEST : EnumRailDirection.NORTH_SOUTH;                                      // no branch: straight
    }

    private static EnumRailDirection join(net.minecraft.util.EnumFacing a, net.minecraft.util.EnumFacing b) {
        boolean n = a == net.minecraft.util.EnumFacing.NORTH || b == net.minecraft.util.EnumFacing.NORTH;
        boolean e = a == net.minecraft.util.EnumFacing.EAST || b == net.minecraft.util.EnumFacing.EAST;
        return n ? (e ? EnumRailDirection.NORTH_EAST : EnumRailDirection.NORTH_WEST) : (e ? EnumRailDirection.SOUTH_EAST : EnumRailDirection.SOUTH_WEST);
    }

    /** decided once per cart per visit (so it can't change its mind halfway round) */
    private boolean decide(World w, BlockPos pos, EntityMinecart cart) {
        net.minecraft.nbt.NBTTagCompound d = cart.getEntityData();
        long now = w.getTotalWorldTime();
        if (d.getLong("pcSwPos") == pos.toLong() && now - d.getLong("pcSwT") < 40) { d.setLong("pcSwT", now); return d.getBoolean("pcSwCurve"); }
        boolean c = rule(w, pos, cart);
        // the whole train takes the same way
        for (EntityMinecart t : Train.of(cart).carts) {
            net.minecraft.nbt.NBTTagCompound td = t.getEntityData();
            td.setLong("pcSwPos", pos.toLong());
            td.setLong("pcSwT", now + 200);
            td.setBoolean("pcSwCurve", c);
        }
        return c;
    }

    private static int alternate = 0;

    private boolean rule(World w, BlockPos pos, EntityMinecart cart) {
        List<EntityMinecart> train = Train.of(cart).carts;
        switch (kind) {
            case PASSENGER: for (EntityMinecart t : train) if (!t.getPassengers().isEmpty()) return true; return false;
            case ENGINE: for (EntityMinecart t : train) if (t instanceof EntityEngineCart) return true; return false;
            case CARGO: for (EntityMinecart t : train) if (t instanceof EntityStorageCart || t instanceof net.minecraft.entity.item.EntityMinecartContainer) return true; return false;
            case TANK: for (EntityMinecart t : train) if (t instanceof EntityTankCart) return true; return false;
            case REDSTONE: return w.isBlockPowered(pos);
            case RANDOM: return w.rand.nextBoolean();
            case ALTERNATE: return (alternate++ & 1) == 0;
            case FULL: return DetectorTrack.carriesFull(cart);
            case MANUAL: return (com.dogpound.pridecarts.carts.Routes.station(w, pos) == 1) != w.isBlockPowered(pos);
            default: return false;
        }
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState state, net.minecraft.entity.player.EntityPlayer p, net.minecraft.util.EnumHand hand, net.minecraft.util.EnumFacing face, float hx, float hy, float hz) {
        if (kind != Kind.MANUAL || !p.getHeldItem(hand).isEmpty()) return false;
        if (!w.isRemote) {
            // the turnout's setting lives in the same saved number table as stations: 1 = curve, none = straight
            boolean curve = com.dogpound.pridecarts.carts.Routes.station(w, pos) != 1;
            com.dogpound.pridecarts.carts.Routes.forget(w, pos);
            if (curve) com.dogpound.pridecarts.carts.Routes.cycle(w, pos, 1);
            p.sendStatusMessage(new net.minecraft.util.text.TextComponentString(curve ? "§d↪ Turnout set to the curve" : "§d⇡ Turnout set to straight"), true);
            w.playSound(null, pos, net.minecraft.init.SoundEvents.BLOCK_LEVER_CLICK, net.minecraft.util.SoundCategory.BLOCKS, 0.6f, curve ? 0.7f : 0.5f);
        }
        return true;
    }

    @Override
    public void breakBlock(World w, BlockPos pos, IBlockState state) {
        if (kind == Kind.MANUAL && !w.isRemote) com.dogpound.pridecarts.carts.Routes.forget(w, pos);
        super.breakBlock(w, pos, state);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tip, ITooltipFlag flag) {
        tip.add("§7" + kind.tip);
        tip.add("§8Place it where the branch joins so it curves into the branch");
    }
}
