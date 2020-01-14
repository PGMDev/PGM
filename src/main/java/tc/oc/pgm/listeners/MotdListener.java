package tc.oc.pgm.listeners;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchPhaseChangeEvent;
import tc.oc.pgm.events.ConfigLoadEvent;

public class MotdListener implements Listener {

  private static final String MAP_NAME_KEY = "map.name";
  private static final String MAP_VERSION_KEY = "map.version";
  private static final String STATE_COLOR_KEY = "state.color";
  private static final String STATE_NAME_KEY = "state.name";
  private static final String STATE_NAME_LOWER_KEY = "state.name-lower";

  private static final Map<String, String> MOTD_DATA = Maps.newHashMap();
  private String format;

  // Show the default MOTD until a match has loaded
  private boolean ready = false;

  public MotdListener() {
    this.format = Config.Motd.format();
  }

  @EventHandler
  public void onServerListPing(ServerListPingEvent event) {
    if (!Config.Motd.enabled() || !ready) return;

    String motd = format;
    // There's no nice named string placeholder system built directly into Java :(.
    for (Entry<String, String> entry : MOTD_DATA.entrySet()) {
      String find = entry.getKey();
      String replace = entry.getValue();
      motd = motd.replace("{" + find + "}", replace);
    }
    event.setMotd(motd);
  }

  @EventHandler
  public void onConfigReload(ConfigLoadEvent event) {
    this.format = Config.Motd.format();
  }

  @EventHandler
  public void onLoad(MatchLoadEvent event) {
    MOTD_DATA.put(MAP_NAME_KEY, event.getMatch().getMap().getName());
    MOTD_DATA.put(MAP_VERSION_KEY, event.getMatch().getMap().getVersion().toString());
    ready = true;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onStateChange(MatchPhaseChangeEvent event) {
    String name = event.getNewPhase().name().toLowerCase();
    ChatColor color = ChatColor.WHITE;
    switch (event.getNewPhase()) {
      case IDLE:
        color = Config.Motd.Colors.idle();
        break;
      case STARTING:
        color = Config.Motd.Colors.starting();
        break;
      case RUNNING:
        color = Config.Motd.Colors.running();
        break;
      case FINISHED:
        color = Config.Motd.Colors.finished();
        break;
    }
    MOTD_DATA.put(STATE_NAME_KEY, StringUtils.capitalize(name));
    MOTD_DATA.put(STATE_NAME_LOWER_KEY, name);
    MOTD_DATA.put(STATE_COLOR_KEY, color.toString());
  }
}
