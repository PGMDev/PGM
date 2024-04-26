package tc.oc.pgm.util.usernames;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface UsernameResolver {

  CompletableFuture<UsernameResponse> resolve(UUID uuid);

  void startBatch();

  CompletableFuture<Void> endBatch();

  final class UsernameResponse {
    private static final UsernameResponse NO_NAME =
        new UsernameResponse(null, Instant.EPOCH, Instant.EPOCH, UsernameResponse.class);

    private final @Nullable String username;
    private final @Nullable Instant validAt;
    private final @NotNull Instant validUntil;
    private final @NotNull Class<?> source;

    private UsernameResponse(
        @Nullable String username,
        @Nullable Instant validAt,
        @NotNull Instant validUntil,
        @NotNull Class<?> source) {
      this.username = username;
      this.validAt = validAt;
      this.validUntil = validUntil;
      this.source = source;
    }

    public @Nullable String getUsername() {
      return username;
    }

    public @Nullable Instant getValidAt() {
      return validAt;
    }

    public @NotNull Instant getValidUntil() {
      return validUntil;
    }

    public @NotNull Class<?> getSource() {
      return source;
    }

    public boolean isAcceptable() {
      return username != null && validUntil.isAfter(Instant.now());
    }

    public static UsernameResponse empty() {
      return NO_NAME;
    }

    public static UsernameResponse of(@Nullable String name, @NotNull Class<?> source) {
      return UsernameResponse.of(name, Instant.now(), source);
    }

    public static UsernameResponse of(
        @Nullable String name, @NotNull Instant validAt, @NotNull Class<?> source) {
      // Assume if user was valid at time N, it will be valid until random 1 to 2 weeks after.
      return UsernameResponse.of(
          name, validAt, validAt.plus(7 + (long) (7 * Math.random()), ChronoUnit.DAYS), source);
    }

    public static UsernameResponse of(
        @Nullable String name,
        @Nullable Instant validAt,
        @NotNull Instant validUntil,
        @NotNull Class<?> source) {
      if (name == null) return NO_NAME;
      return new UsernameResponse(name, validAt, validUntil, source);
    }
  }
}
