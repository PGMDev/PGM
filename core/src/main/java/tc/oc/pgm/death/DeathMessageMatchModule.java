package tc.oc.pgm.death;

import java.util.function.BiPredicate;
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
import tc.oc.pgm.util.Audience;

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

    Audience.console().sendMessage(message);

    for (MatchPlayer viewer : event.getMatch().getPlayers()) {
      boolean involved = event.isInvolved(viewer) || event.isInvolved(viewer.getSpectatorTarget());
      boolean isStaff = event.isTeamKill() && viewer.getBukkit().hasPermission(Permissions.STAFF);
      boolean show = involved
          || isStaff
          || switch (viewer.getSettings().getValue(SettingKey.DEATH)) {
            case DEATH_OWN -> false;
            case DEATH_FRIENDS -> isFriendInvolved(viewer.getBukkit(), event);
            case DEATH_SQUAD -> isFriendInvolved(viewer.getBukkit(), event)
                || isSquadInvolved(viewer.getBukkit(), event);
            case DEATH_ALL -> true;
            default -> false;
          };

      if (show) {
        var messageToSend = message;
        if (involved) messageToSend = message.decoration(TextDecoration.BOLD, true);
        else if (isStaff) messageToSend = message.decoration(TextDecoration.ITALIC, true);
        viewer.sendMessage(messageToSend);
      }
    }
  }

  private boolean isSquadInvolved(Player viewer, MatchPlayerDeathEvent event) {
    return isPlayerInvolved(viewer, event, Integration::areInSquad);
  }

  private boolean isFriendInvolved(Player viewer, MatchPlayerDeathEvent event) {
    return isPlayerInvolved(viewer, event, Integration::isFriend);
  }

  private boolean isPlayerInvolved(
      Player viewer, MatchPlayerDeathEvent event, BiPredicate<Player, Player> relationCheck) {
    Player killer = event.getKiller() != null && event.getKiller().getPlayer().isPresent()
        ? event.getKiller().getPlayer().get().getBukkit()
        : null;
    Player victim = event.getVictim().getBukkit();

    return (killer != null && relationCheck.test(viewer, killer))
        || (victim != null && relationCheck.test(viewer, victim));
  }
}
