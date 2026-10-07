package com.dogpound.pridecarts.blocks;

import com.dogpound.pridecarts.carts.CartRegistry;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

/** Crossing (diamond / X): two lines cross here; every cart carries straight on the way it came in. */
public class CrossingTrack extends BlockRailBase {
    public static final PropertyEnum<EnumRailDirection> SHAPE = PropertyEnum.create("shape", EnumRailDirection.class, d -> d == EnumRailDirection.NORTH_SOUTH);

    public CrossingTrack() {
        super(true);
        setRegistryName("pridecarts", "rail_crossing");
        setUnlocalizedName("pridecarts.rail_crossing");
        setHardness(0.7f);
        setSoundType(SoundType.METAL);
        setCreativeTab(CartRegistry.TAB);
        setDefaultState(blockState.getBaseState().withProperty(SHAPE, EnumRailDirection.NORTH_SOUTH));
    }

    @Override public IProperty<EnumRailDirection> getShapeProperty() { return SHAPE; }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, SHAPE); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState(); }
    @Override public int getMetaFromState(IBlockState s) { return 0; }
    @Override protected void updateState(IBlockState s, World w, BlockPos p, net.minecraft.block.Block b) {}
    @Override protected IBlockState updateDir(World w, BlockPos p, IBlockState s, boolean placing) { return s; }
    @Override public boolean isFlexibleRail(IBlockAccess w, BlockPos p) { return false; }

    @Override
    public EnumRailDirection getRailDirection(IBlockAccess w, BlockPos pos, IBlockState state, EntityMinecart cart) {
        if (cart == null) return EnumRailDirection.NORTH_SOUTH;
        return Math.abs(cart.motionX) > Math.abs(cart.motionZ) ? EnumRailDirection.EAST_WEST : EnumRailDirection.NORTH_SOUTH;
    }

    @Override
    public void addInformation(ItemStack s, World w, List<String> tip, ITooltipFlag f) { tip.add("§7Two lines cross here: carts go straight on the way they came."); }
}
