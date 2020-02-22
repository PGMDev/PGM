package tc.oc.pgm.blitz;

import static com.google.common.base.Preconditions.checkArgument;

/** Represents information needed to run the Blitz game type. */
public class BlitzConfig {
  public BlitzConfig(int lives, boolean broadcastLives) {
    checkArgument(lives > 0, "lives must be greater than zero");

    this.lives = lives;
    this.broadcastLives = broadcastLives;
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

  final int lives;
  final boolean broadcastLives;
}
