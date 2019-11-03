package tc.oc.pgm.broadcast;

import com.google.common.collect.Multimap;
import org.joda.time.Duration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.match.MatchModule;

public class BroadcastMatchModule extends MatchModule {

  private final Multimap<Duration, Broadcast> broadcasts;
  private final CountdownContext countdowns;

  public BroadcastMatchModule(Match match, Multimap<Duration, Broadcast> broadcasts) {
    super(match);
    this.broadcasts = broadcasts;
    this.countdowns = new CountdownContext(match, match.getLogger());
  }

  @Override
  public void enable() {
    for (Broadcast broadcast : this.broadcasts.values()) {
      this.countdowns.start(
          new BroadcastCountdown(this.match, broadcast),
          broadcast.after,
          broadcast.every,
          broadcast.count);
    }
  }

  @Override
  public void disable() {
    this.countdowns.cancelAll();
  }
}
