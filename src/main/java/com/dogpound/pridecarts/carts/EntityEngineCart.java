package com.dogpound.pridecarts.carts;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
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
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;

/**
 * Mini engine: a little locomotive that pulls the train coupled to it (Cart Chain / Cart Rope).
 * Four throttle notches; pulling power is shared out over the train, so a long train starts slower.
 * <ul>
 *   <li>steam: burns furnace fuel (right-click with coal, logs, lava buckets...)</li>
 *   <li>electric: Forge Energy, charged by cables, Cart Loaders or a battery car</li>
 *   <li>solar (3 tiers): makes its own FE in daylight; the elite one keeps notch 3 going in full sun</li>
 * </ul>
 * Drive it: sit in it and hold W/S to notch the throttle up/down. Or sneak-right-click to notch it without
 * riding (it sets off the way you face). Notch 0 brakes.
 */
public class EntityEngineCart extends EntityMinecart implements IPrideCart {
    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(EntityEngineCart.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LINK0 = EntityDataManager.createKey(EntityEngineCart.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LINK1 = EntityDataManager.createKey(EntityEngineCart.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> ROPES = EntityDataManager.createKey(EntityEngineCart.class, DataSerializers.BYTE);
    /** throttle notch 0..4, +8 while it actually has power (for smoke/sparks on the client) */
    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityEngineCart.class, DataSerializers.BYTE);
    /** for the control panel: fuel ticks (steam) or FE, distance run in blocks, obey-smart-tracks flag */
    private static final DataParameter<Integer> FUEL = EntityDataManager.createKey(EntityEngineCart.class, DataSerializers.VARINT);
    private static final DataParameter<Float> ODO = EntityDataManager.createKey(EntityEngineCart.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> OBEY = EntityDataManager.createKey(EntityEngineCart.class, DataSerializers.BOOLEAN);
    /** the no-code route, e.g. "1:wait:10;2:reverse:0" (see {@link Routes}) */
    private static final DataParameter<String> ROUTE = EntityDataManager.createKey(EntityEngineCart.class, DataSerializers.STRING);

    /** control panel actions */
    public static final int A_NOTCH = 0, A_REVERSE = 1, A_HORN = 2, A_UNCOUPLE = 3, A_OBEY = 4, A_ESTOP = 5;

    public static final int NOTCHES = 4;
    /** target speed per notch, blocks/tick (vanilla rail cap is 0.4) */
    private static final double[] SPEED = {0, 0.1, 0.2, 0.3, 0.4};
    /** FE per tick per notch for electric/solar */
    private static final int FE_PER_NOTCH = 20;

    private final EnergyStorage energy = new EnergyStorage(400_000, 2_000, 2_000);
    private int capacity = 400_000;
    /** steam: ticks of fuel left in the firebox */
    private int burn;
    private int throttle;
    /** push direction (unit, horizontal); 0,0 = not set */
    private double pushX, pushZ;
    private int trainSize = 1, inputCooldown;
    private double odometer;

    public EntityEngineCart(World w) {
        super(w);
    }

    public EntityEngineCart(World w, double x, double y, double z, CartType type) {
        super(w, x, y, z);
        dataManager.set(TYPE, type.ordinal());
        capacity = type.size;
    }

    /** FE/tick a solar engine makes in full sun (0 = not solar). */
    public static int solarRate(CartType t) {
        switch (t) {
            case SOLAR_ENGINE: return 15;
            case SOLAR_ENGINE_ADVANCED: return 40;
            case SOLAR_ENGINE_ELITE: return 90;
            default: return 0;
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TYPE, CartType.STEAM_ENGINE.ordinal());
        dataManager.register(LINK0, 0);
        dataManager.register(LINK1, 0);
        dataManager.register(ROPES, (byte) 0);
        dataManager.register(STATE, (byte) 0);
        dataManager.register(FUEL, 0);
        dataManager.register(ODO, 0f);
        dataManager.register(OBEY, true);
        dataManager.register(ROUTE, "");
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

    private boolean steam() {
        return cartType() == CartType.STEAM_ENGINE;
    }

    public int throttle() {
        return dataManager.get(STATE) & 7;
    }

    public boolean powered() {
        return (dataManager.get(STATE) & 8) != 0;
    }

    @Override
    public Type getType() {
        return Type.RIDEABLE;
    }

    @Override
    public boolean canBeRidden() {
        return true;
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
    public void killMinecart(DamageSource source) {
        setDead();
        if (world.getGameRules().getBoolean("doEntityDrops")) entityDropItem(CartRegistry.named(this, cartType()), 0);
    }

    // ---------------------------------------------------------------- running

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            clientEffects();
            return;
        }
        if (ticksExisted % 20 == 3) {
            linksChanged();
            trainSize = Math.max(1, CartLinks.train(this).size());
        }
        driverInput();
        solar();
        boolean held = com.dogpound.pridecarts.CartData.hold(this) != null;   // waiting at a station: idle
        boolean power = throttle > 0 && !held && use();
        if (power) pull();
        else if (throttle == 0) brake();
        double moved = Math.sqrt((posX - prevPosX) * (posX - prevPosX) + (posZ - prevPosZ) * (posZ - prevPosZ));
        if (moved < 2) odometer += moved;                                      // < 2: ignore teleports
        if (ticksExisted % 10 == 0) {
            dataManager.set(FUEL, steam() ? burn : energy.getEnergyStored());
            dataManager.set(ODO, (float) odometer);
        }
        byte st = (byte) (throttle | (power ? 8 : 0));
        if (dataManager.get(STATE) != st) dataManager.set(STATE, st);
        if (power && steam() && ticksExisted % (24 - throttle * 4) == 0)
            world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.NEUTRAL, 0.15f, 1.6f);
    }

    /** The rider notches the throttle with W/S. */
    private void driverInput() {
        if (inputCooldown > 0) { inputCooldown--; return; }
        Entity r = getPassengers().isEmpty() ? null : getPassengers().get(0);   // minecarts have no "controlling" passenger
        if (!(r instanceof EntityPlayer)) return;
        EntityPlayer p = (EntityPlayer) r;
        float f = p.moveForward;
        if (Math.abs(f) < 0.1f) return;
        if (throttle == 0 && f > 0) aim(p);
        setThrottle(throttle + (f > 0 ? 1 : -1), p);
        inputCooldown = 8;
    }

    private void solar() {
        int rate = solarRate(cartType());
        if (rate <= 0 || !world.isDaytime() || ticksExisted % 5 != 0) return;
        BlockPos up = new BlockPos(posX, posY + 1, posZ);
        if (!world.canSeeSky(up)) return;
        int gain = rate * 5;
        if (world.isRaining()) gain /= 3;
        energy.receiveEnergy(Math.min(gain, capacity - energy.getEnergyStored()), false);
    }

    /** Spend one tick of power at the current notch. */
    private boolean use() {
        if (steam()) {
            if (burn <= 0) return false;
            burn -= 1 + (throttle >= 3 ? 1 : 0);
            return true;
        }
        int need = FE_PER_NOTCH * throttle;
        if (energy.getEnergyStored() < need) return false;
        energy.extractEnergy(need, false);
        return true;
    }

    private void pull() {
        double sp = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (sp > 0.04) {                                          // follow the track round curves: curves turn the motion a little
            double nx = motionX / sp, nz = motionZ / sp;           // each tick; bumps and coupling jolts don't count (they'd ratchet
            if (pushX == 0 && pushZ == 0) { pushX = nx; pushZ = nz; }                     // the engine round, 2026-10-04 test)
            else if (nx * pushX + nz * pushZ > 0.5) { pushX = pushX * 0.5 + nx * 0.5; pushZ = pushZ * 0.5 + nz * 0.5; double l = Math.sqrt(pushX * pushX + pushZ * pushZ); pushX /= l; pushZ /= l; }
        }
        if (pushX == 0 && pushZ == 0) return;
        double target = SPEED[throttle];
        double along = motionX * pushX + motionZ * pushZ;
        if (along >= target) return;
        // ponytail: power shared evenly over the train, steam pulls a bit harder; real tractive effort curve later
        double accel = (steam() ? 0.026 : 0.022) / (1 + 0.18 * (trainSize - 1));
        double add = Math.min(accel, target - along);
        motionX += pushX * add;
        motionZ += pushZ * add;
    }

    private void brake() {
        motionX *= 0.9;
        motionZ *= 0.9;
    }

    @Override
    public float getMaxCartSpeedOnRail() {
        return 0.4f;
    }

    private void clientEffects() {
        if (!powered()) return;
        int n = throttle();
        if (steam()) {
            if (rand.nextInt(5 - Math.min(4, n)) == 0)
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX, posY + 1.0, posZ, 0, 0.05, 0);
        } else if (rand.nextInt(12) == 0) {
            world.spawnParticle(EnumParticleTypes.REDSTONE, posX + (rand.nextDouble() - 0.5) * 0.6, posY + 0.9, posZ + (rand.nextDouble() - 0.5) * 0.6, 0.3, 0.6, 1.0);
        }
    }

    // ---------------------------------------------------------------- controls

    private void aim(EntityPlayer p) {
        EnumFacing f = p.getHorizontalFacing();
        pushX = f.getFrontOffsetX();
        pushZ = f.getFrontOffsetZ();
    }

    /** Set the throttle notch (smart tracks, signs). Starting from 0 keeps the last direction, or the way it faces. */
    public void notch(int t) {
        if (throttle == 0 && t > 0 && pushX == 0 && pushZ == 0) {
            float yaw = (rotationYaw + 90) * (float) Math.PI / 180f;            // a cart's yaw is 90° off its travel
            pushX = Math.round(-Math.sin(yaw)); pushZ = Math.round(Math.cos(yaw));
        }
        setThrottle(t, null);
    }

    // ---------------------------------------------------------------- control panel

    /** Synced for the panel: fuel ticks (steam) or FE stored. */
    public int fuelShown() { return dataManager.get(FUEL); }

    public int capacity() { return cartType().size; }

    /** Synced: blocks this engine has run. */
    public float odometer() { return dataManager.get(ODO); }

    public String route() { return dataManager.get(ROUTE); }

    public void setRoute(String r) { dataManager.set(ROUTE, Routes.join(Routes.parse(r))); }

    /** Does it listen to engine-control smart tracks? */
    public boolean obeysTracks() { return dataManager.get(OBEY); }

    /** A control-panel button (server side, from {@link com.dogpound.pridecarts.net.PcNet}). */
    public void panel(EntityPlayer p, int action, int value) {
        switch (action) {
            case A_NOTCH:
                if (throttle == 0 && value > 0 && pushX == 0 && pushZ == 0) aim(p);
                setThrottle(value, p);
                break;
            case A_REVERSE:
                reverse();
                for (EntityMinecart c : CartLinks.train(this)) { c.motionX = -c.motionX * 0.3; c.motionZ = -c.motionZ * 0.3; }
                status(p);
                break;
            case A_HORN:
                horn();
                break;
            case A_UNCOUPLE:
                for (CartLinks.Kind k : CartLinks.uncoupleAll(this)) entityDropItem(CartRegistry.coupler(k), 0.5f);
                trainSize = 1;
                break;
            case A_OBEY:
                dataManager.set(OBEY, !obeysTracks());
                break;
            case A_ESTOP:
                setThrottle(0, p);
                for (EntityMinecart c : CartLinks.train(this)) { c.motionX = 0; c.motionZ = 0; }
                world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.NEUTRAL, 0.8f, 0.6f);
                break;
            default:
        }
    }

    /** Steam whistle or electric horn. */
    public void horn() {
        if (steam()) world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_NOTE_FLUTE, SoundCategory.NEUTRAL, 2.5f, 1.4f);
        else world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_NOTE_BASS, SoundCategory.NEUTRAL, 3f, 0.7f);
    }

    /** Turn the engine round (reverse track, signs). */
    public void reverse() {
        pushX = -pushX;
        pushZ = -pushZ;
    }

    public boolean isSteam() {
        return steam();
    }

    /** Steam: put fuel in the firebox if it fits. */
    public boolean addFuel(int ticks) {
        if (!steam() || burn + ticks > capacity) return false;
        burn += ticks;
        return true;
    }

    private void setThrottle(int t, EntityPlayer p) {
        throttle = MathHelper.clamp(t, 0, NOTCHES);
        if (p != null) status(p);
    }

    private void status(EntityPlayer p) {
        StringBuilder bar = new StringBuilder();
        for (int i = 1; i <= NOTCHES; i++) bar.append(i <= throttle ? "§d■" : "§8□");
        String fuel = steam()
                ? (burn > 0 ? String.format("§6🔥 %ds of fuel", burn / 20) : "§cno fuel: right-click with coal")
                : String.format("§b⚡ %,d / %,d FE", energy.getEnergyStored(), capacity);
        p.sendStatusMessage(new TextComponentString("§f" + name() + " " + bar + " §7· " + fuel
                + (trainSize > 1 ? " §7· pulling " + (trainSize - 1) : "")), true);
    }

    private String name() {
        String id = cartType().id.replace('_', ' ');
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (world.isRemote) {
            if (player.isSneaking() && player.getHeldItem(hand).isEmpty()) com.dogpound.pridecarts.client.GuiEngine.open(this);
            return true;
        }
        ItemStack held = player.getHeldItem(hand);
        if (steam() && !held.isEmpty()) {
            int t = TileEntityFurnace.getItemBurnTime(held);
            if (t > 0) {
                if (burn + t > capacity) { status(player); return true; }
                burn += t;
                if (!player.capabilities.isCreativeMode) {
                    ItemStack left = held.getItem().getContainerItem(held);
                    held.shrink(1);
                    if (!left.isEmpty() && !player.inventory.addItemStackToInventory(left)) player.dropItem(left, false);
                }
                status(player);
                return true;
            }
        }
        if (player.isSneaking() && held.isEmpty()) return true;              // the client opens the control panel
        if (!isBeingRidden() && held.isEmpty()) {
            player.startRiding(this);
            status(player);
            return true;
        }
        status(player);
        return true;
    }

    // ---------------------------------------------------------------- energy + save

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing) {
        return (cap == CapabilityEnergy.ENERGY && !steam()) || super.hasCapability(cap, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> cap, EnumFacing facing) {
        return cap == CapabilityEnergy.ENERGY && !steam() ? (T) energy : super.getCapability(cap, facing);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setString("pcType", cartType().id);
        nbt.setInteger("fe", energy.getEnergyStored());
        nbt.setInteger("burn", burn);
        nbt.setByte("throttle", (byte) throttle);
        nbt.setDouble("pushX", pushX);
        nbt.setDouble("pushZ", pushZ);
        nbt.setDouble("odometer", odometer);
        nbt.setBoolean("obey", obeysTracks());
        nbt.setString("route", route());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        CartType t = CartRegistry.typeById(nbt.getString("pcType"));
        dataManager.set(TYPE, t.ordinal());
        capacity = t.size;
        burn = nbt.getInteger("burn");
        throttle = MathHelper.clamp(nbt.getByte("throttle"), 0, NOTCHES);
        pushX = nbt.getDouble("pushX");
        pushZ = nbt.getDouble("pushZ");
        odometer = nbt.getDouble("odometer");
        if (nbt.hasKey("obey")) dataManager.set(OBEY, nbt.getBoolean("obey"));
        setRoute(nbt.getString("route"));
        int fe = Math.min(nbt.getInteger("fe"), capacity);
        while (fe > 0) { int r = energy.receiveEnergy(fe, false); if (r <= 0) break; fe -= r; }
    }
}
