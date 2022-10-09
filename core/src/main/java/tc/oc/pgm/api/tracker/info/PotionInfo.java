package tc.oc.pgm.api.tracker.info;

import org.jetbrains.annotations.Nullable;
import org.bukkit.potion.PotionEffectType;

public interface PotionInfo extends PhysicalInfo, DamageInfo {
  @Nullable
  PotionEffectType getPotionEffect();
}
