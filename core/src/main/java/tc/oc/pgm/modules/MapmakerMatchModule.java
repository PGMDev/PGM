package tc.oc.pgm.modules;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.entity.Player;
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
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

@ListenerScope(MatchScope.LOADED)
public class MapmakerMatchModule implements MatchModule, Listener {
  private final Collection<Contributor> authors;
  private final Match match;
  private final OnlinePlayerMapAdapter<PermissionAttachment> attachmentMap;

  public MapmakerMatchModule(Match match) {
    this.match = match;
    this.authors = match.getMap().getAuthors();
    this.attachmentMap = new OnlinePlayerMapAdapter<>(PGM.get());
  }

  @EventHandler(ignoreCancelled = true)
  public void onMatchPlayerAdd(final MatchPlayerAddEvent event) {
    Player player = event.getPlayer().getBukkit();
    UUID uuid = player.getUniqueId();

    if (authors.stream().anyMatch(c -> c.isPlayer(uuid))) {
      attachmentMap.put(player, player.addAttachment(PGM.get(), Permissions.MAPMAKER, true));
      match.callEvent(new NameDecorationChangeEvent(uuid)); // Refresh prefixes for the player
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onMatchPlayerLeave(final PlayerLeaveMatchEvent event) {
    Player player = event.getPlayer().getBukkit();
    PermissionAttachment attachment = attachmentMap.remove(player);
    if (attachment != null) {
      player.removeAttachment(attachment);
      match.callEvent(
          new NameDecorationChangeEvent(player.getUniqueId())); // Refresh prefixes for the player
    }
  }

  @Override
  public void unload() {
    attachmentMap.disable();
  }
}
