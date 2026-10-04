package tc.oc.pgm.rotation.vote;

import java.util.logging.Level;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public enum ElectoralSystem {
  /**
   * <a href="https://en.wikipedia.org/wiki/Approval_voting">Approval voting</a> is a system where
   * players cast votes for all the maps they want, and the most voted map wins.
   */
  APPROVAL_VOTING,
  /**
   * <a href="https://en.wikipedia.org/wiki/Random_ballot">Random ballot</a> is a system where
   * players cast votes for all the maps they want, and one ballot is randomly drawn deciding the
   * map. This means that the chances of a map winning are directly proportional to how many votes
   * it received.
   */
  RANDOM_BALLOT;

  public static ElectoralSystem of(String str) {
    try {
      if (str != null && !str.isBlank()) return TextParser.parseEnum(str, ElectoralSystem.class);
    } catch (TextException e) {
      PGM.get().getGameLogger().log(Level.WARNING, e.getLocalizedMessage(), e);
    }
    return APPROVAL_VOTING; // default or fallback
  }
}
