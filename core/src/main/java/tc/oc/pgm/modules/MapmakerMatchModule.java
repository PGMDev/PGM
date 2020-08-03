package tc.oc.pgm.modules;

import java.util.Collection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;

@ListenerScope(MatchScope.LOADED)
public class MapmakerMatchModule implements MatchModule, Listener {
  private final Collection<Contributor> authors;

  public MapmakerMatchModule(Match match) {
    this.authors = match.getMap().getAuthors();
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerJoinMatchEvent(final PlayerJoinMatchEvent event) {
    MatchPlayer player = event.getPlayer();
    if (authors.stream().anyMatch(c -> c.isPlayer(player.getId()))) {
      player.getBukkit().addAttachment(PGM.get(), Permissions.MAPMAKER, true);
    } else {
      player.getBukkit().removeAttachments(Permissions.MAPMAKER);
    }
  }
}
