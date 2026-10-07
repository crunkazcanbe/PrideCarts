package com.dogpound.pridecarts.carts;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;

/**
 * Mini power car (list §1: battery car, mobile power generator). Real Forge Energy storage, so
 * cables, machines and the Cart Loader/Unloader can charge or drain it. The generator burns any
 * furnace fuel you give it (right-click) into FE.
 */
public class EntityPowerCart extends EntityMinecart implements IPrideCart {
    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(EntityPowerCart.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LINK0 = EntityDataManager.createKey(EntityPowerCart.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LINK1 = EntityDataManager.createKey(EntityPowerCart.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> ROPES = EntityDataManager.createKey(EntityPowerCart.class, DataSerializers.BYTE);
    /** FE made per tick while the generator burns. */
    private static final int GEN_RATE = 60;
    private final EnergyStorage energy = new EnergyStorage(2_000_000, 50_000, 50_000);
    private int capacity = 2_000_000;
    private int burn;

    public EntityPowerCart(World w) {
        super(w);
    }

    public EntityPowerCart(World w, double x, double y, double z, CartType type) {
        super(w, x, y, z);
        dataManager.set(TYPE, type.ordinal());
        capacity = type.size;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TYPE, CartType.BATTERY.ordinal());
        dataManager.register(LINK0, 0);
        dataManager.register(LINK1, 0);
        dataManager.register(ROPES, (byte) 0);
    }

    @Override
    public int linkedId(int slot) {
        return dataManager.get(slot == 0 ? LINK0 : LINK1);
    }

    @Override
    public boolean linkedRope(int slot) {
        return (dataManager.get(ROPES) & (1 << slot)) != 0;
    }

    @Override
    public void linksChanged() {
        int[] ids = CartLinks.resolve(this);
        dataManager.set(LINK0, ids[0]);
        dataManager.set(LINK1, ids[1]);
        dataManager.set(ROPES, (byte) ids[2]);
    }

    @Override
    public CartType cartType() {
        return CartType.byOrdinal(dataManager.get(TYPE));
    }

    public EnergyStorage energy() {
        return energy;
    }

    @Override
    public Type getType() {
        return cartType() == CartType.GENERATOR ? Type.FURNACE : Type.RIDEABLE;
    }

    @Override
    public boolean canBeRidden() {
        return false;
    }

    @Override
    public IBlockState getDefaultDisplayTile() {
        return cartType().display();
    }

    @Override
    public int getDefaultDisplayTileOffset() {
        return 8;
    }

    @Override
    public ItemStack getCartItem() {
        return CartRegistry.stack(cartType());
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote && ticksExisted % 20 == 3) linksChanged();
        if (world.isRemote) {
            if (cartType() == CartType.GENERATOR && burn > 0 && rand.nextInt(4) == 0)
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX, posY + 0.8, posZ, 0, 0, 0);
            return;
        }
        if (burn > 0) {
            burn--;
            if (energy.getEnergyStored() < capacity) energy.receiveEnergy(GEN_RATE, false);
        }
        // a power car coupled into a train keeps its electric / solar engines charged (up to 4,000 FE a second)
        if (ticksExisted % 10 == 5 && energy.getEnergyStored() > 0) {
            for (EntityMinecart c : CartLinks.train(this)) {
                if (!(c instanceof EntityEngineCart) || ((EntityEngineCart) c).isSteam()) continue;
                net.minecraftforge.energy.IEnergyStorage dst = c.getCapability(CapabilityEnergy.ENERGY, null);
                if (dst == null) continue;
                int give = dst.receiveEnergy(Math.min(2000, energy.getEnergyStored()), true);
                if (give > 0) dst.receiveEnergy(energy.extractEnergy(give, false), false);
            }
        }
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (world.isRemote) return true;
        ItemStack held = player.getHeldItem(hand);
        if (cartType() == CartType.GENERATOR && !held.isEmpty()) {
            int t = TileEntityFurnace.getItemBurnTime(held);
            if (t > 0) {
                burn += t;
                if (!player.capabilities.isCreativeMode) held.shrink(1);
            }
        }
        player.sendStatusMessage(new TextComponentString(String.format("§d%s§7 · %,d / %,d FE%s",
                cartType().id.replace('_', ' '), Math.min(energy.getEnergyStored(), capacity), capacity,
                burn > 0 ? String.format(" · burning %ds", burn / 20) : "")), true);
        return true;
    }

    @Override
    public void killMinecart(DamageSource source) {
        setDead();
        if (world.getGameRules().getBoolean("doEntityDrops")) entityDropItem(CartRegistry.named(this, cartType()), 0);
    }

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
        return cap == CapabilityEnergy.ENERGY || super.hasCapability(cap, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
        return cap == CapabilityEnergy.ENERGY ? (T) energy : super.getCapability(cap, facing);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setString("pcType", cartType().id);
        nbt.setInteger("fe", energy.getEnergyStored());
        nbt.setInteger("burn", burn);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        CartType t = CartRegistry.typeById(nbt.getString("pcType"));
        dataManager.set(TYPE, t.ordinal());
        capacity = t.size;
        burn = nbt.getInteger("burn");
        int fe = Math.min(nbt.getInteger("fe"), capacity);
        while (fe > 0) { int r = energy.receiveEnergy(fe, false); if (r <= 0) break; fe -= r; }
    }
}
