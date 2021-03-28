package tc.oc.pgm.spawns.events;

import org.bukkit.Location;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.states.Alive;

/**
 * Called after a participating player is done spawning(teleported, kits applied etc.)
 *
 * @see ParticipantSpawnEvent
 * @see Alive#enterState()
 */
public class ParticipantPostSpawnEvent extends PlayerPostSpawnEvent {
  public ParticipantPostSpawnEvent(MatchPlayer player, Location location) {
    super(player, location);
  }
}
