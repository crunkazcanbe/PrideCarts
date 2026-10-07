package com.dogpound.pridecarts.blocks;

import com.dogpound.pridecarts.carts.CartRegistry;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Wide (gentle) curve track. One item lays a whole quarter circle of radius 3, 5 or 8 as a chain of these blocks.
 * Underneath it is ordinary rail (each block's shape joins its neighbours, so carts never derail), but every tick a
 * cart on it is moved onto the true arc and turned along it, so the ride is a smooth sweep instead of zig-zag
 * corners. The rails themselves are drawn as a real curve by {@link CurveRenderer}.
 */
public class CurveRail extends BlockRailBase implements ITileEntityProvider {
    public static final PropertyEnum<EnumRailDirection> SHAPE = PropertyEnum.create("shape", EnumRailDirection.class,
            d -> !d.isAscending());

    public CurveRail() {
        super(false);
        setRegistryName("pridecarts", "curve_rail");
        setUnlocalizedName("pridecarts.curve_rail");
        setHardness(0.7f);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(SHAPE, EnumRailDirection.NORTH_SOUTH));
    }

    @Override public IProperty<EnumRailDirection> getShapeProperty() { return SHAPE; }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, SHAPE); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(SHAPE, EnumRailDirection.byMetadata(m)); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(SHAPE).getMetadata(); }
    @Override public EnumBlockRenderType getRenderType(IBlockState s) { return EnumBlockRenderType.INVISIBLE; }   // drawn by CurveRenderer
    @Override protected void updateState(IBlockState s, World w, BlockPos p, net.minecraft.block.Block b) {}         // keep the laid shape
    @Override protected IBlockState updateDir(World w, BlockPos p, IBlockState s, boolean placing) { return s; }
    @Override public boolean isFlexibleRail(IBlockAccess w, BlockPos p) { return false; }
    @Override public TileEntity createNewTileEntity(World w, int meta) { return new Tile(); }
    @Override public int quantityDropped(java.util.Random r) { return 0; }

    /** breaking any piece removes the whole curve and gives the item back */
    @Override
    public void breakBlock(World w, BlockPos pos, IBlockState state) {
        TileEntity te = w.getTileEntity(pos);
        super.breakBlock(w, pos, state);
        if (w.isRemote || !(te instanceof Tile) || ((Tile) te).removing) return;
        Tile t = (Tile) te;
        TileEntity ot = w.getTileEntity(t.origin);
        List<BlockPos> all = ot instanceof Tile ? ((Tile) ot).pieces : t.pieces;
        for (BlockPos p : all) {
            TileEntity pt = w.getTileEntity(p);
            if (pt instanceof Tile) ((Tile) pt).removing = true;
            if (!p.equals(pos) && w.getBlockState(p).getBlock() == this) w.setBlockToAir(p);
        }
        net.minecraft.inventory.InventoryHelper.spawnItemStack(w, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ItemCurve.byRadius(t.radius)));
    }

    // ---------------------------------------------------------------- the smooth ride

    @Override
    public void onMinecartPass(World w, EntityMinecart cart, BlockPos pos) {
        TileEntity te = w.getTileEntity(pos);
        if (!(te instanceof Tile)) return;
        Tile t = (Tile) te;
        double lo = Math.min(t.c0, t.c1), hi = Math.max(t.c0, t.c1), mid = (lo + hi) / 2;
        NBTTagCompound d = cart.getEntityData();
        long now = w.getTotalWorldTime();
        double sp = Math.sqrt(cart.motionX * cart.motionX + cart.motionZ * cart.motionZ);
        double ang, dir;
        if (now - d.getLong("pcCurveT") <= 2 && d.getLong("pcCurveO") == t.origin.toLong()) {
            // already on this curve: move along the arc ourselves (the zig-zag rail underneath can't brake or bend it)
            sp = Math.max(sp, d.getDouble("pcCurveV") * 0.999);
            dir = d.getDouble("pcCurveD");
            ang = d.getDouble("pcCurveA") + dir * sp / t.radius;
        } else {
            double dx = cart.posX - t.cx, dz = cart.posZ - t.cz;
            ang = Tile.norm(Math.atan2(dz, dx), mid);
            if (ang < lo - 0.05 || ang > hi + 0.05) return;
            double tx = -Math.sin(ang), tz = Math.cos(ang);       // +angle direction
            dir = cart.motionX * tx + cart.motionZ * tz >= 0 ? 1 : -1;
        }
        boolean done = ang > hi || ang < lo;
        if (done) ang = Math.max(lo, Math.min(hi, ang));
        double tx = -Math.sin(ang) * dir, tz = Math.cos(ang) * dir;
        double x = t.cx + Math.cos(ang) * t.radius, z = t.cz + Math.sin(ang) * t.radius;
        if (done) { x += tx * 0.35; z += tz * 0.35; }             // hand over to the straight track beyond the end
        cart.setPosition(x, cart.posY, z);
        cart.motionX = tx * sp;
        cart.motionZ = tz * sp;
        cart.rotationYaw = (float) Math.toDegrees(Math.atan2(tz, tx));
        if (done) { d.setLong("pcCurveT", 0); return; }
        d.setLong("pcCurveT", now);
        d.setLong("pcCurveO", t.origin.toLong());
        d.setDouble("pcCurveA", ang);
        d.setDouble("pcCurveV", sp);
        d.setDouble("pcCurveD", dir);
    }

    // ---------------------------------------------------------------- tile: the curve this piece belongs to

    public static class Tile extends TileEntity {
        public double cx, cz;
        public int radius = 5;
        /** this piece's angle range (radians, centre → piece), and the whole curve's */
        public double a0, a1, c0, c1;
        public BlockPos origin = BlockPos.ORIGIN;
        public List<BlockPos> pieces = new ArrayList<>();
        boolean removing;

        boolean within(double a) {
            double lo = Math.min(c0, c1) - 0.02, hi = Math.max(c0, c1) + 0.02;
            a = norm(a, (lo + hi) / 2);
            return a >= lo && a <= hi;
        }

        static double norm(double a, double around) {
            while (a < around - Math.PI) a += Math.PI * 2;
            while (a > around + Math.PI) a -= Math.PI * 2;
            return a;
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound n) {
            super.writeToNBT(n);
            n.setDouble("cx", cx); n.setDouble("cz", cz); n.setInteger("r", radius);
            n.setDouble("a0", a0); n.setDouble("a1", a1); n.setDouble("c0", c0); n.setDouble("c1", c1);
            n.setLong("origin", origin.toLong());
            NBTTagList l = new NBTTagList();
            for (BlockPos p : pieces) l.appendTag(new net.minecraft.nbt.NBTTagLong(p.toLong()));
            n.setTag("pieces", l);
            return n;
        }

        @Override
        public void readFromNBT(NBTTagCompound n) {
            super.readFromNBT(n);
            cx = n.getDouble("cx"); cz = n.getDouble("cz"); radius = n.getInteger("r");
            a0 = n.getDouble("a0"); a1 = n.getDouble("a1"); c0 = n.getDouble("c0"); c1 = n.getDouble("c1");
            origin = BlockPos.fromLong(n.getLong("origin"));
            pieces.clear();
            NBTTagList l = n.getTagList("pieces", Constants.NBT.TAG_LONG);
            for (int i = 0; i < l.tagCount(); i++) pieces.add(BlockPos.fromLong(((net.minecraft.nbt.NBTTagLong) l.get(i)).getLong()));
        }

        @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
        @Override public net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket() { return new net.minecraft.network.play.server.SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }
        @Override public void onDataPacket(net.minecraft.network.NetworkManager net, net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) { readFromNBT(pkt.getNbtCompound()); }
        @Override public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() { return new net.minecraft.util.math.AxisAlignedBB(pos).grow(1); }
    }
}
