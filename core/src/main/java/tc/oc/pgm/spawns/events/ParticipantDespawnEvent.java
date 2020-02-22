package tc.oc.pgm.spawns.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

/**
 * Called immediately before a participating player ceases to be a specific living, physical entity
 * in the match world, for whatever reason. This can happen as a result of death, team change,
 * leaving the match, or the match ending.
 *
 * <p>It is guaranteed that a matching despawn event will be called for every {@link
 * ParticipantSpawnEvent}, before the match ends.
 *
 * <p>TODO: Figure out a way for other modules to apply kits/items through this event. This is a bit
 * trickier than it sounds, because they have to be applied in the right order, some before the
 * actual spawn kit and some after it. Displaced items also need to be handled properly.
 */
public class ParticipantDespawnEvent extends MatchPlayerEvent {

  protected final Location location;

  public ParticipantDespawnEvent(MatchPlayer player, Location location) {
    super(player);
    this.location = location;
  }

  /** The player's final location before despawning */
  public Location getLocation() {
    return location;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
