package com.dogpound.pridecarts.signs;

import com.dogpound.pridecarts.CartData;
import com.dogpound.pridecarts.Train;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Locale;

/**
 * sound — plays a sound at the train's location.
 *   line 3: sound id like "minecraft:block.note.bell" (add "minecraft:" when there is no colon).
 *   line 4: optional "volume pitch" (defaults 1 1).
 */
public class SoundAction implements SignAction {
    @Override
    public String[] names() {
        return new String[]{"sound", "playsound"};
    }

    @Override
    public void enter(ActionSign sign, EntityMinecart cart, Train train) {
        String soundId = sign.arg(2).trim();
        if (soundId.isEmpty()) return;
        
        if (soundId.indexOf(':') == -1) {
            soundId = "minecraft:" + soundId;
        }
        
        ResourceLocation location = new ResourceLocation(soundId);
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(location);
        if (sound == null) return;
        
        String[] args = sign.arg(3).trim().split("\\s+");
        float volume = 1.0f;
        float pitch = 1.0f;
        if (args.length > 0 && !args[0].isEmpty()) {
            try { volume = Float.parseFloat(args[0]); } catch (NumberFormatException ignored) {}
        }
        if (args.length > 1 && !args[1].isEmpty()) {
            try { pitch = Float.parseFloat(args[1]); } catch (NumberFormatException ignored) {}
        }
        
        EntityMinecart head = train.head();
        BlockPos pos = head.getPosition();
        head.world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                            sound, SoundCategory.NEUTRAL, volume, pitch);
    }
}
