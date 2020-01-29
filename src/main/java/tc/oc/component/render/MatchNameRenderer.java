package tc.oc.component.render;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.identity.Identity;
import tc.oc.named.NameType;
import tc.oc.named.Names;
import tc.oc.named.NicknameRenderer;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.util.components.ComponentUtils;

/** Adds team colors to player names */
public class MatchNameRenderer extends NicknameRenderer implements Listener {

  protected final MatchManager matchManager;

  public MatchNameRenderer(MatchManager matchManager) {
    this.matchManager = matchManager;
  }

  @Override
  public ChatColor getColor(Identity identity, NameType type) {
    if (type.online && !(type.dead && type.style.showDeath)) {
      MatchPlayer player = matchManager.getPlayer(identity.getPlayerId());
      if (player != null) {
        return ComponentUtils.convert(player.getParty().getColor());
      }
    }

    return super.getColor(identity, type);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    final Player player = event.getPlayer().getBukkit();
    Names.invalidate(player);
    final Party party = event.getNewParty();
    if (party != null) {
      player.setDisplayName(event.getPlayer().getPrefixedName());
    }
  }
}
