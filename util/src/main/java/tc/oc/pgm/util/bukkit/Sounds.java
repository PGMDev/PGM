package tc.oc.pgm.util.bukkit;

import static net.kyori.adventure.sound.Sound.sound;
import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;

import net.kyori.adventure.sound.Sound;

public interface Sounds {
  Sound WARNING = sound("NOTE_BASS", "BLOCK_NOTE_BLOCK_BASS", 1f, 0.75f);

  static Sound sound(String legacyConstant, String modernConstant, float volume, float pitch) {
    return sound(
        MISC_UTILS.getSound(
            BukkitUtils.parse(org.bukkit.Sound::valueOf, legacyConstant, modernConstant)),
        Sound.Source.MASTER,
        volume,
        pitch);
  }
}
