package tc.oc.pgm.spawns.events;

import org.bukkit.Location;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Called immediately before a participating player transitions into a specific, living, physical
 * entity in a match world. The player may have been a different physical entity immediately before
 * this (e.g. team change) or may have been a non-interacting observer or not in the match at all.
 *
 * <p>This event fires before the player has been teleported or spawn kits applied. The event knows
 * the location they will spawn at, and it can be changed.
 */
public class ParticipantSpawnEvent extends PlayerSpawnEvent {

  public ParticipantSpawnEvent(MatchPlayer player, Location location) {
    super(player, location);
  }
}
