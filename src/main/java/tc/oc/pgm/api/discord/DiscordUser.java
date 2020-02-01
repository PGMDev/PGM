package tc.oc.pgm.api.discord;

import java.util.UUID;
import javax.annotation.Nullable;

/** Represents the mapping of a {@link UUID} to a DiscordId. */
public interface DiscordUser {
  /**
   * Get the Discord username of the player.
   *
   * @return The username.
   */
  @Nullable
  String getUsername();

  /**
   * Change the Discord username of the player.
   *
   * @param username The new username.
   */
  void setUsername(String username);

  /**
   * Get the verification token of the player.
   *
   * @return The token.
   */
  String getToken();
}
