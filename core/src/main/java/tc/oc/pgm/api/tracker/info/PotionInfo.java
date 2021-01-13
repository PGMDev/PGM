package tc.oc.pgm.api.tracker.info;

import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface PotionInfo extends PhysicalInfo, DamageInfo {
  @Nullable
  PotionEffectType getPotionEffect();
}
