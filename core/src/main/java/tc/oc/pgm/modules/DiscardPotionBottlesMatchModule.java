package tc.oc.pgm.modules;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.RUNNING)
public class DiscardPotionBottlesMatchModule implements MatchModule, Listener {

  public DiscardPotionBottlesMatchModule(Match match) {}

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onDrinkPotion(final PlayerItemConsumeEvent event) {
    if (event.getItem().getType() == Material.POTION) {
      event.setReplacement(new ItemStack(Material.AIR));
    }
  }
}
