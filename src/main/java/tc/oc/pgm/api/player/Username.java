package tc.oc.pgm.api.player;

import java.util.UUID;
import javax.annotation.Nullable;

/** Represents the mapping of a {@link UUID} to a username. */
public interface Username {

  /**
   * Get the unique {@link UUID} of the player.
   *
   * @return The unique {@link UUID}.
   */
  UUID getId();

  /**
   * Get the username of the player.
   *
   * @return The username.
   */
  @Nullable
  String getName();

  /**
   * Change the username of the player.
   *
   * @param name The new username or null.
   * @return Whether the operation was successful.
   */
  boolean setName(@Nullable String name);
}
