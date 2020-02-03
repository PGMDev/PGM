package tc.oc.pgm.datastore;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.datastore.Datastore;
import tc.oc.pgm.api.datastore.OneTimePin;
import tc.oc.pgm.api.discord.DiscordId;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.Settings;

public class DatastoreCacheImpl implements Datastore {

  private final Datastore datastore;
  private final LoadingCache<UUID, Username> usernames;
  private final LoadingCache<UUID, Settings> settings;
  private final LoadingCache<UUID, DiscordId> discordIds;

  public DatastoreCacheImpl(Datastore datastore) {
    this.datastore = checkNotNull(datastore);
    this.usernames =
        buildCache(
            builder ->
                builder
                    .weakValues()
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
    this.discordIds =
        buildCache(
            builder ->
                builder
                    .weakValues()
                    .maximumSize(Bukkit.getMaxPlayers())
                    // .refreshAfterWrite(15, TimeUnit.MINUTES)
                    .expireAfterAccess(1, TimeUnit.HOURS),
            datastore::getDiscordId);
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
  public DiscordId getDiscordId(UUID id) {
    return discordIds.getUnchecked(id);
  }

  @Override
  public OneTimePin getOneTimePin(@Nullable UUID id, @Nullable String password) {
    return datastore.getOneTimePin(id, password); // No caching since already in-memory
  }
}
