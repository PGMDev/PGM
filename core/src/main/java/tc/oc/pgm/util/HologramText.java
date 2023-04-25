package tc.oc.pgm.util;

import static tc.oc.pgm.util.Assert.assertNotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.util.text.TextTranslations;

/** Epic floating text, its like a sci-fi movie! */
public class HologramText implements Listener {

  private final Match match;

  private @Nullable ArmorStand labelEntity;
  private Location location;
  private Component text;

  public HologramText(
      @NotNull Match match, @NotNull Location location, @NotNull Component text, boolean show) {
    assertNotNull(match, "match");
    this.match = match;
    assertNotNull(location, "location");
    this.location = location;
    assertNotNull(text, "text");
    this.text = text;

    if (show) this.show();

    this.setText(this.text);

    match.addListener(this, MatchScope.LOADED);
  }

  /** Spawn in the hologram text at the previously given location, if not already spawned */
  public void show() {
    if (this.labelEntity != null) return;

    this.labelEntity = match.getWorld().spawn(location, ArmorStand.class);
    this.labelEntity.setVisible(false);
    this.labelEntity.setMarker(true);
    this.labelEntity.setGravity(false);
    this.labelEntity.setRemoveWhenFarAway(false);
    this.labelEntity.setSmall(true);
    this.labelEntity.setArms(false);
    this.labelEntity.setBasePlate(false);
    this.labelEntity.setCustomNameVisible(true);
    this.setLabelEntityName(this.text);
  }

  private void setLabelEntityName(@NotNull Component text) {
    assertNotNull(this.labelEntity, "labelEntity");
    this.labelEntity.setCustomName(TextTranslations.translateLegacy(text));
  }

  /**
   * Spawn in the hologram text at the given location, if not already spawned
   *
   * @param location the location to spawn the text at
   */
  public void show(@NotNull Location location) {
    assertNotNull(location, "location");
    if (this.labelEntity != null) return;
    this.show();
    this.setLocation(location);
  }

  /** Hide the hologram text if currently spawned */
  public void hide() {
    if (this.labelEntity == null) return;
    this.labelEntity.remove();
    this.labelEntity = null;
  }

  /**
   * Set the text this hologram will display, will update the hologram if its currently spawned
   *
   * @param text the text this hologram should show
   */
  public void setText(@NotNull Component text) {
    this.text = text;
    if (this.labelEntity != null) {
      this.setLabelEntityName(this.text);
    }
  }

  /**
   * Set the location of this hologram, will move the hologram if its currently spawned
   *
   * @param location the location this hologram should display at
   */
  public void setLocation(@NotNull Location location) {
    assertNotNull(location, "location");
    this.location = location;
    if (this.labelEntity != null) {
      this.labelEntity.teleport(this.location);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    if (event.getEntity() == this.labelEntity) {
      event.setCancelled(true);

      if (event instanceof EntityDamageByEntityEvent
          && ((EntityDamageByEntityEvent) event).getDamager() instanceof Projectile) {
        ((EntityDamageByEntityEvent) event).getDamager().remove();
      }
    }
  }
}
