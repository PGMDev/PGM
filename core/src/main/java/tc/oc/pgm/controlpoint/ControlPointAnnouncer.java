package tc.oc.pgm.controlpoint;

import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.controlpoint.events.ControllerChangeEvent;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.util.text.TextFormatter;

public class ControlPointAnnouncer implements Listener {
  private final Match match;

  public ControlPointAnnouncer(Match match) {
    this.match = match;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onOwnerChange(ControllerChangeEvent event) {
    if (event.getControlPoint().hasShowOption(ShowOption.SHOW_MESSAGES)) {

      if (event.getOldController() != null && event.getNewController() == null) {
        this.match.sendMessage(
            text()
                .append(event.getOldController().getName())
                .append(text(" lost ", NamedTextColor.GRAY))
                .append(text(event.getControlPoint().getName(), NamedTextColor.WHITE))
                .build());

      } else if (event.getNewController() != null) {
        this.match.sendMessage(
            text()
                .append(event.getNewController().getName())
                .append(text(" captured ", NamedTextColor.GRAY))
                .append(
                    text(
                        event.getControlPoint().getName(),
                        TextFormatter.convert(event.getNewController().getColor())))
                .build());
      }
    }
  }
}
