package tc.oc.pgm.modules;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchScope;
import tc.oc.world.NMSHacks;

@ListenerScope(MatchScope.RUNNING)
public class FriendlyFireRefundMatchModule extends MatchModule implements Listener {

  public FriendlyFireRefundMatchModule(Match match) {
    super(match);
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void handleFriendlyFire(EntityDamageByEntityEvent event) {
    if (event.isCancelled() && event.getDamager() instanceof Arrow) {
      Arrow arrow = (Arrow) event.getDamager();
      if (!NMSHacks.hasInfinityEnchanment(arrow)
          && arrow.getShooter() != null
          && arrow.getShooter() instanceof Player) {
        Player owner = (Player) arrow.getShooter();
        owner.getInventory().addItem(new ItemStack(Material.ARROW));
        arrow.remove();
      }
    }
  }
}
