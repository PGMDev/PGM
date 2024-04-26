package tc.oc.pgm.util.usernames;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public abstract class AbstractUsernameResolver implements UsernameResolver {

  protected final Map<UUID, CompletableFuture<UsernameResponse>> futureCache =
      new ConcurrentHashMap<>();

  protected Executor getExecutor() {
    return ForkJoinPool.commonPool();
  }

  @Override
  public synchronized CompletableFuture<UsernameResponse> resolve(UUID uuid) {
    CompletableFuture<UsernameResponse> response =
        futureCache.computeIfAbsent(uuid, this::createFuture);
    getExecutor().execute(() -> process(uuid, response));
    return response;
  }

  @Override
  public void startBatch() {}

  @Override
  public synchronized CompletableFuture<Void> endBatch() {
    return CompletableFuture.allOf(futureCache.values().toArray(new CompletableFuture[0]));
  }

  protected CompletableFuture<UsernameResponse> createFuture(UUID uuid) {
    return new CompletableFuture<UsernameResponse>()
        .whenComplete((o, t) -> futureCache.remove(uuid));
  }

  protected CompletableFuture<UsernameResponse> getFuture(UUID uuid) {
    return futureCache.computeIfAbsent(uuid, this::createFuture);
  }

  protected abstract void process(UUID uuid, CompletableFuture<UsernameResponse> future);
}
