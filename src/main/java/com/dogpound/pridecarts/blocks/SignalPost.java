package com.dogpound.pridecarts.blocks;

import com.dogpound.pridecarts.Signals;
import com.dogpound.pridecarts.carts.CartRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/**
 * A Pride signal: place it beside a Signal Track, facing the trains that come towards it. Green = the section ahead
 * is free, red = a train is in it (or the signal track is powered). Gives a redstone signal while red.
 */
public class SignalPost extends Block {
    public static final PropertyBool RED = PropertyBool.create("red");

    public SignalPost() {
        super(Material.IRON);
        setRegistryName("pridecarts", "signal_post");
        setUnlocalizedName("pridecarts.signal_post");
        setHardness(1.5f);
        setSoundType(SoundType.METAL);
        setCreativeTab(CartRegistry.TAB);
        setDefaultState(blockState.getBaseState().withProperty(BlockHorizontal.FACING, EnumFacing.NORTH).withProperty(RED, false));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, BlockHorizontal.FACING, RED); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.getHorizontal(m & 3)).withProperty(RED, (m & 4) != 0); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(BlockHorizontal.FACING).getHorizontalIndex() | (s.getValue(RED) ? 4 : 0); }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return new AxisAlignedBB(0.3, 0, 0.3, 0.7, 1, 0.7); }
    @Override public int getLightValue(IBlockState s, IBlockAccess w, BlockPos p) { return 9; }
    @Override public boolean canProvidePower(IBlockState s) { return true; }
    /** redstone only out of its back (never into the signal track beside it, or it would hold itself red) */
    @Override public int getWeakPower(IBlockState s, IBlockAccess w, BlockPos p, EnumFacing f) { return s.getValue(RED) && f == s.getValue(BlockHorizontal.FACING) ? 15 : 0; }

    @Override
    public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing facing, float hx, float hy, float hz, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(BlockHorizontal.FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override public void onBlockAdded(World w, BlockPos pos, IBlockState s) { if (!w.isRemote) w.scheduleUpdate(pos, this, 5); }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random rnd) {
        BlockPos track = findTrack(w, pos);
        boolean red = track == null || !Signals.postClear(w, track, s.getValue(BlockHorizontal.FACING));
        if (red != s.getValue(RED)) {
            w.setBlockState(pos, s.withProperty(RED, red), 3);
            w.notifyNeighborsOfStateChange(pos, this, false);
        }
        w.scheduleUpdate(pos, this, 10);
    }

    /** the Signal Track next to (or below the side of) this post */
    static BlockPos findTrack(World w, BlockPos pos) {
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) for (int dy = -1; dy <= 0; dy++) {
            BlockPos p = pos.offset(f).up(dy);
            if (Signals.isSignal(w, p)) return p;
        }
        return null;
    }

    @Override
    public void addInformation(ItemStack st, World w, List<String> tip, ITooltipFlag f) {
        tip.add("§7Place beside a Signal Track, facing the trains coming towards it.");
        tip.add("§aGreen§7: the track ahead is free · §cRed§7: a train is in it (redstone comes out of its back)");
    }
}
