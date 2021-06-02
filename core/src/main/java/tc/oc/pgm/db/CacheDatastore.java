package tc.oc.pgm.db;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapVisibility;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.util.skin.Skin;

@SuppressWarnings({"UnstableApiUsage"})
public class CacheDatastore implements Datastore {

  private final Datastore datastore;
  private final LoadingCache<UUID, Username> usernames;
  private final LoadingCache<UUID, Settings> settings;
  private final LoadingCache<UUID, Skin> skins; // Skins are only stored in cache
  private final LoadingCache<String, MapActivity> activities;
  private final LoadingCache<MapInfo, MapVisibility> mapVisibility;

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
            .build(
                new CacheLoader<UUID, Settings>() {
                  @Override
                  public Settings load(UUID id) {
                    return datastore.getSettings(id);
                  }
                });
    this.skins =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, Skin>() {
                  @Override
                  public Skin load(UUID id) {
                    return datastore.getSkin(id);
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
    this.mapVisibility =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<MapInfo, MapVisibility>() {
                  @Override
                  public MapVisibility load(MapInfo map) throws Exception {
                    return datastore.getMapVisibility(map);
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
  public MapVisibility getMapVisibility(MapInfo map) {
    return mapVisibility.getUnchecked(map);
  }

  @Override
  public List<MapVisibility> getHiddenMaps() {
    return mapVisibility.asMap().values().stream()
        .filter(MapVisibility::isHidden)
        .collect(Collectors.toList());
  }

  @Override
  public void close() {
    datastore.close();

    usernames.invalidateAll();
    settings.invalidateAll();
    skins.invalidateAll();
    activities.invalidateAll();
    mapVisibility.invalidateAll();
  }
}
