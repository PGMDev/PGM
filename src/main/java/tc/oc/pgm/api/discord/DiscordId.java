package tc.oc.pgm.api.discord;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A Discord identity that is linked to a Minecraft account.
 *
 * @link https://discordapp.com/developers/docs/resources/user
 */
public interface DiscordId {

  /**
   * Get the Minecraft {@link UUID} of the user.
   *
   * @return A unique identifier.
   */
  UUID getId();

  /**
   * Get the Discord identifier, or "snowflake," of the user.
   *
   * <p>If the Minecraft account has not yet been linked to a Discord user, this should be {@code
   * null}.
   *
   * @return A unique identifier or {@code null} if not linked.
   */
  @Nullable
  Long getSnowflake();

  /**
   * Set the Discord identifier, or "snowflake," of the user.
   *
   * @param snowflake The new Discord identifier or {@code null} to unlink.
   */
  void setSnowflake(@Nullable Long snowflake);
}
