package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.Train;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One TrainCarts-style action sign, read the same way TrainCarts reads them.
 *
 * Line 1 header:  [train]  needs redstone power (the default)      [cart]   = acts on each cart, not the whole train
 *                 [+train] always on    [!train] on until powered  [-train] always off
 *                 [/train] [\train] [/\train]  pulse modes — on a train's arrival they act like [train]
 *   directions:   [train:lr] [train:n] [train:*] — f/b/l/r relative to the sign, n/e/s/w on the compass, * = any.
 *                 Without them a sign reacts to trains coming toward its text (moving the way the sign's back faces),
 *                 or both ways when the track runs across the sign.
 * Line 2: the action ("station", "launcher", ...).   Lines 3-4: that action's settings.
 * Placement: under the rail (stack as many as you like down the pillar), or on the sides of that pillar.
 */
public final class ActionSign {
    public enum Mode { POWERED, ALWAYS, INVERTED, OFF, PULSE_ON, PULSE_OFF, PULSE_ANY }

    private static final Pattern HEADER = Pattern.compile("^\\[\\s*(\\+|!|-|/\\\\|/|\\\\)?\\s*(train|cart)\\s*(?::\\s*([a-z*]+))?\\s*]$", Pattern.CASE_INSENSITIVE);

    public final World world;
    public final BlockPos pos, rail;
    public final TileEntitySign tile;
    public final EnumFacing facing;          // the side the text is on
    public final String[] lines = new String[4];
    public final Mode mode;
    public final boolean perCart;
    public final String action;
    private final Set<EnumFacing> dirs;      // empty = default rule

    private ActionSign(World w, BlockPos pos, BlockPos rail, TileEntitySign t, EnumFacing facing, Matcher m) {
        this.world = w; this.pos = pos; this.rail = rail; this.tile = t; this.facing = facing;
        for (int i = 0; i < 4; i++) lines[i] = t.signText[i] == null ? "" : t.signText[i].getUnformattedText().trim();
        String sym = m.group(1) == null ? "" : m.group(1);
        mode = sym.equals("+") ? Mode.ALWAYS : sym.equals("!") ? Mode.INVERTED : sym.equals("-") ? Mode.OFF
                : sym.equals("/\\") ? Mode.PULSE_ANY : sym.equals("/") ? Mode.PULSE_ON : sym.equals("\\") ? Mode.PULSE_OFF : Mode.POWERED;
        perCart = m.group(2).equalsIgnoreCase("cart");
        action = lines[1].toLowerCase(Locale.ROOT).replace(" ", "");
        dirs = parseDirs(m.group(3), facing);
    }

    /** every action sign belonging to the rail at `rail` */
    public static List<ActionSign> at(World w, BlockPos rail) {
        List<ActionSign> out = new ArrayList<>();
        for (int dy = 1; dy <= 6; dy++) {
            BlockPos col = rail.down(dy);
            add(w, col, rail, out);                                          // a sign in the pillar itself
            for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {            // wall signs hung on the pillar
                BlockPos p = col.offset(side);
                IBlockState s = w.getBlockState(p);
                if (s.getBlock() instanceof BlockWallSign && s.getValue(BlockWallSign.FACING) == side) add(w, p, rail, out);
            }
        }
        return out;
    }

    private static void add(World w, BlockPos p, BlockPos rail, List<ActionSign> out) {
        TileEntity te = w.getTileEntity(p);
        if (!(te instanceof TileEntitySign)) return;
        TileEntitySign t = (TileEntitySign) te;
        if (t.signText[0] == null) return;
        Matcher m = HEADER.matcher(t.signText[0].getUnformattedText().trim());
        if (!m.matches()) return;
        IBlockState s = w.getBlockState(p);
        EnumFacing f = s.getBlock() instanceof BlockWallSign ? s.getValue(BlockWallSign.FACING)
                : s.getBlock() instanceof BlockStandingSign ? EnumFacing.fromAngle(s.getValue(BlockStandingSign.ROTATION) * 22.5) : EnumFacing.NORTH;
        for (ActionSign a : out) if (a.pos.equals(p)) return;
        out.add(new ActionSign(w, p, rail, t, f, m));
    }

    // ------------------------------------------------------------------ redstone + direction rules
    public boolean powered() { return world.isBlockIndirectlyGettingPowered(pos) > 0; }

    /** does the sign react right now (redstone mode) */
    public boolean active() {
        switch (mode) {
            case ALWAYS: return true;
            case OFF: return false;
            case INVERTED: return !powered();
            default: return powered();                      // [train] and the pulse modes, for an arriving train
        }
    }

    /** does a train moving `moving` (null = standing still) trigger this sign */
    public boolean watches(EnumFacing moving) {
        if (moving == null) return true;
        if (!dirs.isEmpty()) return dirs.contains(moving);
        if (moving.getAxis() == facing.getAxis()) return moving == facing.getOpposite();
        return true;                                        // track runs across the sign: both ways
    }

    private static Set<EnumFacing> parseDirs(String s, EnumFacing facing) {
        Set<EnumFacing> out = EnumSet.noneOf(EnumFacing.class);
        if (s == null || s.isEmpty()) return out;
        EnumFacing fwd = facing.getOpposite();
        for (char c : s.toLowerCase(Locale.ROOT).toCharArray()) {
            switch (c) {
                case '*': for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) out.add(f); break;
                case 'n': out.add(EnumFacing.NORTH); break;
                case 'e': out.add(EnumFacing.EAST); break;
                case 's': out.add(EnumFacing.SOUTH); break;
                case 'w': out.add(EnumFacing.WEST); break;
                case 'f': out.add(fwd); break;
                case 'b': out.add(fwd.getOpposite()); break;
                case 'l': out.add(fwd.rotateYCCW()); break;
                case 'r': out.add(fwd.rotateY()); break;
                default: break;
            }
        }
        return out;
    }

    public String arg(int line) { return lines[line]; }

    /** the action sign at exactly this spot (rail = the rail above it, if any), or null */
    public static ActionSign read(World w, BlockPos signPos) {
        List<ActionSign> tmp = new ArrayList<>();
        add(w, signPos, signPos.up(), tmp);
        return tmp.isEmpty() ? null : tmp.get(0);
    }

    public void output(boolean on) { outputAt(world, pos, on); }

    /** a lever on (or next to) the sign: stations switch it on while a train waits */
    public static void outputAt(World world, BlockPos pos, boolean on) {
        for (EnumFacing f : EnumFacing.values()) {
            BlockPos p = pos.offset(f);
            IBlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof net.minecraft.block.BlockLever && s.getValue(net.minecraft.block.BlockLever.POWERED) != on) {
                world.setBlockState(p, s.withProperty(net.minecraft.block.BlockLever.POWERED, on), 3);
                world.notifyNeighborsOfStateChange(p, s.getBlock(), false);
                world.notifyNeighborsOfStateChange(p.offset(s.getValue(net.minecraft.block.BlockLever.FACING).getFacing().getOpposite()), s.getBlock(), false);
            }
        }
    }

    public Train trainOf(EntityMinecart cart) { return Train.of(cart); }
}
