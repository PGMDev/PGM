package tc.oc.pgm.util.usernames;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.nms.NMSHacks;

public final class BukkitUsernameResolver extends AbstractUsernameResolver {
  private static final SyncExecutor EXECUTOR = new SyncExecutor();
  private static final Plugin PGM = BukkitUtils.getPlugin();

  @Override
  protected Executor getExecutor() {
    return EXECUTOR;
  }

  @Override
  protected void process(UUID uuid, CompletableFuture<UsernameResponse> future) {
    future.complete(resolveUser(uuid));
  }

  private UsernameResponse resolveUser(UUID uuid) {
    OfflinePlayer pl = Bukkit.getOfflinePlayer(uuid);
    if (pl.getName() != null) {
      long lastPlayed = pl.getLastPlayed();
      return UsernameResponse.of(
          pl.getName(),
          lastPlayed > 0 ? Instant.ofEpochMilli(lastPlayed) : Instant.now(),
          BukkitUsernameResolver.class);
    }

    // Does vanilla user cache hold this player?
    String name = NMSHacks.getPlayerName(uuid);
    if (name != null) {
      return UsernameResponse.of(name, Instant.now(), BukkitUsernameResolver.class);
    }
    return UsernameResponse.empty();
  }

  private static class SyncExecutor implements Executor {

    @Override
    public void execute(@NotNull Runnable command) {
      if (Bukkit.isPrimaryThread()) command.run();
      else NMSHacks.postToMainThread(PGM, false, command);
    }
  }
}
