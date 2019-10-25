package tc.oc.pgm.controlpoint;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.controlpoint.events.ControllerChangeEvent;
import tc.oc.pgm.match.Match;

public class ControlPointAnnouncer implements Listener {
  private final Match match;

  public ControlPointAnnouncer(Match match) {
    this.match = match;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onOwnerChange(ControllerChangeEvent event) {
    if (event.getControlPoint().isVisible()) {
      if (event.getOldController() != null && event.getNewController() == null) {
        this.match.sendMessage(
            event.getOldController().getColoredName()
                + ChatColor.GRAY
                + " lost "
                + ChatColor.WHITE
                + event.getControlPoint().getName());
      } else if (event.getNewController() != null) {
        this.match.sendMessage(
            event.getNewController().getColoredName()
                + ChatColor.GRAY
                + " captured "
                + event.getNewController().getColor()
                + event.getControlPoint().getName());
      }
    }
  }
}
