package tc.oc.pgm.controlpoint;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.controlpoint.events.ControllerChangeEvent;
import tc.oc.pgm.util.text.TextFormatter;

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
            Component.text()
                .append(event.getOldController().getName())
                .append(Component.text(" lost ", NamedTextColor.GRAY))
                .append(Component.text(event.getControlPoint().getName(), NamedTextColor.WHITE))
                .build());

      } else if (event.getNewController() != null) {
        this.match.sendMessage(
            Component.text()
                .append(event.getNewController().getName())
                .append(Component.text(" captured ", NamedTextColor.GRAY))
                .append(
                    Component.text(
                        event.getControlPoint().getName(),
                        TextFormatter.convert(event.getNewController().getColor())))
                .build());
      }
    }
  }
}
