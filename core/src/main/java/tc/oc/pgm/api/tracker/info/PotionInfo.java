package tc.oc.pgm.api.tracker.info;

import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public interface PotionInfo extends PhysicalInfo, DamageInfo {
  @Nullable
  PotionEffectType getPotionEffect();
}
