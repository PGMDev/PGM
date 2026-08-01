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

  private final boolean preventDepletion;
  private final boolean preventReplenishment;

  public HungerMatchModule(Match match, boolean depletion, boolean replenishment) {
    this.preventDepletion = !depletion;
    this.preventReplenishment = !replenishment;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void handleHungerChange(final FoodLevelChangeEvent event) {
    if (!(event.getEntity() instanceof Player p)) return;
    int oldFoodLevel = p.getFoodLevel();
    if (event.getFoodLevel() < oldFoodLevel ? preventDepletion : preventReplenishment) {
      event.setFoodLevel(oldFoodLevel);
    }
  }
}
