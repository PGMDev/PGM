package tc.oc.pgm.api.map;

import javax.annotation.Nullable;

/* Represents data related to persisting map pool info between server restarts */
public interface PoolActivity {

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
  public String getNextMap();

  /**
   * Whether this map pool was the last active. If the map pool was the last active, it will be
   * resumed upon server restart.
   *
   * @return Whether the map pool was last active
   */
  public boolean isLastActive();

  /**
   * Updates the map pool activity in the {@link Datastore}
   *
   * @param nextMap Name of next map if rotation based.
   * @param active If this pool is the currently active one
   */
  public void updatePool(@Nullable String nextMap, boolean active);
}
