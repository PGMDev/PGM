package tc.oc.pgm.util.usernames;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;

public abstract class AbstractBatchingUsernameResolver extends AbstractUsernameResolver
    implements UsernameResolver {

  protected List<UUID> currentBatch = null;

  @Override
  public synchronized CompletableFuture<UsernameResponse> resolve(UUID uuid) {
    var response = futures.get(uuid);
    if (response == null) {
      var newFuture = response = createFuture(uuid);
      futures.put(uuid, newFuture);
      if (currentBatch != null) currentBatch.add(uuid);
      else getExecutor().execute(() -> process(uuid, newFuture));
    }
    return response;
  }

  @Override
  public synchronized void startBatch() {
    if (currentBatch == null) currentBatch = new ArrayList<>();
  }

  @Override
  public synchronized CompletableFuture<Void> endBatch() {
    List<UUID> batch = currentBatch;
    currentBatch = null;
    if (batch != null && !batch.isEmpty()) {
      Bukkit.getLogger().info(LOG_PREFIX + "Batch resolving " + batch.size() + " uuids");

      return CompletableFuture.runAsync(
          () -> {
            process(batch);
            Bukkit.getLogger().info(LOG_PREFIX + "Done resolving " + batch.size() + " uuids");
          },
          getExecutor());
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  protected abstract void process(List<UUID> uuids);
}
