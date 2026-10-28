package tc.oc.pgm.platform.sportpaper.modules.damage;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.damage.PlatformDamage;
import tc.oc.pgm.tracker.info.ContactInfo;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpPlatformDamage implements PlatformDamage {
  @Override
  public @Nullable String getDeathKey(DamageCause cause) {
    // 1.8 has no damage causes beyond those shared with every platform
    return null;
  }

  @Override
  public boolean causesKnockback(DamageCause cause) {
    return false;
  }

  @Override
  public ContactInfo.Type getContactType(Material type) {
    // Contact damage only ever comes from cacti in 1.8
    return type == Material.CACTUS ? ContactInfo.Type.CACTUS : ContactInfo.Type.OTHER;
  }
}
