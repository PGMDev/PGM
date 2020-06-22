package tc.oc.pgm.rage;

import java.util.List;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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

  private final boolean allProjectiles;
  private final List<EntityType> entities;

  public RageMatchModule(Match match, boolean allProjectiles, List<EntityType> entities) {
    this.allProjectiles = allProjectiles;
    this.entities = entities;
  }

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
    } else if (damager instanceof Projectile) {
      if (!(((Projectile) damager).getShooter() instanceof Player))
        return false; // Block mobs from being instant kill - preserves old behavior.
      return entities.contains(damager.getType()) || allProjectiles;
    }
    return false;
  }
}
