package tc.oc.pgm.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.map.MapData;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.util.skin.Skin;

/** A fast, persistent datastore that provides synchronous responses. */
public interface Datastore {

  /**
   * Get the username for a given player {@link UUID}.
   *
   * @param uuid The {@link UUID} of a player.
   * @return A {@link Username}.
   */
  Username getUsername(UUID uuid);

  /**
   * Get the settings for a given player {@link UUID}.
   *
   * @param uuid The {@link UUID} of a player.
   * @return A {@link Settings}.
   */
  Settings getSettings(UUID uuid);

  /**
   * Set the skin for a given player {@link UUID}.
   *
   * @param uuid the {@link UUID} of a player
   * @param skin the {@link Skin} of the player
   */
  void setSkin(UUID uuid, Skin skin);

  /**
   * Get the skin for a given player {@link UUID}.
   *
   * @param uuid The {@link UUID} of a player
   * @return A {@link Skin}
   */
  Skin getSkin(UUID uuid);

  /**
   * Get the activity related to a defined map pool.
   *
   * @param poolName The name of a defined map pool.
   * @return A {@link MapActivity}.
   */
  MapActivity getMapActivity(String poolName);

  /**
   * Get the data related to a map
   *
   * @param mapId the map id or slug
   * @param score default score if no map data exists
   * @return A {@link MapData}
   */
  MapData getMapData(String mapId, double score);

  /**
   * Get the data related to several maps in bulk
   *
   * @param mapIds The map ids wanted
   * @param score default score if no map data exists
   * @return A map containing at least all maps in {@param mapIds}, potentially more
   */
  Map<String, ? extends MapData> getMapData(Collection<String> mapIds, double score);

  void tickMapScores(VotingPool.VoteConstants constants);

  void refreshMapData();

  /** Cleans up any resources or connections. */
  void close();
}
