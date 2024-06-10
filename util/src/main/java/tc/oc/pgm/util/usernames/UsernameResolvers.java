package tc.oc.pgm.util.usernames;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public interface UsernameResolvers {

  AtomicReference<UsernameResolver> INSTANCE =
      new AtomicReference<>(of(new BukkitUsernameResolver(), new ApiUsernameResolver()));

  static UsernameResolver get() {
    return INSTANCE.get();
  }

  static UsernameResolver of(UsernameResolver... resolvers) {
    if (resolvers == null || resolvers.length == 0) return null;
    if (resolvers.length == 1) return resolvers[0];
    return new ChainedUsernameResolver(resolvers);
  }

  static void setResolvers(UsernameResolver... resolvers) {
    INSTANCE.set(Objects.requireNonNull(of(resolvers)));
  }

  static CompletableFuture<UsernameResolver.UsernameResponse> resolve(UUID uuid) {
    return INSTANCE.get().resolve(uuid);
  }

  static void startBatch() {
    INSTANCE.get().startBatch();
  }

  static void endBatch() {
    INSTANCE.get().endBatch();
  }
}
