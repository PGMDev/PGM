package tc.oc.pgm.api.datastore;

import java.util.UUID;
import javax.annotation.Nullable;
import tc.oc.pgm.api.discord.DiscordId;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.Settings;

/** A fast, persistent datastore that provides synchronous responses. */
public interface Datastore {

  /**
   * Get the username for a given player {@link UUID}.
   *
   * @param id The {@link UUID} of a player.
   * @return A {@link Username}.
   */
  Username getUsername(UUID id);

  /**
   * Get the settings for a given player {@link UUID}.
   *
   * @param id The {@link UUID} of a player.
   * @return A {@link Settings}.
   */
  Settings getSettings(UUID id);

  /**
   * Get the discord identity for a given player {@link UUID}.
   *
   * @param id The {@link UUID} of a player.
   * @return A {@link DiscordId}.
   */
  DiscordId getDiscordId(UUID id);

  /**
   * Get or search for a one-time-pin for a given player {@link UUID}.
   *
   * @param id The {@link UUID} of a player, or {@code null} to search by {@param pin}.
   * @param pin The pin, or {@code null} to generate a new pin.
   * @return A one-time-pin or {@code null} if not found.
   */
  @Nullable
  OneTimePin getOneTimePin(@Nullable UUID id, @Nullable String pin);
}
