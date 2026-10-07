package com.dogpound.pridecarts.carts;

import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.world.World;

import java.util.function.Supplier;

/**
 * Every mini (minecart-sized) car type from her railway list §1, grouped by what the cart is:
 * something you ride, something that stores items, a tank, or a power car. Each shows its cargo
 * inside (a cushion, a chest, logs, TNT...) so a train reads at a glance.
 */
public enum CartType {
    // ---- passenger (Family.SEAT) -------------------------------------------------------------------
    PASSENGER("passenger", Family.SEAT, 0, () -> carpet(EnumDyeColor.WHITE), "A plain seat."),
    FIRST_CLASS("first_class", Family.SEAT, 0, () -> carpet(EnumDyeColor.PURPLE), "Riders slowly heal and stay fed."),
    SLEEPER("sleeper", Family.SEAT, 0, () -> carpet(EnumDyeColor.BLUE), "When everyone is riding a sleeper at night, the night is skipped."),
    DINING("dining", Family.SEAT, 0, () -> Blocks.CAKE.getDefaultState(), "Feeds its rider a bite every 10 seconds."),
    OBSERVATION("observation", Family.SEAT, 0, () -> carpet(EnumDyeColor.LIGHT_BLUE), "Night vision while you ride."),
    PRISONER("prisoner", Family.SEAT, 0, () -> Blocks.IRON_BARS.getDefaultState(), "You can't get out while it's moving."),
    AMBULANCE("ambulance", Family.SEAT, 0, () -> carpet(EnumDyeColor.RED), "Heals its rider and clears poison."),
    MAINTENANCE_COACH("maintenance_coach", Family.SEAT, 0, () -> carpet(EnumDyeColor.GRAY), "Slowly repairs the tool in your hand."),
    CREW("crew", Family.SEAT, 0, () -> carpet(EnumDyeColor.BROWN), "Haste while riding: the crew gets to work fast."),
    CABOOSE("caboose", Family.SEAT, 0, () -> carpet(EnumDyeColor.ORANGE), "End of the train: night vision and a lookout."),
    SUBWAY("subway", Family.SEAT, 0, () -> carpet(EnumDyeColor.SILVER), "Metro seat."),
    TRAM("tram", Family.SEAT, 0, () -> carpet(EnumDyeColor.YELLOW), "Street tram seat."),
    HIGH_SPEED("high_speed", Family.SEAT, 0, () -> carpet(EnumDyeColor.LIME), "Allowed to go faster on high-speed track."),
    // ---- freight that holds items (Family.STORAGE), size = slots -----------------------------------
    BAGGAGE("baggage", Family.STORAGE, 18, () -> Blocks.CHEST.getDefaultState(), "18 slots of luggage."),
    MAIL("mail", Family.STORAGE, 27, () -> Blocks.BOOKSHELF.getDefaultState(), "27 slots of letters and parcels."),
    BOXCAR_COVERED("covered_boxcar", Family.STORAGE, 27, () -> Blocks.PLANKS.getStateFromMeta(1), "Covered boxcar, 27 slots."),
    BOXCAR_OPEN("open_boxcar", Family.STORAGE, 27, () -> Blocks.WOODEN_SLAB.getStateFromMeta(0), "Open boxcar, 27 slots."),
    CONTAINER("container", Family.STORAGE, 36, () -> Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(1), "Shipping container, 36 slots."),
    WAREHOUSE("warehouse", Family.STORAGE, 54, () -> Blocks.PLANKS.getStateFromMeta(5), "A rolling warehouse: 54 slots."),
    REFRIGERATED("refrigerated", Family.STORAGE, 27, () -> Blocks.PACKED_ICE.getDefaultState(), "Food only, 27 slots."),
    ORE("ore", Family.STORAGE, 36, () -> Blocks.IRON_ORE.getDefaultState(), "Ores, stone and dusts only, 36 slots."),
    LOG("log", Family.STORAGE, 36, () -> Blocks.LOG.getDefaultState(), "Logs, planks, saplings and sticks only, 36 slots."),
    HOPPER("hopper", Family.STORAGE, 27, () -> Blocks.HOPPER.getDefaultState(), "Picks up items lying on the track."),
    DUMP("dump", Family.STORAGE, 27, () -> Blocks.GRAVEL.getDefaultState(), "Dumps everything beside the track on a powered activator rail."),
    EXPLOSIVES("explosives", Family.STORAGE, 18, () -> Blocks.TNT.getDefaultState(), "Careful: if it burns or is blown up with TNT inside, it goes off."),
    MACHINERY("machinery", Family.STORAGE, 27, () -> Blocks.PISTON.getDefaultState(), "Machine parts, 27 slots."),
    FLATBED("flatbed", Family.STORAGE, 18, () -> Blocks.STONE_SLAB.getDefaultState(), "Flatbed, 18 slots."),
    HEAVY_FLATBED("heavy_flatbed", Family.STORAGE, 36, () -> Blocks.IRON_BLOCK.getDefaultState(), "Heavy flatbed, 36 slots."),
    // ---- tanks (Family.TANK), size = millibuckets -------------------------------------------------
    TANKER("tanker", Family.TANK, 16000, () -> Blocks.CAULDRON.getDefaultState(), "Any fluid, 16 buckets."),
    LIQUID_TANKER("liquid_tanker", Family.TANK, 32000, () -> Blocks.GLASS.getDefaultState(), "Liquids only, 32 buckets."),
    GAS_TANKER("gas_tanker", Family.TANK, 32000, () -> Blocks.CONCRETE.getStateFromMeta(0), "Gases only, 32 buckets."),
    // ---- power (Family.POWER), size = FE -----------------------------------------------------------
    BATTERY("battery", Family.POWER, 2_000_000, () -> Blocks.REDSTONE_BLOCK.getDefaultState(), "Stores 2,000,000 FE; charges and discharges at Cart Loaders."),
    GENERATOR("generator", Family.POWER, 200_000, () -> Blocks.LIT_FURNACE.getDefaultState(), "Right-click with fuel: burns it into FE."),
    // ---- mini engines (Family.ENGINE): pull a coupled train; size = fuel ticks / FE buffer ---------
    STEAM_ENGINE("steam_engine", Family.ENGINE, 64_000, () -> Blocks.FURNACE.getDefaultState(), "Burns furnace fuel. Strong, smoky, a classic."),
    ELECTRIC_ENGINE("electric_engine", Family.ENGINE, 400_000, () -> Blocks.IRON_BLOCK.getDefaultState(), "Runs on Forge Energy: charge it at a Cart Loader or with cables."),
    SOLAR_ENGINE("solar_engine", Family.ENGINE, 40_000, () -> Blocks.DAYLIGHT_DETECTOR.getDefaultState(), "Basic solar: charges in sunlight, bursts of travel."),
    SOLAR_ENGINE_ADVANCED("solar_engine_advanced", Family.ENGINE, 120_000, () -> Blocks.DAYLIGHT_DETECTOR.getDefaultState(), "Advanced solar: runs a light train all day."),
    SOLAR_ENGINE_ELITE("solar_engine_elite", Family.ENGINE, 300_000, () -> Blocks.DAYLIGHT_DETECTOR_INVERTED.getDefaultState(), "Elite solar: full power in sunlight, a big battery for the night.");

    public enum Family { SEAT, STORAGE, TANK, POWER, ENGINE }

    /** Cars that have a Blockbench model so far (grows as they're made). */
    private static final String[] MODELLED = {"*"};
    static { for (CartType t : values()) for (String m : MODELLED) if (m.equals("*") || t.id.equals(m)) t.modelled = true; }

    public final String id;
    public final Family family;
    /** slots / millibuckets / FE, by family */
    public final int size;
    private final Supplier<IBlockState> display;
    public final String tip;
    /** Has its own Blockbench model (models/item/cart_<id>.json); set in {@link #MODELLED}. */
    public boolean modelled;

    CartType(String id, Family family, int size, Supplier<IBlockState> display, String tip) {
        this.id = id;
        this.family = family;
        this.size = size;
        this.display = display;
        this.tip = tip;
    }

    public IBlockState display() {
        return display.get();
    }

    public static CartType byOrdinal(int i) {
        CartType[] v = values();
        return i >= 0 && i < v.length ? v[i] : PASSENGER;
    }

    public EntityMinecart create(World w, double x, double y, double z) {
        switch (family) {
            case STORAGE: return new EntityStorageCart(w, x, y, z, this);
            case TANK: return new EntityTankCart(w, x, y, z, this);
            case POWER: return new EntityPowerCart(w, x, y, z, this);
            case ENGINE: return new EntityEngineCart(w, x, y, z, this);
            default: return new EntitySeatCart(w, x, y, z, this);
        }
    }

    private static IBlockState carpet(EnumDyeColor c) {
        return Blocks.CARPET.getDefaultState().withProperty(BlockCarpet.COLOR, c);
    }
}
