package tc.oc.pgm.rage;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.world.NMSHacks;

public class RageMatchModule implements MatchModule, Listener {

  public RageMatchModule(Match match) {}

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void handlePlayerDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player) {
      if (((Player) event.getDamager()).getItemInHand().containsEnchantment(Enchantment.DAMAGE_ALL))
        event.setDamage(1000);
    } else if (event.getDamager() instanceof Arrow
        && ((Arrow) event.getDamager()).getShooter() instanceof Player) {
      if (NMSHacks.hasPowerEnchanment(((Arrow) event.getDamager()))) event.setDamage(1000);
    }
  }
}
