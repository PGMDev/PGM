package tc.oc.pgm.destroyable;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

/** Provides additional information about why a {@link Destroyable} was damaged. */
public class DestroyableHealthChange {
  private final @NotNull BlockState oldState;
  private final @NotNull BlockState newState;
  private final @Nullable ParticipantState playerCause;
  private final int healthChange;

  /**
   * Creates an instance of the class.
   *
   * @param oldState State the destroyed block was in before broken
   * @param playerCause Player most responsible for the damage
   */
  public DestroyableHealthChange(
      @NotNull BlockState oldState,
      @NotNull BlockState newState,
      @Nullable ParticipantState playerCause,
      int healthChange) {
    assertNotNull(oldState, "old block state");

    this.oldState = oldState;
    this.newState = newState;
    this.playerCause = playerCause;
    this.healthChange = healthChange;
  }

  /**
   * Gets the block that was damaged.
   *
   * @return Damaged block
   */
  public @NotNull Block getBlock() {
    return this.oldState.getBlock();
  }

  /**
   * Gets the state of the block before being damaged.
   *
   * @return Old block state
   */
  public @NotNull BlockState getOldState() {
    return this.oldState;
  }

  /**
   * Gets the state of the block after being damaged.
   *
   * @return Old block state
   */
  public @NotNull BlockState getNewState() {
    return this.newState;
  }

  /**
   * Gets the player responsible for the damage.
   *
   * @return Player responsible for the damage or null if none exists
   */
  public @Nullable ParticipantState getPlayerCause() {
    return this.playerCause;
  }

  public int getHealthChange() {
    return healthChange;
  }
}
