package tc.oc.pgm.modules;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import tc.oc.pgm.action.actions.LaunchProjectileAction;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.RUNNING)
public class LaunchProjectileMatchModule implements MatchModule, Listener {

  private static final String METADATA_KEY = "launchProjectile";
  private final Match match;

  public LaunchProjectileMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onDamage(EntityDamageByEntityEvent event) {
    Entity damager = event.getDamager();
    if (!damager.hasMetadata(METADATA_KEY)) return;

    LaunchProjectileAction<?> action =
        (LaunchProjectileAction<?>) damager.getMetadata(METADATA_KEY).getFirst().value();

    if (!(event.getEntity() instanceof Player player)) return;

    MatchPlayer victim = match.getPlayer(player);
    if (victim == null) return;

    Filter damageFilter = action.getDamageFilter();
    if (damageFilter == null) return;

    if (!damageFilter.query(victim).isAllowed()) {
      event.setCancelled(true);
      damager.remove();
    }
  }
}
