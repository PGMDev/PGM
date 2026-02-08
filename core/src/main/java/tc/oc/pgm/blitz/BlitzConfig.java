package tc.oc.pgm.blitz;

import static tc.oc.pgm.util.Assert.assertTrue;

import lombok.Getter;
import tc.oc.pgm.api.filter.Filter;

/** Represents information needed to run the Blitz game type. */
public class BlitzConfig {

  private final int lives;
  private final boolean broadcastLives;
  private final boolean lightning;

  @Getter
  private final Filter filter;

  @Getter
  private final Filter scoreboardFilter;

  @Getter
  private final Filter joinFilter;

  public BlitzConfig(
      int lives,
      boolean broadcastLives,
      boolean lightning,
      Filter filter,
      Filter scoreboardFilter,
      Filter joinFilter) {
    assertTrue(lives > 0, "lives must be greater than zero");

    this.lives = lives;
    this.broadcastLives = broadcastLives;
    this.lightning = lightning;
    this.filter = filter;
    this.scoreboardFilter = scoreboardFilter;
    this.joinFilter = joinFilter;
  }

  /**
   * Number of lives a player has during the match.
   *
   * @return Number of lives
   */
  public int getNumLives() {
    return this.lives;
  }

  public boolean getBroadcastLives() {
    return this.broadcastLives;
  }

  public boolean getLightning() {
    return this.lightning;
  }
}
