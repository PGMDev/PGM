package tc.oc.pgm.db;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;
import java.util.UUID;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.Setting;

public class DatastoreCacheImpl implements Datastore {

  private final LoadingCache<UUID, Username> usernames;
  private final LoadingCache<UUID, Setting> settings;

  public DatastoreCacheImpl(Datastore datastore) {
    this.usernames =
        Caffeine.newBuilder()
            .weakValues()
            .maximumSize(1000)
            .refreshAfterWrite(Duration.ofHours(1))
            .expireAfterAccess(Duration.ofDays(1))
            .build(datastore::getUsername);
    this.settings =
        Caffeine.newBuilder()
            .weakValues()
            .maximumSize(Bukkit.getMaxPlayers())
            .refreshAfterWrite(Duration.ofMinutes(15))
            .expireAfterAccess(Duration.ofHours(1))
            .build(datastore::getSetting);
  }

  @Override
  public Username getUsername(UUID id) {
    return usernames.get(id);
  }

  @Override
  public Setting getSetting(UUID id) {
    return settings.get(id);
  }
}
