package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import java.util.Locale;

/**
 * effect / particle — puff a particle around every cart of the train when it reaches the sign.
 *   line 3: particle name as in EnumParticleTypes (e.g. "flame", "heart", "smoke", "note", "portal"), default "cloud".
 *   line 4: optional "count spread" (defaults 20 and 0.5).
 */
public class EffectAction implements SignAction {
    @Override
    public String[] names() { return new String[]{"effect", "particle"}; }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        if (!(sign.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) sign.world;

        String name = sign.arg(2).trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) name = "cloud";

        EnumParticleTypes type;
        try {
            type = EnumParticleTypes.getByName(name);
        } catch (Throwable ignored) {
            return;
        }
        if (type == null) return;

        int count = 20;
        float spread = 0.5f;
        String spec = sign.arg(3).trim().replace(',', ' ');
        if (!spec.isEmpty()) {
            String[] parts = spec.split("\\s+");
            if (parts.length > 0) {
                try { count = Math.max(0, Integer.parseInt(parts[0])); } catch (NumberFormatException ignored) {}
            }
            if (parts.length > 1) {
                try { spread = Math.max(0f, Float.parseFloat(parts[1])); } catch (NumberFormatException ignored) {}
            }
        }
        if (count <= 0) return;

        for (EntityMinecart c : train.carts) {
            ws.spawnParticle(type, c.posX, c.posY + 0.5, c.posZ, count, spread, spread, spread, 0.02);
        }
    }
}
