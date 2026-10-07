package com.dogpound.pridecarts.blocks;

import com.dogpound.pridecarts.RailGraph;
import com.dogpound.pridecarts.carts.CartRegistry;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wide Curve Track (radius 3, 5 or 8): right-click the ground where the curve should start, facing the way the line
 * runs. It sweeps a quarter circle to the right (sneak: to the left). Needs a flat, clear area.
 */
public class ItemCurve extends Item {
    public static final int[] RADII = {3, 5, 8};
    public static final List<ItemCurve> ALL = new ArrayList<>();
    public final int radius;

    public ItemCurve(int radius) {
        this.radius = radius;
        setRegistryName("pridecarts", "curve_r" + radius);
        setUnlocalizedName("pridecarts.curve_r" + radius);
        setCreativeTab(CartRegistry.TAB);
        ALL.add(this);
    }

    public static ItemCurve byRadius(int r) {
        for (ItemCurve c : ALL) if (c.radius == r) return c;
        return ALL.get(0);
    }

    @Override
    public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) {
        tip.add("§7A smooth quarter-circle curve, radius " + radius + " (" + (radius * 2 + 1) + "×" + (radius * 2 + 1) + " area)");
        tip.add("§8Right-click where it starts, facing along the line: turns right · Sneak: turns left");
        tip.add("§8Break any piece to pick the whole curve back up");
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer p, World w, BlockPos pos, EnumHand hand, EnumFacing face, float hx, float hy, float hz) {
        BlockPos start = w.getBlockState(pos).getBlock().isReplaceable(w, pos) ? pos : pos.up();
        if (BlockRailBase.isRailBlock(w, start)) start = start;          // replacing a straight rail end is fine
        EnumFacing dir = p.getHorizontalFacing();
        boolean left = p.isSneaking();
        double tx = dir.getFrontOffsetX(), tz = dir.getFrontOffsetZ();
        double nx = -tz, nz = tx;
        if (left) { nx = -nx; nz = -nz; }
        double sx = start.getX() + 0.5, sz = start.getZ() + 0.5, cx = sx + nx * radius, cz = sz + nz * radius;
        // sample the arc: every block it passes through, with the angle range inside each block
        Map<BlockPos, double[]> pieces = new LinkedHashMap<>();
        BlockPos last = null;
        int steps = radius * 60;
        double first = 0, lastAng = 0;
        for (int i = 0; i <= steps; i++) {
            double phi = Math.PI / 2 * i / steps;
            double px = cx - nx * radius * Math.cos(phi) + tx * radius * Math.sin(phi);
            double pz = cz - nz * radius * Math.cos(phi) + tz * radius * Math.sin(phi);
            double ang = Math.atan2(pz - cz, px - cx);
            if (i > 0) { while (ang - lastAng > Math.PI) ang -= Math.PI * 2; while (ang - lastAng < -Math.PI) ang += Math.PI * 2; }   // keep it continuous
            if (i == 0) first = ang;
            lastAng = ang;
            BlockPos b = new BlockPos(MathHelper.floor(px), start.getY(), MathHelper.floor(pz));
            if (last != null && last.getX() != b.getX() && last.getZ() != b.getZ()) {
                BlockPos mid = new BlockPos(b.getX(), start.getY(), last.getZ());
                final double am = ang;
                pieces.computeIfAbsent(mid, k -> new double[]{am, am});
            }
            final double af = ang;
            double[] r = pieces.computeIfAbsent(b, k -> new double[]{af, af});
            r[0] = Math.min(r[0], ang); r[1] = Math.max(r[1], ang);
            last = b;
        }
        List<BlockPos> order = new ArrayList<>(pieces.keySet());
        for (BlockPos b : order) {
            IBlockState there = w.getBlockState(b);
            boolean ok = there.getBlock().isReplaceable(w, b) || (BlockRailBase.isRailBlock(w, b) && b.equals(start));
            if (!ok || !w.getBlockState(b.down()).isSideSolid(w, b.down(), EnumFacing.UP)) {
                p.sendMessage(new TextComponentString("§cWide curve: needs flat solid ground and room at " + b.getX() + ", " + b.getY() + ", " + b.getZ() + (ok ? " (no ground under it)" : " (" + there.getBlock().getLocalizedName() + " in the way)")));
                return EnumActionResult.FAIL;
            }
        }
        if (w.isRemote) return EnumActionResult.SUCCESS;
        CurveRail block = PcBlocks.CURVE;
        for (int i = 0; i < order.size(); i++) {
            BlockPos b = order.get(i);
            EnumFacing a = i == 0 ? dir.getOpposite() : side(b, order.get(i - 1));
            EnumFacing z = i == order.size() - 1 ? (left ? dir.rotateYCCW() : dir.rotateY()) : side(b, order.get(i + 1));
            BlockRailBase.EnumRailDirection shape = RailGraph.join(a, z);
            if (shape == null) shape = a.getAxis() == EnumFacing.Axis.X ? BlockRailBase.EnumRailDirection.EAST_WEST : BlockRailBase.EnumRailDirection.NORTH_SOUTH;
            w.setBlockState(b, block.getDefaultState().withProperty(CurveRail.SHAPE, shape), 2);
            net.minecraft.tileentity.TileEntity te = w.getTileEntity(b);
            if (te instanceof CurveRail.Tile) {
                CurveRail.Tile t = (CurveRail.Tile) te;
                double[] r = pieces.get(b);
                t.cx = cx; t.cz = cz; t.radius = radius; t.a0 = r[0]; t.a1 = r[1]; t.c0 = first; t.c1 = lastAng; t.origin = order.get(0);
                if (i == 0) t.pieces = new ArrayList<>(order);
                t.markDirty();
                IBlockState st = w.getBlockState(b);
                w.notifyBlockUpdate(b, st, st, 3);
            }
        }
        w.playSound(null, start, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.5f, 1.4f);
        if (!p.capabilities.isCreativeMode) p.getHeldItem(hand).shrink(1);
        return EnumActionResult.SUCCESS;
    }

    /** the side of `from` that `to` (a neighbour) is on */
    private static EnumFacing side(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        if (dx > 0) return EnumFacing.EAST;
        if (dx < 0) return EnumFacing.WEST;
        return dz > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
    }
}
