package tc.oc.pgm.events;

import org.bukkit.Physical;
import org.bukkit.World;
import org.bukkit.event.Event;
import tc.oc.pgm.match.Match;

public abstract class MatchEvent extends Event implements Physical {
  protected final Match match;

  protected MatchEvent(Match match) {
    super();
    this.match = match;
  }

  /** Gets the match that this event occurs in. */
  public Match getMatch() {
    return this.match;
  }

  @Override
  public World getWorld() {
    return match.getWorld();
  }
}
