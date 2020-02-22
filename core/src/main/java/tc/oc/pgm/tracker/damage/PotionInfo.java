package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import org.bukkit.potion.PotionEffectType;

public interface PotionInfo extends PhysicalInfo, DamageInfo {
  @Nullable
  PotionEffectType getPotionEffect();
}
