package tc.oc.pgm.modules;

import java.util.concurrent.TimeUnit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.bukkit.BukkitUtils;

@ListenerScope(MatchScope.RUNNING)
public class DiscardPotionBottlesMatchModule implements MatchModule, Listener {

  Match match;

  public DiscardPotionBottlesMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onDrinkPotion(final PlayerItemConsumeEvent event) {
    if (event.getItem().getType() == Material.POTION) {
      if (BukkitUtils.isSportPaper()) {
        // This method does not exist in spigot
        event.setReplacement(new ItemStack(Material.AIR));
      } else {
        int itemSlot = event.getPlayer().getInventory().getHeldItemSlot();
        Player player = event.getPlayer();

        // Setting the event item should work, but Spigot doesn't have proper error checking for
        // this so if you set the event item to air it throws a null pointer
        match
            .getExecutor(MatchScope.LOADED)
            .schedule(
                () -> {
                  if (player.getInventory().getItem(itemSlot).getType() == Material.GLASS_BOTTLE) {
                    player.getInventory().setItem(itemSlot, new ItemStack(Material.AIR));
                  }
                },
                0,
                TimeUnit.MILLISECONDS);
      }
    }
  }
}
