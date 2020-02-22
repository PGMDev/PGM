package tc.oc.pgm.match;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.ComponentUtils;
import tc.oc.util.bukkit.component.types.PersonalizedText;
import tc.oc.util.bukkit.identity.Identity;
import tc.oc.util.bukkit.named.NameType;
import tc.oc.util.bukkit.named.Names;
import tc.oc.util.bukkit.named.NicknameRenderer;

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

  @Override
  public String getLegacyName(Identity identity, NameType type) {
    String rendered = super.getLegacyName(identity, type);

    if (type.style.showPrefix && type.online && type.reveal) {
      final String prefix = PGM.get().getPrefixRegistry().getPrefix(identity.getPlayerId());
      if (prefix != null) {
        rendered = prefix + rendered;
      }
    }

    return rendered;
  }

  @Override
  public Component getComponentName(Identity identity, NameType type) {
    Component rendered = super.getComponentName(identity, type);

    if (type.style.showPrefix && type.online && type.reveal) {
      final String prefix = PGM.get().getPrefixRegistry().getPrefix(identity.getPlayerId());
      if (prefix != null) {
        rendered = new PersonalizedText(new PersonalizedText(prefix), rendered);
      }
    }

    return rendered;
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
