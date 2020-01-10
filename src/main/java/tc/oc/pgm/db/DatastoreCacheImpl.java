package tc.oc.pgm.db;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.Settings;

public class DatastoreCacheImpl implements Datastore {

  private final LoadingCache<UUID, Username> usernames;
  private final LoadingCache<UUID, Settings> settings;

  public DatastoreCacheImpl(Datastore datastore) {
    this.usernames =
        buildCache(
            builder ->
                builder
                    .weakValues()
                    .maximumSize(1000)
                    // .refreshAfterWrite(1, TimeUnit.HOURS)
                    .expireAfterAccess(1, TimeUnit.DAYS),
            datastore::getUsername);
    this.settings =
        buildCache(
            builder ->
                builder
                    .weakValues()
                    .maximumSize(Bukkit.getMaxPlayers())
                    // .refreshAfterWrite(15, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.HOURS),
            datastore::getSettings);
  }

  // FIXME: Potential deadlock as a result of async loading, removed temporarily
  private <K, V> LoadingCache<K, V> buildCache(
      Function<CacheBuilder, CacheBuilder> builder, Function<K, V> function) {
    return builder
        .apply(CacheBuilder.newBuilder())
        .build(
            new CacheLoader<K, V>() {
              @Override
              public V load(K key) {
                return function.apply(key);
              }

              /*@Override
              public ListenableFuture<V> reload(K key, V old) {
                return ListenableFutureTask.create(() -> function.apply(key));
              }*/
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
  public void shutdown() {
    usernames.invalidateAll();
    settings.invalidateAll();
  }
}
