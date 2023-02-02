package tc.oc.pgm.death;

import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.RUNNING)
public class DeathMessageMatchModule implements MatchModule, Listener {

  private final Logger logger;

  public DeathMessageMatchModule(Match match) {
    this.logger = match.getLogger();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onVanillaDeath(final PlayerDeathEvent event) {
    event.setDeathMessage(null);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void handleDeathBroadcast(MatchPlayerDeathEvent event) {
    if (!event.getMatch().isRunning()) return;

    DeathMessageBuilder builder = new DeathMessageBuilder(event, logger);
    Component message = builder.getMessage().color(NamedTextColor.GRAY);

    for (MatchPlayer viewer : event.getMatch().getPlayers()) {
      switch (viewer.getSettings().getValue(SettingKey.DEATH)) {
        case DEATH_OWN:
          if (event.isInvolved(viewer) || event.isInvolved(viewer.getSpectatorTarget())) {
            viewer.sendMessage(message);
          } else if (event.isTeamKill() && viewer.getBukkit().hasPermission(Permissions.STAFF)) {
            viewer.sendMessage(message.decoration(TextDecoration.ITALIC, true));
          }
          break;
        case DEATH_FRIENDS:
          if (event.isInvolved(viewer)) {
            viewer.sendMessage(message.decoration(TextDecoration.BOLD, true));
          } else if (isFriendInvolved(viewer.getBukkit(), event)) {
            viewer.sendMessage(message);
          } else if (event.isTeamKill() && viewer.getBukkit().hasPermission(Permissions.STAFF)) {
            viewer.sendMessage(message.decoration(TextDecoration.ITALIC, true));
          }
          break;
        case DEATH_ALL:
          if (event.isInvolved(viewer) || event.isInvolved(viewer.getSpectatorTarget())) {
            viewer.sendMessage(message.decoration(TextDecoration.BOLD, true));
          } else {
            viewer.sendMessage(message);
          }
          break;
      }
    }
  }

  private boolean isFriendInvolved(Player viewer, MatchPlayerDeathEvent event) {
    Player killer =
        event.getKiller() != null && event.getKiller().getPlayer().isPresent()
            ? event.getKiller().getPlayer().get().getBukkit()
            : null;
    Player victim = event.getVictim().getBukkit();

    return (killer != null && Integration.isFriend(viewer, killer))
        || (victim != null && Integration.isFriend(viewer, victim));
  }
}
