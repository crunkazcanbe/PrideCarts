package com.dogpound.pridecarts;

import com.dogpound.pridecarts.signs.SignAction;
import com.dogpound.pridecarts.signs.StationAction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PrideCarts — TrainCarts' minecart features on Forge 1.12.2: action signs, train properties, /train commands.
 * Inspired by TrainCarts by bergerhealer (MIT licence). Works with Railcraft (its linked carts are our trains).
 */
@Mod(modid = PrideCarts.MODID, name = "PrideCarts", version = PrideCarts.VERSION, acceptableRemoteVersions = "*")
public class PrideCarts {
    public static final String MODID = "pridecarts", VERSION = "0.1.0";
    public static final Logger LOG = LogManager.getLogger("PrideCarts");
    /** trains with a destination steer themselves at junctions without a switcher sign */
    public static boolean autoRoute = true;

    @Mod.EventHandler
    public void pre(FMLPreInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(CartEvents.class);
        com.dogpound.pridecarts.net.PcNet.init();
        SignAction.register(new StationAction());
        SignAction.register(new com.dogpound.pridecarts.signs.SwitcherAction());
        Actions.registerAll();
    }

    @Mod.EventHandler
    public void start(FMLServerStartingEvent e) {
        boolean taken = e.getServer().getCommandManager().getCommands().containsKey("train");
        e.registerServerCommand(new TrainCommand(taken ? "ptrain" : "train"));
        LOG.info("PrideCarts: {} sign actions, /{} ready", SignAction.ALL.size(), taken ? "ptrain" : "train");
    }
}
