package tc.oc.pgm.api.player;

import java.util.UUID;
import java.util.function.Consumer;
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
   * Change the username of the player. Operation occurs synchronously.
   *
   * @param name The new username or null.
   * @return Whether the operation was successful.
   */
  boolean setNameSync(@Nullable String name);

  /**
   * Change the username of the player. Operation occurs asynchronously.
   *
   * @param name The new username or null.
   * @param callback Callback that consumes whether the operation was successful or not.
   */
  void setName(@Nullable String name, @Nullable Consumer<Boolean> callback);

  /**
   * Change the username of the player. Operation occurs asynchronously.
   *
   * @param name The new username or null.
   */
  default void setName(@Nullable String name) {
    setName(name, null);
  }
}
