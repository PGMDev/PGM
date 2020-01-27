package tc.oc.pgm.api.match.factory;

import java.util.concurrent.Future;
import tc.oc.pgm.api.match.Match;

/** A {@link Future} that creates a {@link Match}. */
public interface MatchFactory extends Future<Match> {

  /**
   * Clears any delays on the {@link Future} and ensures the {@link Match} is created as quickly as
   * possible.
   */
  void await();
}
