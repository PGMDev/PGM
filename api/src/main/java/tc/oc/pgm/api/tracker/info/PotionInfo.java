package tc.oc.pgm.api.tracker.info;

import javax.annotation.Nullable;
import org.bukkit.potion.PotionEffectType;

public interface PotionInfo extends PhysicalInfo, DamageInfo {
  @Nullable
  PotionEffectType getPotionEffect();
}
