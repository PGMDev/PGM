package tc.oc.pgm.db;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.UUID;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.Settings;

@SuppressWarnings({"UnstableApiUsage"})
public class CacheDatastore implements Datastore {

  private final Datastore datastore;
  private final LoadingCache<UUID, Username> usernames;
  private final LoadingCache<UUID, Settings> settings;
  private final LoadingCache<String, MapActivity> activities;

  public CacheDatastore(Datastore datastore) {
    this.datastore = datastore;
    this.usernames =
        CacheBuilder.newBuilder()
            .softValues()
            .build(
                new CacheLoader<UUID, Username>() {
                  @Override
                  public Username load(UUID id) {
                    return datastore.getUsername(id);
                  }
                });
    this.settings =
        CacheBuilder.newBuilder()
            .softValues()
            .maximumSize(Math.min(100, Bukkit.getMaxPlayers()))
            .build(
                new CacheLoader<UUID, Settings>() {
                  @Override
                  public Settings load(UUID id) {
                    return datastore.getSettings(id);
                  }
                });
    this.activities =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<String, MapActivity>() {
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
  public MapActivity getMapActivity(String poolName) {
    return activities.getUnchecked(poolName);
  }

  @Override
  public void close() {
    datastore.close();

    usernames.invalidateAll();
    settings.invalidateAll();
    activities.invalidateAll();
  }
}
