package tc.oc.pgm.api.db;

import java.util.UUID;
import javax.annotation.Nullable;

/** Represents the mapping of a {@link UUID} to a username that expires at a given time. */
public interface Username {

  /**
   * Get the unique {@link UUID} of the user.
   *
   * @return The unique {@link UUID}.
   */
  UUID getId();

  /**
   * Get the username of the user.
   *
   * @return The username.
   */
  @Nullable
  String getName();

  /**
   * Change the username of the user, also updating the expiration.
   *
   * @param name The new username or null.
   * @return Whether the operation was successful.
   */
  boolean setName(@Nullable String name);
}
