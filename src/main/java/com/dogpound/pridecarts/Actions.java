package com.dogpound.pridecarts;

import com.dogpound.pridecarts.signs.*;

/**
 * Registers all sign actions with the global registry.
 */
public class Actions {
    public static void registerAll() {
        SignAction.register(new LauncherAction());
        SignAction.register(new BlockerAction());
        SignAction.register(new DestroyAction());
        SignAction.register(new EjectAction());
        SignAction.register(new EnterAction());
        SignAction.register(new AnnounceAction());
        SignAction.register(new SoundAction());
        SignAction.register(new PropertyAction());
        SignAction.register(new DestinationAction());
        SignAction.register(new FlipAction());
        SignAction.register(new WaiterAction());
        SignAction.register(new MutexAction());
        SignAction.register(new EffectAction());
        SignAction.register(new TransferAction());
        SignAction.register(new SkipAction());
    }
}
