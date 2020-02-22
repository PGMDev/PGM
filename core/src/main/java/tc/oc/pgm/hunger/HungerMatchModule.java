package tc.oc.pgm.hunger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.RUNNING)
public class HungerMatchModule implements MatchModule, Listener {

  public HungerMatchModule(Match match) {}

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void handleHungerChange(final FoodLevelChangeEvent event) {
    if (event.getEntity() instanceof Player) {
      int oldFoodLevel = ((Player) event.getEntity()).getFoodLevel();
      event.setFoodLevel(oldFoodLevel);
    }
  }
}
