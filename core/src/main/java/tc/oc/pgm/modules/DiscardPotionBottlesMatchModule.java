package tc.oc.pgm.modules;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.bukkit.MiscUtils;

@ListenerScope(MatchScope.RUNNING)
public class DiscardPotionBottlesMatchModule implements MatchModule, Listener {

  private final Match match;

  public DiscardPotionBottlesMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onDrinkPotion(final PlayerItemConsumeEvent event) {
    if (event.getItem().getType() == Material.POTION) {
      MiscUtils.INSTANCE.removeDrankPotion(event, match.getExecutor(MatchScope.LOADED));
    }
  }
}
