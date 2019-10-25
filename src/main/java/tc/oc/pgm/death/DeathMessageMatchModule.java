package tc.oc.pgm.death;

import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.MatchPlayerDeathEvent;
import tc.oc.pgm.match.*;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.module.ModuleLoadException;

@ModuleDescription(name = "Death Messages")
@ListenerScope(MatchScope.RUNNING)
public class DeathMessageMatchModule extends MatchModule implements Listener {

  public static class Factory implements MatchModuleFactory<DeathMessageMatchModule> {
    @Override
    public DeathMessageMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new DeathMessageMatchModule(match);
    }
  }

  public DeathMessageMatchModule(Match match) {
    super(match);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onVanillaDeath(final PlayerDeathEvent event) {
    event.setDeathMessage(null);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void handleDeathBroadcast(MatchPlayerDeathEvent event) {
    if (!event.getMatch().isRunning()) return;

    DeathMessageBuilder builder =
        new DeathMessageBuilder(event.getVictim(), event.getDamageInfo(), logger);
    Component message = new PersonalizedText(builder.getMessage(), ChatColor.GRAY);

    if (event.isPredicted()) {
      message.extra(
          new PersonalizedText(" "), new PersonalizedTranslatable("death.predictedSuffix"));
    }

    UUID victimId = event.getVictim().getPlayerId();
    UUID killerId = event.getKiller() == null ? null : event.getKiller().getPlayerId();

    for (MatchPlayer viewer : event.getMatch().getPlayers()) {
      if (event.isInvolved(viewer)) {
        viewer.sendMessage(new PersonalizedText(message, ChatColor.BOLD));
      } else {
        viewer.sendMessage(message);
      }
    }
  }
}
