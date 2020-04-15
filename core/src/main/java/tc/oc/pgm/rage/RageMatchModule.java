package tc.oc.pgm.rage;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.RUNNING)
public class RageMatchModule implements MatchModule, Listener {

  public RageMatchModule(Match match) {}

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void handlePlayerDamage(EntityDamageByEntityEvent event) {
    if (isRage(event.getDamager())) {
      event.setDamage(1000);
    }
  }

  private boolean isRage(Entity damager) {
    if (damager instanceof Player) {
      Player player = (Player) damager;
      return player.getItemInHand().containsEnchantment(Enchantment.DAMAGE_ALL);
    } else if (damager instanceof Arrow) {
      Arrow arrow = (Arrow) damager; // Arrows with damage > 2 are from power bows.
      return arrow.getShooter() instanceof Player && arrow.spigot().getDamage() > 2.0D;
    }
    return false;
  }
}
