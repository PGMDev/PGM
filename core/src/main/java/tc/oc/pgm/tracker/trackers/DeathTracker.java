package tc.oc.pgm.tracker.trackers;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.damagehistory.DamageHistoryMatchModule;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.GenericDamageInfo;
import tc.oc.pgm.util.ClassLogger;

/**
 * - Resolves all damage done to players and tracks the most recent one - Wraps {@link
 * PlayerDeathEvent}s in a {@link MatchPlayerDeathEvent}, together with the causing info - Displays
 * death messages
 */
public class DeathTracker implements Listener {

  private final Logger logger;
  private final TrackerMatchModule tmm;
  private final Match match;
  private final Map<MatchPlayer, DamageInfo> lastDamageInfos = new HashMap<>();

  public DeathTracker(TrackerMatchModule tmm) {
    this.logger = ClassLogger.get(tmm.getMatch().getLogger(), getClass());
    this.tmm = tmm;
    this.match = tmm.getMatch();
  }

  // Trackers will do their cleanup at MONITOR level, so we listen at
  // HIGHEST to make sure all the info is still available.
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerDamage(EntityDamageEvent event) {
    MatchPlayer victim = match.getParticipant(event.getEntity());
    if (victim == null) return;

    lastDamageInfos.put(victim, tmm.resolveDamage(event));
  }

  @Nullable
  DamageInfo getLastDamage(MatchPlayer victim) {
    DamageInfo info = lastDamageInfos.get(victim);
    if (info != null) return info;

    EntityDamageEvent damageEvent = victim.getBukkit().getLastDamageCause();
    if (damageEvent != null) {
      return tmm.resolveDamage(damageEvent);
    }

    return null;
  }

  /** Must run after {@link tc.oc.pgm.spawns.SpawnMatchModule#onVanillaDeath} */
  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerDeath(PlayerDeathEvent event) {
    logger.fine("Wrapping " + event);
    MatchPlayer victim = match.getParticipant(event.getEntity());
    if (victim == null || victim.isDead()) return;

    DamageInfo info = getLastDamage(victim);
    if (info == null) info = new GenericDamageInfo(EntityDamageEvent.DamageCause.CUSTOM);

    ParticipantState assister = null;
    DamageHistoryMatchModule dhm = match.getModule(DamageHistoryMatchModule.class);
    if (dhm != null) assister = dhm.getAssister(victim);

    match.callEvent(
        new MatchPlayerDeathEvent(
            event, victim, info, CombatLogTracker.isCombatLog(event), assister));
  }
}
