package tc.oc.pgm.damage;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.tracker.info.ContactInfo;
import tc.oc.pgm.util.platform.Platform;

public interface PlatformDamage {
  PlatformDamage PLATFORM_DAMAGE = Platform.get(PlatformDamage.class);

  @Nullable
  String getDeathKey(DamageCause cause);

  boolean causesKnockback(DamageCause cause);

  ContactInfo.Type getContactType(Material type);
}
