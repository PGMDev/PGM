package tc.oc.pgm.blitz;

import static com.google.common.base.Preconditions.checkArgument;

import tc.oc.pgm.api.filter.Filter;

/** Represents information needed to run the Blitz game type. */
public class BlitzConfig {

  private final int lives;
  private final boolean broadcastLives;
  private final Filter filter;

  public BlitzConfig(int lives, boolean broadcastLives, Filter filter) {
    checkArgument(lives > 0, "lives must be greater than zero");

    this.lives = lives;
    this.broadcastLives = broadcastLives;
    this.filter = filter;
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
}
