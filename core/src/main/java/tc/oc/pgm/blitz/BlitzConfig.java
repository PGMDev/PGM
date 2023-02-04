package tc.oc.pgm.blitz;

import static tc.oc.pgm.util.Assert.assertTrue;

import tc.oc.pgm.api.filter.Filter;

/** Represents information needed to run the Blitz game type. */
public class BlitzConfig {

  private final int lives;
  private final boolean broadcastLives;
  private final Filter filter;
  private final Filter scoreboardFilter;
  private final Filter joinFilter;

  public BlitzConfig(
      int lives,
      boolean broadcastLives,
      Filter filter,
      Filter scoreboardFilter,
      Filter joinFilter) {
    assertTrue(lives > 0, "lives must be greater than zero");

    this.lives = lives;
    this.broadcastLives = broadcastLives;
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

  public Filter getFilter() {
    return this.filter;
  }

  public Filter getScoreboardFilter() {
    return scoreboardFilter;
  }

  public Filter getJoinFilter() {
    return joinFilter;
  }
}
