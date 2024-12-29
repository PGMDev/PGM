package tc.oc.pgm.hologram;

import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.LOADED)
public class HologramMatchModule implements MatchModule, Listener {

  private final Set<Hologram> labelEntities = new HashSet<>();
  private final Match match;

  public HologramMatchModule(Match match) {
    this.match = match;
  }

  /**
   * Create a hologram with text
   *
   * @param location where the hologram is located
   * @param text the text to display
   * @param show should this hologram be shown right away?
   * @return Hologram object which can be used to change text, location and show/hide the hologram
   */
  public Hologram createHologram(
      @NotNull Location location, @NotNull Component text, boolean show) {
    final Hologram hologram = new Hologram(this.match, location, text, show);
    this.labelEntities.add(hologram);
    return hologram;
  }

  /**
   * Create a hologram with text
   *
   * @param vector where the hologram is located
   * @param text the text to display
   * @param show should this hologram be shown right away?
   * @return Hologram object which can be used to change text, location and show/hide the hologram
   */
  public Hologram createHologram(@NotNull Vector vector, @NotNull Component text, boolean show) {
    final Hologram hologram =
        new Hologram(this.match, vector.toLocation(this.match.getWorld()), text, show);
    this.labelEntities.add(hologram);
    return hologram;
  }

  @Override
  public void unload() {
    for (Hologram labelEntity : this.labelEntities) {
      labelEntity.hide();
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    for (Hologram hologram : this.labelEntities) {
      if (event.getEntity() == hologram.labelEntity) {
        event.setCancelled(true);

        if (event instanceof EntityDamageByEntityEvent
            && ((EntityDamageByEntityEvent) event).getDamager() instanceof Projectile) {
          ((EntityDamageByEntityEvent) event).getDamager().remove();
        }
        break;
      }
    }
  }
}
