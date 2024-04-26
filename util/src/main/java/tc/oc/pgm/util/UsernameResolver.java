package tc.oc.pgm.util;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.usernames.UsernameResolver.UsernameResponse;
import tc.oc.pgm.util.usernames.UsernameResolvers;

/**
 * Utility to resolve Minecraft usernames from an external API.
 *
 * @link https://github.com/Electroid/mojang-api
 * @deprecated See {@link tc.oc.pgm.util.usernames.UsernameResolvers} instead
 */
@Deprecated
public final class UsernameResolver {
  private UsernameResolver() {}

  /**
   * Queue all remaining username resolves on an asynchronous thread.
   *
   * @see #resolve(UUID, Consumer)
   */
  @Deprecated
  public static void resolveAll() {}

  /**
   * Queue a username resolve with an asynchronous callback.
   *
   * @param id A {@link UUID} to resolve.
   * @param callback A callback to run after the username is resolved.
   */
  @Deprecated
  public static void resolve(UUID id, @Nullable Consumer<String> callback) {
    CompletableFuture<UsernameResponse> future = UsernameResolvers.resolve(id);
    if (callback != null) future.thenAccept(ur -> callback.accept(ur.getUsername()));
  }
}
