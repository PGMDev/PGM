package tc.oc.pgm.util.usernames;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public abstract class AbstractUsernameResolver implements UsernameResolver {

  protected final String LOG_PREFIX = "[" + getClass().getSimpleName() + "] ";
  protected final Map<UUID, CompletableFuture<UsernameResponse>> futures =
      new ConcurrentHashMap<>();

  protected Executor getExecutor() {
    return ForkJoinPool.commonPool();
  }

  @Override
  public synchronized CompletableFuture<UsernameResponse> resolve(UUID uuid) {
    CompletableFuture<UsernameResponse> response =
        futures.computeIfAbsent(uuid, this::createFuture);
    getExecutor().execute(() -> process(uuid, response));
    return response;
  }

  @Override
  public void startBatch() {}

  @Override
  public synchronized CompletableFuture<Void> endBatch() {
    return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]));
  }

  protected CompletableFuture<UsernameResponse> createFuture(UUID uuid) {
    CompletableFuture<UsernameResponse> future = new CompletableFuture<>();
    future.whenComplete((o, t) -> futures.remove(uuid));
    return future;
  }

  protected void complete(UUID uuid, UsernameResponse response) {
    CompletableFuture<UsernameResponse> future = futures.get(uuid);
    if (future != null) future.complete(response);
  }

  protected abstract void process(UUID uuid, CompletableFuture<UsernameResponse> future);
}
