package tc.oc.pgm.platform.modern.modules.damage;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.damage.PlatformDamage;
import tc.oc.pgm.tracker.info.ContactInfo;
import tc.oc.pgm.util.platform.Supports;

@Supports(PAPER)
public class ModernPlatformDamage implements PlatformDamage {
  @Override
  public @Nullable String getDeathKey(DamageCause cause) {
    return switch (cause) {
      case FREEZE -> "freeze";
      case FLY_INTO_WALL -> "flyIntoWall";
      case CRAMMING -> "cramming";
      case DRYOUT -> "dryout";
      case WORLD_BORDER -> "worldBorder";
      case SONIC_BOOM -> "sonicBoom";
      default -> null;
    };
  }

  @Override
  public boolean causesKnockback(DamageCause cause) {
    return cause == DamageCause.ENTITY_SWEEP_ATTACK;
  }

  @Override
  public ContactInfo.Type getContactType(Material type) {
    // Magma blocks and campfires are reported as CONTACT, rather than HOT_FLOOR or CAMPFIRE
    if (type == Material.CACTUS) return ContactInfo.Type.CACTUS;
    if (type == Material.MAGMA_BLOCK) return ContactInfo.Type.HOT_FLOOR;
    if (Tag.CAMPFIRES.isTagged(type)) return ContactInfo.Type.CAMPFIRE;
    if (type == Material.SWEET_BERRY_BUSH) return ContactInfo.Type.SWEET_BERRY_BUSH;
    if (type == Material.POINTED_DRIPSTONE) return ContactInfo.Type.STALAGMITE;
    return ContactInfo.Type.OTHER;
  }
}
