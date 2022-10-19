package tc.oc.pgm.api.player;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.named.Named;

/** Represents the mapping of a {@link UUID} to a username. */
public interface Username extends Named {

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
  @Override
  @Nullable
  String getNameLegacy();

  /**
   * Change the username of the player.
   *
   * @param name The new username or null.
   */
  void setName(@Nullable String name);
}
