package tc.oc.pgm.api.map;

import javax.annotation.Nullable;
import tc.oc.pgm.api.Datastore;

/* Represents data related to persisting map pool info between server restarts */
public interface MapActivity {

  /**
   * Get the name of the map pool.
   *
   * @return The name of the map pool
   */
  public String getPoolName();

  /**
   * For rotation based pools, will be the map that is resumed upon server restart.
   *
   * @return The name of a map
   */
  public String getMapName();

  /**
   * Whether this map pool was the last active. If the map pool was the last active, it will be
   * resumed upon server restart.
   *
   * @return Whether the map pool is active
   */
  public boolean isActive();

  /**
   * Updates the map activity in the {@link Datastore} synchronously
   *
   * @param nextMap Name of next map (if rotation based).
   * @param active If this pool is currently active.
   */
  public void updateSync(@Nullable String nextMap, boolean active);

  /**
   * Updates the map activity in the {@link Datastore} asynchronously
   *
   * @param nextMap Name of next map (if rotation based).
   * @param active If this pool is currently active.
   */
  public void update(@Nullable String nextMap, boolean active);
}
