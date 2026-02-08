package tc.oc.pgm.core;

import lombok.Getter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;

@Getter
public abstract class CoreEvent extends MatchEvent {
  protected final Core core;

  public CoreEvent(Match match, Core core) {
    super(match);
    this.core = core;
  }
}
