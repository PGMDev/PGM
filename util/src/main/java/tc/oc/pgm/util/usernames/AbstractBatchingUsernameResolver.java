package tc.oc.pgm.util.usernames;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;

public abstract class AbstractBatchingUsernameResolver extends AbstractUsernameResolver
    implements UsernameResolver {

  private final String prefix = "[" + getClass().getSimpleName() + "] ";
  protected List<UUID> currentBatch = null;

  @Override
  public synchronized CompletableFuture<UsernameResponse> resolve(UUID uuid) {
    CompletableFuture<UsernameResponse> response =
        futureCache.computeIfAbsent(
            uuid,
            key -> {
              if (currentBatch != null) currentBatch.add(uuid);
              return createFuture(uuid);
            });

    if (currentBatch == null) getExecutor().execute(() -> process(uuid, response));

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
      Bukkit.getLogger().info(prefix + "Batch resolving " + batch.size() + " uuids");

      return CompletableFuture.runAsync(
          () -> {
            process(batch);
            Bukkit.getLogger().info(prefix + "Done resolving " + batch.size() + " uuids");
          },
          getExecutor());
    } else {
      return CompletableFuture.completedFuture(null);
    }
  }

  protected abstract void process(List<UUID> uuids);
}
