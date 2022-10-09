package tc.oc.pgm.api.map;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.named.Named;

/** A named individual that helped contribute to a {@link MapInfo}. */
public interface Contributor extends Named {

  /**
   * Get a short, human-readable description of their contribution.
   *
   * @return The contribution or {@code null} for unspecified.
   */
  @Nullable
  String getContribution();

  /**
   * Get whether the individual is a given player.
   *
   * @param id A player id.
   * @return Whether the contributor is the player.
   */
  boolean isPlayer(UUID id);
}
