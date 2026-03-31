package tc.oc.pgm.api.tracker.info;

import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

public interface PotionInfo extends PhysicalInfo, DamageInfo {
  @Nullable
  PotionEffectType potionEffect();

  @Deprecated
  default @Nullable PotionEffectType getPotionEffect() {
    return potionEffect();
  }
}
