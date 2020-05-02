package tc.oc.pgm.modules;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;

/** Gives a shooter their arrow back if the projectile was cancelled (eg. shooting a teammate). */
@ListenerScope(MatchScope.RUNNING)
public class FriendlyFireRefundMatchModule implements MatchModule, Listener {

  private static final String METADATA_KEY = "refund";
  private static final MetadataValue METADATA_VALUE = new FixedMetadataValue(PGM.get(), true);

  private final Match match;

  public FriendlyFireRefundMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onShoot(EntityShootBowEvent event) {
    if (event.getBow().getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0) return;

    final MatchPlayer shooter = match.getParticipant(event.getEntity());
    if (shooter == null) return;

    event.getProjectile().setMetadata(METADATA_KEY, METADATA_VALUE);
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onHit(EntityDamageByEntityEvent event) {
    final Entity damager = event.getDamager();
    if (!event.isCancelled()
        || event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE
        || !(damager instanceof Projectile)
        || !damager.hasMetadata(METADATA_KEY)) return;

    final Player shooter = (Player) ((Projectile) damager).getShooter();
    if (match.getParticipant(shooter) == null) return;

    damager.remove();
    shooter.getInventory().addItem(new ItemStack(Material.ARROW));
  }
}
