package tc.oc.pgm.api.db;

import java.util.UUID;

/** A fast, persistent datastore that provides synchronous, cached responses. */
public interface Datastore {

  /**
   * Get the username for a given {@link UUID}.
   *
   * @param uuid The {@link UUID} of a Minecraft user.
   * @return A username.
   */
  Username getUsername(UUID uuid);
}
