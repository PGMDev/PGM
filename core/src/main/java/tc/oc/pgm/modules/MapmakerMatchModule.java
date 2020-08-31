package tc.oc.pgm.modules;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachment;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.LOADED)
public class MapmakerMatchModule implements MatchModule, Listener {
  private final Collection<Contributor> authors;
  private final Match match;
  private final Map<UUID, PermissionAttachment> attachmentMap = new HashMap<>();

  public MapmakerMatchModule(Match match) {
    this.match = match;
    this.authors = match.getMap().getAuthors();
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerJoinMatchEvent(final MatchPlayerAddEvent event) {
    MatchPlayer player = event.getPlayer();
    PermissionAttachment attachment =
        player.getBukkit().addAttachment(PGM.get(), Permissions.MAPMAKER, true);
    if (authors.stream().noneMatch(c -> c.isPlayer(player.getId()))) {
      attachment.remove();
    } else {
      attachmentMap.put(player.getId(), attachment);
    }

    event
        .getMatch()
        .callEvent(
            new NameDecorationChangeEvent(player.getId())); // Refresh prefixes for the player
  }

  @Override
  public void unload() {
    for (Map.Entry<UUID, PermissionAttachment> entry : attachmentMap.entrySet()) {
      entry.getValue().remove();
      match.callEvent(new NameDecorationChangeEvent(entry.getKey()));
    }
  }
}
