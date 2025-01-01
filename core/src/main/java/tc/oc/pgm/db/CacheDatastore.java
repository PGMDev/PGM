package tc.oc.pgm.db;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.map.MapData;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.util.skin.Skin;

@SuppressWarnings({"UnstableApiUsage"})
public class CacheDatastore implements Datastore {

  private final Datastore datastore;
  private final LoadingCache<UUID, Username> usernames;
  private final LoadingCache<UUID, Settings> settings;
  private final LoadingCache<UUID, Skin> skins; // Skins are only stored in cache
  private final LoadingCache<String, MapActivity> activities;

  public CacheDatastore(Datastore datastore) {
    this.datastore = datastore;
    this.usernames = CacheBuilder.newBuilder()
        .softValues()
        .build(new CacheLoader<UUID, Username>() {
          @Override
          public Username load(UUID id) {
            return datastore.getUsername(id);
          }
        });
    this.settings = CacheBuilder.newBuilder().build(new CacheLoader<UUID, Settings>() {
      @Override
      public Settings load(UUID id) {
        return datastore.getSettings(id);
      }
    });
    this.skins = CacheBuilder.newBuilder().build(new CacheLoader<UUID, Skin>() {
      @Override
      public Skin load(UUID id) {
        return datastore.getSkin(id);
      }
    });
    this.activities = CacheBuilder.newBuilder().build(new CacheLoader<String, MapActivity>() {
      @Override
      public MapActivity load(String name) {
        return datastore.getMapActivity(name);
      }
    });
  }

  @Override
  public Username getUsername(UUID id) {
    return usernames.getUnchecked(id);
  }

  @Override
  public Settings getSettings(UUID id) {
    return settings.getUnchecked(id);
  }

  @Override
  public void setSkin(UUID uuid, Skin skin) {
    skins.put(uuid, skin);
  }

  @Override
  public Skin getSkin(UUID id) {
    return skins.getUnchecked(id);
  }

  @Override
  public MapActivity getMapActivity(String poolName) {
    return activities.getUnchecked(poolName);
  }

  @Override
  public MapData getMapData(String mapId, double score) {
    return datastore.getMapData(mapId, score);
  }

  @Override
  public Map<String, ? extends MapData> getMapData(Collection<String> mapIds, double score) {
    return datastore.getMapData(mapIds, score);
  }

  @Override
  public void tickMapScores(VotingPool.VoteConstants constants) {
    datastore.tickMapScores(constants);
  }

  @Override
  public void refreshMapData() {
    datastore.refreshMapData();
  }

  @Override
  public void close() {
    datastore.close();

    usernames.invalidateAll();
    settings.invalidateAll();
    skins.invalidateAll();
    activities.invalidateAll();
  }
}
