package tc.oc.pgm.tracker.trackers;

import static tc.oc.pgm.damage.PlatformDamage.PLATFORM_DAMAGE;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.tracker.DamageResolver;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.BlockInfo;
import tc.oc.pgm.tracker.info.ContactInfo;

/** Resolves damage from contact with blocks, and tracks who placed blocks which deal it. */
public class ContactTracker extends AbstractTracker<BlockInfo> implements DamageResolver {

  public ContactTracker(TrackerMatchModule tmm, Match match) {
    super(BlockInfo.class, tmm, match);
  }

  @Nullable
  @Override
  public DamageInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager) {
    if (damageType != EntityDamageEvent.DamageCause.CONTACT) return null;

    // Contact with entities, such as a sulfur cube, is resolved like any other attack
    if (damager != null && !(damager instanceof BlockInfo)) return null;

    // Contact damage without a block, such as from a plugin, was only ever cactus damage on legacy
    ContactInfo.Type type = damager == null
        ? ContactInfo.Type.CACTUS
        : PLATFORM_DAMAGE.getContactType(((BlockInfo) damager).getMaterial().getItemType());

    // The damaging block is already resolved through the block tracker, so it knows its placer
    return new ContactInfo(type, damager);
  }

  @SuppressWarnings("deprecation")
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlace(ParticipantBlockTransformEvent event) {
    if (PLATFORM_DAMAGE.getContactType(event.getNewState().getType()) != ContactInfo.Type.OTHER) {
      blocks()
          .trackBlockState(
              event.getNewState(), new BlockInfo(event.getNewState(), event.getPlayerState()));
    }
  }
}
