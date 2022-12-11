package tc.oc.pgm.api.player;

import java.util.Collection;
import tc.oc.pgm.api.integration.VanishIntegration;

/** A manager that holds information related to {@link MatchPlayer}s who are vanished. */
public interface VanishManager extends VanishIntegration {

  /**
   * A collection of {@link MatchPlayer} who are online and vanished.
   *
   * @return A collection of {@link MatchPlayer}
   */
  Collection<MatchPlayer> getOnlineVanished();

  /** Called when the vanish manager is disabled */
  void disable();
}
