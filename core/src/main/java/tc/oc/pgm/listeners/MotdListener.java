package tc.oc.pgm.listeners;

import java.text.MessageFormat;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchPhaseChangeEvent;

public class MotdListener implements Listener {

  private ChatColor phaseColor;
  private String mapName;

  public MotdListener() {
    this.phaseColor = ChatColor.GRAY;

    MapInfo map = PGM.get().getMapOrder().getNextMap();
    if (map == null) {
      map = PGM.get().getMapLibrary().getMaps().next();
    }

    this.mapName = map.getName();
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onServerListPing(ServerListPingEvent event) {
    final String format = PGM.get().getConfiguration().getMotd();
    if (format == null || format.isEmpty()) return;

    event.setMotd(MessageFormat.format(format, event.getMotd(), mapName, phaseColor.toString()));
  }

  @EventHandler
  public void onLoad(MatchLoadEvent event) {
    phaseColor = getPhaseColor(event.getMatch().getPhase());
    mapName = event.getMatch().getMap().getName();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onStateChange(MatchPhaseChangeEvent event) {
    phaseColor = getPhaseColor(event.getNewPhase());
  }

  private ChatColor getPhaseColor(MatchPhase phase) {
    switch (phase) {
      case STARTING:
        return ChatColor.YELLOW;
      case RUNNING:
        return ChatColor.GREEN;
      case FINISHED:
        return ChatColor.RED;
      default:
        return ChatColor.GRAY;
    }
  }
}
