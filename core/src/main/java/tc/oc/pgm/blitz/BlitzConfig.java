package tc.oc.pgm.blitz;

import static tc.oc.pgm.util.Assert.assertTrue;

import lombok.Getter;
import tc.oc.pgm.api.filter.Filter;

/** Represents information needed to run the Blitz game type. */
public class BlitzConfig {

  /** Number of lives a player has during the match. */
  @Getter
  private final int numLives;

  private final boolean broadcastLives;
  private final boolean lightning;

  @Getter
  private final Filter filter;

  @Getter
  private final Filter scoreboardFilter;

  @Getter
  private final Filter joinFilter;

  public BlitzConfig(
      int numLives,
      boolean broadcastLives,
      boolean lightning,
      Filter filter,
      Filter scoreboardFilter,
      Filter joinFilter) {
    assertTrue(numLives > 0, "lives must be greater than zero");

    this.numLives = numLives;
    this.broadcastLives = broadcastLives;
    this.lightning = lightning;
    this.filter = filter;
    this.scoreboardFilter = scoreboardFilter;
    this.joinFilter = joinFilter;
  }

  public boolean getBroadcastLives() {
    return this.broadcastLives;
  }

  public boolean getLightning() {
    return this.lightning;
  }
}
