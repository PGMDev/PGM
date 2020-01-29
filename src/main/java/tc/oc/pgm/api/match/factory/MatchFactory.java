package tc.oc.pgm.api.match.factory;

import java.util.concurrent.Future;
import tc.oc.pgm.api.match.Match;

/** A {@link Future} that creates a {@link Match}. */
public interface MatchFactory extends Future<Match> {

  /** Resets delays on the {@link Future} and creates the {@link Match} as quickly as possible. */
  void await();
}
