package tc.oc.pgm.util.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class EventUtil {

  public static void handleCall(Event pgmEvent, Event bukkitEvent) {
    if (pgmEvent == null) return;
    if (bukkitEvent instanceof Cancellable bCancellable
        && pgmEvent instanceof Cancellable pgmCancellable) {
      pgmCancellable.setCancelled(bCancellable.isCancelled());
      Bukkit.getServer().getPluginManager().callEvent(pgmEvent);
      bCancellable.setCancelled(pgmCancellable.isCancelled());
    } else {
      Bukkit.getServer().getPluginManager().callEvent(pgmEvent);
    }
  }
}
