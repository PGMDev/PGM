package tc.oc.pgm.api.match.event;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.world.WorldEvent;
import tc.oc.pgm.api.match.Match;

/** Represents an {@link WorldEvent} that is tied to a {@link Match}. */
public abstract class MatchEvent extends Event {

  private final Match match;

  protected MatchEvent(Match match) {
    this.match = match;
  }

  public World getWorld() {
    return assertNotNull(match.getWorld(), "match event");
  }

  /**
   * Get the {@link Match} related to the {@link MatchEvent}.
   *
   * @return The {@link Match}.
   */
  public final Match getMatch() {
    return match;
  }
}
