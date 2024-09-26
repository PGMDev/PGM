package tc.oc.pgm.action;

import net.kyori.adventure.sound.Sound;
import tc.oc.pgm.util.bukkit.Sounds;

public enum SoundType {
  CUSTOM(Sounds.FALLBACK),
  TIP(Sounds.TIP),
  ALERT(Sounds.ALERT),
  PORTAL(Sounds.PORTAL),
  SCORE(Sounds.SCORE),
  OBJECTIVE_FIREWORKS_FAR(Sounds.OBJECTIVE_FIREWORKS_FAR),
  OBJECTIVE_FIREWORKS_TWINKLE(Sounds.OBJECTIVE_FIREWORKS_TWINKLE),
  OBJECTIVE_GOOD(Sounds.OBJECTIVE_GOOD),
  OBJECTIVE_BAD(Sounds.OBJECTIVE_BAD),
  OBJECTIVE_MODE(Sounds.OBJECTIVE_MODE),
  DEATH_OWN(Sounds.DEATH_OWN),
  DEATH_OTHER(Sounds.DEATH_ENEMY);

  private final Sound sound;

  SoundType(Sound sound) {
    this.sound = sound;
  }

  public String getResource() {
    return sound.name().value();
  }

  public float getVolume() {
    return sound.volume();
  }

  public float getPitch() {
    return sound.pitch();
  }
}
