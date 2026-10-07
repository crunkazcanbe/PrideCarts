package com.dogpound.pridecarts.blocks;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import com.dogpound.pridecarts.carts.CartLinks;
import com.dogpound.pridecarts.carts.CartRegistry;
import com.dogpound.pridecarts.carts.EntityEngineCart;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Smart tracks: rails that do one job with no programming and no signs. Straight pieces (flat or sloped),
 * like powered rail. Redstone switches most of them off (or, for the holding track, lets the train go).
 * Work with any minecart: vanilla, PrideCarts, Railcraft.
 */
public class SmartRail extends BlockRailBase {
    public static final PropertyEnum<EnumRailDirection> SHAPE = PropertyEnum.create("shape", EnumRailDirection.class,
            d -> d != EnumRailDirection.NORTH_EAST && d != EnumRailDirection.NORTH_WEST
                    && d != EnumRailDirection.SOUTH_EAST && d != EnumRailDirection.SOUTH_WEST);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public enum Kind {
        BOOSTER("Booster Track", "Speeds carts up, no redstone needed. Powered: off."),
        LAUNCHER("Launcher Track", "Fires carts off at top speed. Powered: off."),
        BRAKE("Brake Track", "Slows carts to a crawl. Powered: off."),
        SLOW_ZONE("Slow Zone Track", "Speed limit: slow (about 2 m/s). Powered: off."),
        MEDIUM_ZONE("Medium Zone Track", "Speed limit: medium (about 4 m/s). Powered: off."),
        STATION("Station Track", "Stops the train for 5 seconds, then it carries on. Powered: trains pass through."),
        HOLDING("Holding Track", "Holds the train until it gets a redstone signal."),
        REVERSE("Reverse Track", "Sends the train back the way it came. Powered: off."),
        EJECT("Eject Track", "Everyone gets out here. Powered: off."),
        DECOUPLER("Decoupler Track", "Uncouples each cart that rolls over it (drops the chain). Powered: off."),
        ENGINE_START("Engine Start Track", "Mini engines set off at notch 2."),
        ENGINE_FASTER("Engine Faster Track", "Mini engines notch up one."),
        ENGINE_SLOWER("Engine Slower Track", "Mini engines notch down one."),
        ENGINE_FULL("Engine Full Track", "Mini engines go full power."),
        ENGINE_STOP("Engine Stop Track", "Mini engines shut off and brake."),
        REFUEL("Refuel Track", "Steam engines take fuel from chests beside it; electric/solar engines charge from FE blocks beside it."),
        RAINBOW("Rainbow Track", "Pride track: a little boost and a trail of colour."),
        BOOSTER_2("Booster Track II", "A stronger booster: gets carts to top speed fast. Powered: off."),
        BOOSTER_3("Booster Track III", "The strongest booster: top speed at once, even uphill. Powered: off."),
        HIGH_SPEED("High-Speed Track", "Lets carts go twice as fast as normal track (keep it straight!)."),
        ONE_WAY("One-Way Track", "Carts may only go east / south over it; others are turned back. Powered: west / north only."),
        COUPLER("Auto-Coupler Track", "Couples a cart to the cart right behind or ahead of it with a chain (needs no item). Powered: off."),
        DELAY("Delay Track", "Stops the train for 2 seconds. Powered: trains pass through."),
        WHISTLE("Whistle Track", "Mini engines sound their whistle / horn as they pass."),
        LAMP("Lamp Track", "A glowing track: lights up tunnels and stations."),
        ANNOUNCER("Announcer Track", "Tells riders the next station number ahead."),
        HEALING("Healing Track", "Heals and feeds riders as they roll over it."),
        FIREWORKS("Pride Fireworks Track", "Rainbow fireworks when a train passes. Powered: off."),
        EMBARK("Embark Track", "Players and animals within 2 blocks climb into empty seat cars as they pass. Powered: off."),
        PRIMING("Priming Track", "Lights TNT minecarts and Explosives Cars that roll over it. Powered: off."),
        BUFFER_STOP("Buffer Stop", "The end of the line: carts stop dead here."),
        PICKUP("Pickup Track", "Storage cars take items from chests beside it as they roll past (no stopping)."),
        DROPOFF("Drop-off Track", "Storage cars give their items to chests beside it as they roll past."),
        FLUID_FILL("Fluid Fill Track", "Tank cars fill up from tanks beside it as they roll past."),
        FLUID_DRAIN("Fluid Drain Track", "Tank cars empty into tanks beside it as they roll past."),
        VACUUM("Item Vacuum Track", "Storage cars hoover up items lying within 3 blocks."),
        ICE("Ice Track", "Almost no friction: carts glide on and on."),
        MUD("Mud Track", "Very sticky: carts slow right down."),
        TELEPORT("Teleport Track", "Carts jump to the other Teleport Track with the same number. Right-click to set the number."),
        MUSIC("Music Track", "Plays a note when a cart passes; right-click to change the note."),
        SIGNAL("Signal Track", "Block signal: a train waits here while another train is on the track ahead (up to the next Signal Track). Powered: always stop.");

        public final String title, tip;
        Kind(String title, String tip) { this.title = title; this.tip = tip; }
        public String id() { return "rail_" + name().toLowerCase(); }
    }

    public final Kind kind;

    public SmartRail(Kind kind) {
        super(true);
        this.kind = kind;
        setRegistryName("pridecarts", kind.id());
        setUnlocalizedName("pridecarts." + kind.id());
        setHardness(0.7f);
        setSoundType(SoundType.METAL);
        setCreativeTab(CartRegistry.TAB);
        setDefaultState(blockState.getBaseState().withProperty(SHAPE, EnumRailDirection.NORTH_SOUTH).withProperty(POWERED, false));
    }

    // ---------------------------------------------------------------- block plumbing

    @Override public IProperty<EnumRailDirection> getShapeProperty() { return SHAPE; }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, SHAPE, POWERED); }

    @Override public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(SHAPE, EnumRailDirection.byMetadata(meta & 7)).withProperty(POWERED, (meta & 8) != 0);
    }

    @Override public int getMetaFromState(IBlockState s) { return s.getValue(SHAPE).getMetadata() | (s.getValue(POWERED) ? 8 : 0); }

    @Override
    protected void updateState(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block neighbour) {
        boolean p = world.isBlockPowered(pos);
        if (p != state.getValue(POWERED)) world.setBlockState(pos, state.withProperty(POWERED, p), 3);
    }

    @Override public boolean isFlexibleRail(IBlockAccess world, BlockPos pos) { return false; }

    @Override public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) { return kind == Kind.LAMP ? 15 : kind == Kind.RAINBOW || kind == Kind.FIREWORKS ? 7 : 0; }

    @Override
    public float getRailMaxSpeed(World world, EntityMinecart cart, BlockPos pos) {
        boolean on = !world.getBlockState(pos).getValue(POWERED);
        if (on && kind == Kind.SLOW_ZONE) return 0.1f;
        if (on && kind == Kind.MEDIUM_ZONE) return 0.2f;
        if (kind == Kind.HIGH_SPEED || kind == Kind.BOOSTER_3) return 0.8f;
        return super.getRailMaxSpeed(world, cart, pos);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tip, ITooltipFlag flag) {
        tip.add("§7" + kind.tip);
        if (kind == Kind.STATION) tip.add("§8Right-click with an empty hand to number it (sneak: count down) for engine routes");
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState state, net.minecraft.entity.player.EntityPlayer p, net.minecraft.util.EnumHand hand, EnumFacing face, float hx, float hy, float hz) {
        if ((kind != Kind.STATION && kind != Kind.TELEPORT && kind != Kind.MUSIC) || !p.getHeldItem(hand).isEmpty()) return false;
        if (!w.isRemote) {
            int n = com.dogpound.pridecarts.carts.Routes.cycle(w, pos, p.isSneaking() ? -1 : 1);
            String what = kind == Kind.TELEPORT ? "§5✦ Teleport channel " : kind == Kind.MUSIC ? "§b♪ Note " : "§d🚉 Station ";
            p.sendStatusMessage(new net.minecraft.util.text.TextComponentString(n == 0 ? "§7No number" : what + n), true);
            w.playSound(null, pos, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 0.6f, 0.8f + n * 0.05f);
        }
        return true;
    }

    @Override
    public void breakBlock(World w, BlockPos pos, IBlockState state) {
        if ((kind == Kind.STATION || kind == Kind.TELEPORT || kind == Kind.MUSIC) && !w.isRemote) com.dogpound.pridecarts.carts.Routes.forget(w, pos);
        super.breakBlock(w, pos, state);
    }

    // ---------------------------------------------------------------- what the track does

    @Override
    public void onMinecartPass(World world, EntityMinecart cart, BlockPos pos) {
        if (world.isRemote) {
            if (kind == Kind.RAINBOW && cart.ticksExisted % 2 == 0) rainbow(world, cart);
            return;
        }
        boolean powered = world.getBlockState(pos).getValue(POWERED);
        boolean entered = entered(cart, pos);
        double sp = Math.sqrt(cart.motionX * cart.motionX + cart.motionZ * cart.motionZ);
        switch (kind) {
            case BOOSTER:
                if (!powered) push(cart, sp, Math.min(0.4, sp + 0.06));
                break;
            case RAINBOW:
                push(cart, sp, Math.min(0.3, sp + 0.03));
                break;
            case LAUNCHER:
                if (!powered && entered) {
                    Train.of(cart).launch(null, cart.getMaxCartSpeedOnRail());
                    world.playSound(null, pos, SoundEvents.ENTITY_FIREWORK_LAUNCH, SoundCategory.BLOCKS, 0.6f, 1.2f);
                }
                break;
            case BRAKE:
                if (!powered && sp > 0.04) { cart.motionX *= 0.75; cart.motionZ *= 0.75; }
                break;
            case STATION:
                if (entered) com.dogpound.pridecarts.CartEvents.dbg("station track {}: cart {} entered, powered={}, held={}", pos, cart.getEntityId(), powered, CartData.hold(cart) != null);
                if (!powered && entered && CartData.hold(cart) == null) {
                    int[] r = com.dogpound.pridecarts.carts.Routes.arrive(world, pos, cart);   // route steps for this station number
                    if (r[0] != 0) hold(world, cart, pos, r[0] < 0 ? -1 : world.getTotalWorldTime() + r[0], r[1] == 1);
                }
                break;
            case HOLDING:
                if (!powered && entered && CartData.hold(cart) == null) hold(world, cart, pos, -1, false);
                break;
            case REVERSE:
                if (!powered && entered) for (EntityMinecart c : Train.of(cart).carts) {
                    c.motionX = -c.motionX; c.motionZ = -c.motionZ;
                    if (c instanceof EntityEngineCart) ((EntityEngineCart) c).reverse();
                }
                break;
            case EJECT:
                if (!powered) for (Entity p : new ArrayList<>(cart.getPassengers())) p.dismountRidingEntity();
                break;
            case DECOUPLER:
                if (!powered && entered) for (CartLinks.Kind k : CartLinks.uncoupleAll(cart)) cart.entityDropItem(CartRegistry.coupler(k), 0.5f);
                break;
            case ENGINE_START: if (entered) engines(cart, e -> e.notch(Math.max(2, e.throttle()))); break;
            case ENGINE_FASTER: if (entered) engines(cart, e -> e.notch(e.throttle() + 1)); break;
            case ENGINE_SLOWER: if (entered) engines(cart, e -> e.notch(e.throttle() - 1)); break;
            case ENGINE_FULL: if (entered) engines(cart, e -> e.notch(EntityEngineCart.NOTCHES)); break;
            case ENGINE_STOP: if (entered) engines(cart, e -> e.notch(0)); break;
            case BOOSTER_2:
                if (!powered) push(cart, sp, Math.min(cart.getMaxCartSpeedOnRail(), sp + 0.12));
                break;
            case BOOSTER_3:
                if (!powered) push(cart, sp, cart.getMaxCartSpeedOnRail());
                break;
            case ONE_WAY:
                if (entered && sp > 0.01) {
                    EnumRailDirection sh = world.getBlockState(pos).getValue(SHAPE);
                    boolean ew = sh == EnumRailDirection.EAST_WEST || sh == EnumRailDirection.ASCENDING_EAST || sh == EnumRailDirection.ASCENDING_WEST;
                    double along = ew ? cart.motionX : cart.motionZ;
                    boolean wrong = powered ? along > 0 : along < 0;
                    if (wrong) for (EntityMinecart c : Train.of(cart).carts) {
                        c.motionX = -c.motionX; c.motionZ = -c.motionZ;
                        if (c instanceof EntityEngineCart) ((EntityEngineCart) c).reverse();
                    }
                }
                break;
            case COUPLER:
                if (!powered && entered) autoCouple(world, cart);
                break;
            case EMBARK:
                if (!powered && cart instanceof net.minecraft.entity.item.EntityMinecartEmpty && cart.getPassengers().isEmpty() && cart.ticksExisted % 5 == 0)
                    for (net.minecraft.entity.EntityLivingBase e : world.getEntitiesWithinAABB(net.minecraft.entity.EntityLivingBase.class, cart.getEntityBoundingBox().grow(2))) {
                        if (e.isRiding() || e.isSneaking()) continue;
                        e.startRiding(cart);
                        break;
                    }
                break;
            case PRIMING:
                if (!powered && entered) {
                    if (cart instanceof net.minecraft.entity.item.EntityMinecartTNT) ((net.minecraft.entity.item.EntityMinecartTNT) cart).ignite();
                    else if (cart instanceof com.dogpound.pridecarts.carts.EntityStorageCart && ((com.dogpound.pridecarts.carts.IPrideCart) cart).cartType() == com.dogpound.pridecarts.carts.CartType.EXPLOSIVES)
                        cart.setFire(5);
                }
                break;
            case BUFFER_STOP:
                for (EntityMinecart c : Train.of(cart).carts) {
                    c.motionX = 0; c.motionZ = 0;
                    if (c instanceof EntityEngineCart) ((EntityEngineCart) c).notch(0);      // engines shut off at the end of the line
                }
                cart.setPosition(pos.getX() + 0.5, cart.posY, pos.getZ() + 0.5);         // and nothing rolls past the buffers
                break;
            case PICKUP: case DROPOFF:
                if (cart.ticksExisted % 4 == 0 && cart instanceof com.dogpound.pridecarts.carts.EntityStorageCart)
                    com.dogpound.pridecarts.carts.Routes.transfer(world, pos, cart, kind == Kind.DROPOFF);
                break;
            case FLUID_FILL: case FLUID_DRAIN:
                if (cart.ticksExisted % 4 == 0 && cart instanceof com.dogpound.pridecarts.carts.EntityTankCart) fluids(world, pos, (com.dogpound.pridecarts.carts.EntityTankCart) cart, kind == Kind.FLUID_FILL);
                break;
            case VACUUM:
                if (cart.ticksExisted % 5 == 0 && cart instanceof com.dogpound.pridecarts.carts.EntityStorageCart)
                    for (net.minecraft.entity.item.EntityItem it : world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, cart.getEntityBoundingBox().grow(3))) {
                        if (it.isDead) continue;
                        ItemStack left = com.dogpound.pridecarts.carts.CartInv.insert((com.dogpound.pridecarts.carts.EntityStorageCart) cart, it.getItem().copy());
                        if (left.isEmpty()) it.setDead(); else it.setItem(left);
                    }
                break;
            case ICE:
                if (sp > 0.01) push(cart, sp, Math.min(cart.getMaxCartSpeedOnRail(), sp * 1.02));   // undoes rail friction
                break;
            case MUD:
                if (sp > 0.03) { cart.motionX *= 0.85; cart.motionZ *= 0.85; }
                break;
            case TELEPORT:
                if (entered) com.dogpound.pridecarts.CartEvents.dbg("teleport track {} entered: channel {}, speed {}, last jump {}", pos, com.dogpound.pridecarts.carts.Routes.station(world, pos), sp, cart.getEntityData().getLong("pcTpT"));
                if (entered && sp > 0.01 && cart.getEntityData().getLong("pcTpT") < world.getTotalWorldTime() - 20) teleport(world, pos, cart, sp);
                break;
            case MUSIC:
                if (entered) {
                    int note = Math.floorMod(com.dogpound.pridecarts.carts.Routes.station(world, pos), 25);
                    world.playSound(null, pos, SoundEvents.BLOCK_NOTE_HARP, SoundCategory.RECORDS, 2f, (float) Math.pow(2, (note - 12) / 12.0));
                }
                break;
            case SIGNAL:
                if (entered && CartData.hold(cart) == null && sp > 0.01) {
                    EnumFacing ahead = EnumFacing.getFacingFromVector((float) cart.motionX, 0, (float) cart.motionZ);
                    List<EntityMinecart> own = Train.of(cart).carts;
                    if (powered || !com.dogpound.pridecarts.Signals.clear(world, pos, ahead, own)) {
                        hold(world, cart, pos, -1, false);
                        for (EntityMinecart c : own) { net.minecraft.nbt.NBTTagCompound h = CartData.hold(c); if (h != null) h.setInteger("signalDir", ahead.getIndex()); }
                        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BASS, SoundCategory.BLOCKS, 0.7f, 0.5f);
                    }
                }
                break;
            case DELAY:
                if (!powered && entered && CartData.hold(cart) == null) hold(world, cart, pos, world.getTotalWorldTime() + 40, false);
                break;
            case WHISTLE:
                if (entered) for (EntityMinecart c : Train.of(cart).carts) if (c instanceof EntityEngineCart) { ((EntityEngineCart) c).horn(); break; }
                break;
            case ANNOUNCER:
                if (entered && sp > 0.01) announce(world, cart, pos);
                break;
            case HEALING:
                if (cart.ticksExisted % 10 == 0) for (Entity p : cart.getPassengers()) if (p instanceof net.minecraft.entity.EntityLivingBase) {
                    ((net.minecraft.entity.EntityLivingBase) p).heal(2f);
                    if (p instanceof net.minecraft.entity.player.EntityPlayer) ((net.minecraft.entity.player.EntityPlayer) p).getFoodStats().addStats(1, 0.5f);
                }
                break;
            case FIREWORKS:
                if (!powered && entered) fireworks(world, cart);
                break;
            case REFUEL:
                if (cart instanceof EntityEngineCart && (entered || cart.ticksExisted % 10 == 0)) refuel(world, (EntityEngineCart) cart, pos);
                break;
        }
    }

    /** True the first tick a cart is on this rail (so one-shot jobs don't repeat every tick). */
    private static boolean entered(EntityMinecart cart, BlockPos pos) {
        long p = pos.toLong();
        if (cart.getEntityData().getLong("pcSmartRail") == p && cart.getEntityData().getLong("pcSmartRailT") >= cart.world.getTotalWorldTime() - 2) {
            cart.getEntityData().setLong("pcSmartRailT", cart.world.getTotalWorldTime());
            return false;
        }
        cart.getEntityData().setLong("pcSmartRail", p);
        cart.getEntityData().setLong("pcSmartRailT", cart.world.getTotalWorldTime());
        return true;
    }

    private static void push(EntityMinecart cart, double sp, double target) {
        if (sp < 0.01) return;                                   // which way? only boost carts that are already rolling
        double f = target / sp;
        if (f > 1) { cart.motionX *= f; cart.motionZ *= f; }
    }

    private static void hold(World world, EntityMinecart cart, BlockPos pos, long until, boolean reverse) {
        Train t = Train.of(cart);
        EnumFacing dir = t.moving();
        if (reverse && dir != null) {
            dir = dir.getOpposite();
            for (EntityMinecart c : t.carts) if (c instanceof EntityEngineCart) ((EntityEngineCart) c).reverse();
        }
        t.stop();
        cart.setPosition(pos.getX() + 0.5, cart.posY, pos.getZ() + 0.5);
        for (EntityMinecart c : t.carts) CartData.hold(c, until, pos.toLong(), false, dir == null ? "" : dir.getName(), 0.4);
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.BLOCKS, 0.8f, 1.4f);
    }

    /** chain this cart to the nearest uncoupled cart within 2 blocks (ahead or behind) */
    private static void autoCouple(World world, EntityMinecart cart) {
        EntityMinecart best = null;
        double bd = 4.0;
        for (EntityMinecart o : world.getEntitiesWithinAABB(EntityMinecart.class, cart.getEntityBoundingBox().grow(2.2, 0.5, 2.2))) {
            if (o == cart || CartLinks.train(cart).contains(o)) continue;
            double d = o.getDistanceSq(cart);
            if (d < bd) { bd = d; best = o; }
        }
        if (best != null && CartLinks.couple(cart, best, CartLinks.Kind.CHAIN) == null)
            world.playSound(null, cart.posX, cart.posY, cart.posZ, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.3f, 1.8f);
    }

    /** "Next stop: station N" to everyone riding the train, from the first numbered Station Track ahead (64 blocks) */
    private static void announce(World world, EntityMinecart cart, BlockPos pos) {
        double sp = Math.sqrt(cart.motionX * cart.motionX + cart.motionZ * cart.motionZ);
        EnumFacing f = EnumFacing.getFacingFromVector((float) (cart.motionX / sp), 0, (float) (cart.motionZ / sp));
        String msg = "§d🚉 §fNo more stations ahead on this line";
        for (int i = 1; i <= 64; i++) {
            BlockPos p = pos.offset(f, i);
            for (int dy = -2; dy <= 2; dy++) {
                IBlockState s = world.getBlockState(p.up(dy));
                if (s.getBlock() instanceof SmartRail && ((SmartRail) s.getBlock()).kind == Kind.STATION) {
                    int n = com.dogpound.pridecarts.carts.Routes.station(world, p.up(dy));
                    msg = "§d🚉 §fNext stop: " + (n == 0 ? "station" : "§dStation " + n) + " §7(" + i + " blocks)";
                    i = 999;
                    break;
                }
            }
        }
        for (EntityMinecart c : Train.of(cart).carts)
            for (Entity p : c.getPassengers()) if (p instanceof net.minecraft.entity.player.EntityPlayer)
                ((net.minecraft.entity.player.EntityPlayer) p).sendStatusMessage(new net.minecraft.util.text.TextComponentString(msg), true);
    }

    private static void fireworks(World world, EntityMinecart cart) {
        if (cart.getEntityData().getLong("pcFw") > world.getTotalWorldTime() - 60) return;
        cart.getEntityData().setLong("pcFw", world.getTotalWorldTime());
        int[] rainbow = {0xE40303, 0xFF8C00, 0xFFED00, 0x008026, 0x24408E, 0x732982, 0x5BCEFA, 0xF5A9B8};
        net.minecraft.nbt.NBTTagCompound ex = new net.minecraft.nbt.NBTTagCompound();
        ex.setIntArray("Colors", rainbow);
        ex.setIntArray("FadeColors", new int[]{0xFFFFFF});
        ex.setByte("Type", (byte) world.rand.nextInt(5));
        ex.setBoolean("Trail", true);
        net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
        list.appendTag(ex);
        net.minecraft.nbt.NBTTagCompound fw = new net.minecraft.nbt.NBTTagCompound();
        fw.setTag("Explosions", list);
        fw.setByte("Flight", (byte) 1);
        ItemStack rocket = new ItemStack(net.minecraft.init.Items.FIREWORKS);
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        tag.setTag("Fireworks", fw);
        rocket.setTagCompound(tag);
        world.spawnEntity(new net.minecraft.entity.item.EntityFireworkRocket(world, cart.posX, cart.posY + 1.5, cart.posZ, rocket));
    }

    private static void fluids(World w, BlockPos pos, com.dogpound.pridecarts.carts.EntityTankCart cart, boolean fill) {
        for (EnumFacing side : EnumFacing.values()) {
            net.minecraft.tileentity.TileEntity te = w.getTileEntity(pos.offset(side));
            if (te == null) continue;
            net.minecraftforge.fluids.capability.IFluidHandler h = te.getCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side.getOpposite());
            if (h == null) continue;
            net.minecraftforge.fluids.FluidTank tank = cart.tank();
            if (fill) {
                net.minecraftforge.fluids.FluidStack got = h.drain(4000, false);
                if (got == null || got.amount <= 0) continue;
                int took = tank.fill(got, true);
                if (took > 0) h.drain(new net.minecraftforge.fluids.FluidStack(got, took), true);
            } else {
                net.minecraftforge.fluids.FluidStack have = tank.drain(4000, false);
                if (have == null) return;
                int put = h.fill(have, true);
                if (put > 0) tank.drain(put, true);
            }
        }
    }

    /** jump to the other Teleport Track with the same number, keeping speed and heading */
    private static void teleport(World w, BlockPos pos, EntityMinecart cart, double sp) {
        int ch = com.dogpound.pridecarts.carts.Routes.station(w, pos);
        if (ch == 0) return;
        BlockPos target = null;
        for (java.util.Map.Entry<Long, Integer> e : com.dogpound.pridecarts.carts.Routes.StationIds.get(w).ids.entrySet()) {
            BlockPos p = BlockPos.fromLong(e.getKey());
            if (e.getValue() == ch && !p.equals(pos) && w.getBlockState(p).getBlock() instanceof SmartRail && ((SmartRail) w.getBlockState(p).getBlock()).kind == Kind.TELEPORT) { target = p; break; }
        }
        com.dogpound.pridecarts.CartEvents.dbg("  teleport target {}", target);
        if (target == null) return;
        EnumFacing f = EnumFacing.getFacingFromVector((float) cart.motionX, 0, (float) cart.motionZ);
        cart.getEntityData().setLong("pcTpT", w.getTotalWorldTime());
        java.util.List<Entity> riders = new ArrayList<>(cart.getPassengers());
        cart.setPositionAndUpdate(target.getX() + 0.5 + f.getFrontOffsetX() * 0.6, target.getY() + 0.1, target.getZ() + 0.5 + f.getFrontOffsetZ() * 0.6);
        cart.motionX = f.getFrontOffsetX() * sp;
        cart.motionZ = f.getFrontOffsetZ() * sp;
        for (Entity r : riders) { r.setPositionAndUpdate(cart.posX, cart.posY + 0.5, cart.posZ); r.startRiding(cart, true); }
        w.playSound(null, target, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.BLOCKS, 0.7f, 1.3f);
    }

    private static void engines(EntityMinecart cart, java.util.function.Consumer<EntityEngineCart> f) {
        for (EntityMinecart c : Train.of(cart).carts)
            if (c instanceof EntityEngineCart && ((EntityEngineCart) c).obeysTracks()) f.accept((EntityEngineCart) c);
    }

    private static void refuel(World world, EntityEngineCart e, BlockPos pos) {
        for (EnumFacing side : EnumFacing.values()) {
            TileEntity te = world.getTileEntity(pos.offset(side));
            if (te == null) continue;
            EnumFacing from = side.getOpposite();
            if (e.isSteam()) {
                IItemHandler inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, from);
                if (inv == null) continue;
                for (int i = 0; i < inv.getSlots(); i++) {
                    ItemStack s = inv.extractItem(i, 1, true);
                    int t = TileEntityFurnace.getItemBurnTime(s);
                    if (t > 0 && e.addFuel(t)) { inv.extractItem(i, 1, false); return; }
                }
            } else {
                IEnergyStorage src = te.getCapability(CapabilityEnergy.ENERGY, from);
                IEnergyStorage dst = e.getCapability(CapabilityEnergy.ENERGY, null);
                if (src == null || dst == null) continue;
                int moved = dst.receiveEnergy(src.extractEnergy(20_000, true), true);
                if (moved > 0) dst.receiveEnergy(src.extractEnergy(moved, false), false);
            }
        }
    }

    private static final float[][] RAINBOW_RGB = {{0.89f, 0.16f, 0.16f}, {1f, 0.55f, 0.1f}, {1f, 0.93f, 0.2f}, {0.1f, 0.6f, 0.2f}, {0.2f, 0.35f, 0.9f}, {0.45f, 0.2f, 0.6f}};

    private static void rainbow(World world, EntityMinecart cart) {
        float[] c = RAINBOW_RGB[(int) (world.getTotalWorldTime() / 2 % 6)];
        world.spawnParticle(EnumParticleTypes.REDSTONE, cart.posX, cart.posY + 0.2, cart.posZ, c[0] == 0 ? 0.001 : c[0], c[1], c[2]);
    }
}
