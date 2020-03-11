package tc.oc.pgm.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.RUNNING)
public class CoronavirusMatchModule implements MatchModule, Listener {

  public CoronavirusMatchModule(Match match) {}

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onDamagePlayer(final EntityDamageByEntityEvent event) {
    Entity entity = event.getEntity();

        if(entity instanceof Player) {
            if(!Damage.contains(entity))
                return;
            } else {
                if(event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK); {
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0, false));
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0, false));
                    entity.getLocation().getWorld().playSound(enemy.getLocation(), Sound.ENTITY_LIGHTING_BOLT_THUNDER, 1.0F, rand.nextFloat() * 0.4F + 0.8F)
    }
  }
}
