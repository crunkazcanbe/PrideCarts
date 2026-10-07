package com.dogpound.pridecarts.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Base for PrideCarts' rail-side blocks (Cart Loader, Cart Unloader, Railway Observer). Ported from Autowork by Ict00
 * (MIT licence, https://github.com/Ict00/autowork) to Forge 1.12.2, with PrideCarts' own behaviour on top.
 * The block's FRONT faces the rail; a wall sign on its side or back with a cart's name limits it to that cart.
 */
public abstract class CartBlock extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    private final String[] tips;

    protected CartBlock(String name, String... tips) {
        super(Material.IRON);
        this.tips = tips;
        setRegistryName("pridecarts", name);
        setUnlocalizedName("pridecarts." + name);
        setHardness(2.5F);
        setCreativeTab(CreativeTabs.TRANSPORTATION);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(POWERED, false));
    }

    // ---------------------------------------------------------------- state
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, POWERED); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(FACING).getHorizontalIndex() | (s.getValue(POWERED) ? 4 : 0); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(m & 3)).withProperty(POWERED, (m & 4) != 0); }

    @Override
    public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing side, float hx, float hy, float hz, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());   // front looks back at you: stand on the rail to place it
    }

    // ---------------------------------------------------------------- ticking
    @Override public void onBlockAdded(World w, BlockPos pos, IBlockState s) { if (!w.isRemote) w.scheduleUpdate(pos, this, 4); }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        if (w.isRemote) return;
        boolean done = work(w, pos, s);
        if (done != s.getValue(POWERED)) {
            w.setBlockState(pos, s.withProperty(POWERED, done), 3);
            w.notifyNeighborsOfStateChange(pos.down(), this, false);
        }
        w.scheduleUpdate(pos, this, 4);
    }

    /** do the job once; return true when the redstone output should be on */
    protected abstract boolean work(World w, BlockPos pos, IBlockState s);

    // ---------------------------------------------------------------- redstone out (all sides; strong downwards like Autowork)
    @Override public boolean canProvidePower(IBlockState s) { return true; }
    @Override public int getWeakPower(IBlockState s, IBlockAccess w, BlockPos pos, EnumFacing side) { return s.getValue(POWERED) ? 15 : 0; }
    @Override public int getStrongPower(IBlockState s, IBlockAccess w, BlockPos pos, EnumFacing side) { return s.getValue(POWERED) && side == EnumFacing.UP ? 15 : 0; }

    // ---------------------------------------------------------------- helpers
    protected static BlockPos front(BlockPos pos, IBlockState s) { return pos.offset(s.getValue(FACING)); }
    protected static BlockPos back(BlockPos pos, IBlockState s) { return pos.offset(s.getValue(FACING).getOpposite()); }

    /** carts on the rail in front, limited to the name on a wall sign attached to the block's side/back (if any) */
    protected static List<EntityMinecart> carts(World w, BlockPos pos, IBlockState s) {
        BlockPos f = front(pos, s);
        String filter = signFilter(w, pos, s.getValue(FACING));
        List<EntityMinecart> out = new ArrayList<EntityMinecart>();
        for (EntityMinecart c : w.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(f).grow(0.2)))
            if (filter == null || (c.hasCustomName() && c.getCustomNameTag().equalsIgnoreCase(filter))) out.add(c);
        return out;
    }

    static String signFilter(World w, BlockPos pos, EnumFacing facing) {
        for (EnumFacing d : new EnumFacing[]{ facing.rotateY(), facing.rotateYCCW(), facing.getOpposite() }) {
            BlockPos sp = pos.offset(d);
            IBlockState st = w.getBlockState(sp);
            if (!(st.getBlock() instanceof BlockWallSign) || st.getValue(BlockWallSign.FACING) != d) continue;
            TileEntity te = w.getTileEntity(sp);
            if (te instanceof TileEntitySign)
                for (net.minecraft.util.text.ITextComponent line : ((TileEntitySign) te).signText) {
                    String t = line == null ? "" : line.getUnformattedText().trim();
                    if (!t.isEmpty()) return t;
                }
        }
        return null;
    }

    @Override
    public void addInformation(ItemStack stack, World w, List<String> tip, ITooltipFlag flag) {
        for (String t : tips) tip.add("§7" + t);
        tip.add("§8Ported from Autowork by Ict00 (MIT)");
    }
}
