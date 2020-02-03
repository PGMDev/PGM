package tc.oc.pgm.api.datastore;

import java.util.UUID;

/**
 * A one-time-pin, also know as "OTP," for verifying third-party accounts.
 *
 * @link https://en.wikipedia.org/wiki/One-time_password
 */
public interface OneTimePin {

  /**
   * Get the Minecraft {@link UUID} of the player that requested the pin.
   *
   * @return A unique identifier.
   */
  UUID getId();

  /**
   * Get the secure, random pin.
   *
   * @return A pin.
   */
  String getPin();

  /**
   * Get whether the pin is valid to be used.
   *
   * @return Will be {@code false} if the token has expired or was previously used.
   */
  boolean isValid();

  /** Mark the pin as permanently used. */
  void markAsUsed();
}
