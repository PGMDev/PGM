package tc.oc.pgm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.events.MatchPlayerDeathEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;

public class KillStreakListener implements Listener {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void updateKillStreak(final MatchPlayerDeathEvent event) {
    if (event.isChallengeKill()) {
      MatchPlayer killer = event.getOnlineKiller();
      if (killer != null) killer.setKillStreak(killer.getKillStreak() + 1);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void resetKillStreakOnSpawn(final ParticipantSpawnEvent event) {
    event.getPlayer().setKillStreak(0);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void resetKillStreakOnTeamChange(final PlayerPartyChangeEvent event) {
    // Probably redundant, but on the off chance we ever have a way to change teams
    // without respawning, we would probably still want to end the player's kill streak.
    event.getPlayer().setKillStreak(0);
  }
}
