package tc.oc.pgm.listeners;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.util.text.TextTranslations;

public class InitialMatchLoader implements Listener {

  private final MatchManager mm;
  private final ReentrantLock lock;

  public InitialMatchLoader(Plugin plugin, MatchManager mm) {
    this.mm = mm;
    this.lock = new ReentrantLock();
    Bukkit.getScheduler().runTaskAsynchronously(plugin, this::tryCreateMatch);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPrePlayerLogin(final AsyncPlayerPreLoginEvent event) {
    if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
    if (mm.getMatches().hasNext()) {
      HandlerList.unregisterAll(this);
      return;
    }

    tryCreateMatch();

    if (!mm.getMatches().hasNext()) {
      event.disallow(
          AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
          TextTranslations.translate("misc.incorrectWorld"));
    }
  }

  private void tryCreateMatch() {
    try {
      lock.lock();
      // Already created
      if (mm.getMatches().hasNext()) return;

      // If the server is suspended, need to release so match can be created
      NMS_HACKS.resumeServer();
      try {
        mm.createMatch(null).get();
        if (mm.getMatches().hasNext()) HandlerList.unregisterAll(this);
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    } finally {
      lock.unlock();
    }
  }
}
