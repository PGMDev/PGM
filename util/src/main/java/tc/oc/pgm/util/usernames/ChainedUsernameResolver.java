package tc.oc.pgm.util.usernames;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ChainedUsernameResolver implements UsernameResolver {

  private final UsernameResolver[] resolvers;

  public ChainedUsernameResolver(UsernameResolver[] resolvers) {
    this.resolvers = resolvers;
  }

  @Override
  public CompletableFuture<UsernameResolver.UsernameResponse> resolve(UUID uuid) {
    CompletableFuture<UsernameResolver.UsernameResponse> future = resolvers[0].resolve(uuid);

    // Future completed sync (or very fast), avoid composing & return sync if we have a good answer
    if (future.isDone() && future.getNow(UsernameResponse.empty()).isAcceptable()) {
      return future;
    }

    for (int i = 1; i < resolvers.length; i++) {
      UsernameResolver nextResolver = resolvers[i];
      future =
          future.thenCompose(
              first -> {
                if (first.isAcceptable()) return CompletableFuture.completedFuture(first);
                return nextResolver.resolve(uuid).thenApply(second -> combine(first, second));
              });
    }
    return future;
  }

  @Override
  public void startBatch() {
    for (UsernameResolver resolver : resolvers) {
      resolver.startBatch();
    }
  }

  @Override
  public CompletableFuture<Void> endBatch() {
    CompletableFuture<Void> future = resolvers[0].endBatch();
    for (int i = 1; i < resolvers.length; i++) {
      UsernameResolver resolver = resolvers[i];
      future =
          future
              .thenRunAsync(
                  () -> {
                    // Delay ensures composed futures had time to queue for next resolver
                    try {
                      Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                  })
              .thenCompose(ignore -> resolver.endBatch());
    }
    return future;
  }

  private static UsernameResponse combine(UsernameResponse first, UsernameResponse second) {
    // Operate under the assumption that first isn't acceptable; otherwise second wouldn't have been
    // requested
    if (second.isAcceptable()) return second;

    // Does only one have a name?
    boolean firstHasName = first.getUsername() != null;
    boolean secondHasName = second.getUsername() != null;
    if (firstHasName != secondHasName) return firstHasName ? first : second;
    // Both are bad, no name in either.
    if (!firstHasName) return second;

    // Prefer comparison by validAt if available, this is a not-random value
    if (first.getValidAt() != null && second.getValidAt() != null) {
      return first.getValidAt().isAfter(second.getValidAt()) ? first : second;
    }
    // If valid at is unknown, use validUntil which has a bit of randomness, but we can take it
    return first.getValidUntil().isAfter(second.getValidUntil()) ? first : second;
  }
}
