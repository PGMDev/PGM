package tc.oc.pgm.api.match.event;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.event.world.WorldEvent;
import tc.oc.pgm.api.match.Match;

/** Represents an {@link WorldEvent} that is tied to a {@link Match}. */
public abstract class MatchEvent extends WorldEvent {

  private final Match match;

  protected MatchEvent(Match match) {
    super(checkNotNull(match.getWorld(), "match event"));
    this.match = match;
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
