package tc.oc.pgm.util.compatability;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.event.entity.EntityDespawnInVoidEvent;
import tc.oc.pgm.api.event.entity.EntityExtinguishEvent;
import tc.oc.pgm.api.event.entity.ExplosionPrimeByEntityEvent;
import tc.oc.pgm.api.event.entity.PotionEffectRemoveEvent;
import tc.oc.pgm.util.event.block.BlockDispenseEntityEvent;
import tc.oc.pgm.util.event.block.BlockFallEvent;
import tc.oc.pgm.util.event.player.PlayerAttackEntityEvent;
import tc.oc.pgm.util.event.player.PlayerLocaleChangeEvent;
import tc.oc.pgm.util.event.player.PlayerOnGroundEvent;
import tc.oc.pgm.util.event.player.PlayerSkinPartsChangeEvent;
import tc.oc.pgm.util.event.player.PlayerSpawnEntityEvent;

public class SportPaperListener implements Listener {

  private static void handleCall(Event pgmEvent, Event sportEvent) {
    if (sportEvent instanceof Cancellable) {
      ((Cancellable) pgmEvent).setCancelled(((Cancellable) sportEvent).isCancelled());
      Bukkit.getServer().getPluginManager().callEvent(pgmEvent);
      ((Cancellable) sportEvent).setCancelled(((Cancellable) pgmEvent).isCancelled());
    } else {
      Bukkit.getServer().getPluginManager().callEvent(pgmEvent);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockFall(org.bukkit.event.block.BlockFallEvent sportEvent) {
    BlockFallEvent pgmEvent = new BlockFallEvent(sportEvent.getBlock(), sportEvent.getEntity());
    handleCall(pgmEvent, sportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerOnGround(org.bukkit.event.player.PlayerOnGroundEvent sportEvent) {
    PlayerOnGroundEvent pgmEvent =
        new PlayerOnGroundEvent(sportEvent.getPlayer(), sportEvent.getOnGround());
    handleCall(pgmEvent, sportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerAttackEntity(org.bukkit.event.player.PlayerAttackEntityEvent sportEvent) {
    PlayerAttackEntityEvent pgmEvent =
        new PlayerAttackEntityEvent(sportEvent.getPlayer(), sportEvent.getLeftClicked());
    handleCall(pgmEvent, sportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onLocaleChange(org.bukkit.event.player.PlayerLocaleChangeEvent sportEvent) {
    PlayerLocaleChangeEvent pgmEvent =
        new PlayerLocaleChangeEvent(
            sportEvent.getPlayer(), sportEvent.getOldLocale(), sportEvent.getNewLocale());
    handleCall(pgmEvent, sportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onEffectRemove(org.bukkit.event.entity.PotionEffectRemoveEvent sportEvent) {
    PotionEffectRemoveEvent pgmEvent =
        new PotionEffectRemoveEvent(sportEvent.getEntity(), sportEvent.getEffect());
    handleCall(pgmEvent, sportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onSkinPartsChange(org.bukkit.event.player.PlayerSkinPartsChangeEvent sportEvent) {
    PlayerSkinPartsChangeEvent pgmEvent = new PlayerSkinPartsChangeEvent(sportEvent.getPlayer());
    handleCall(pgmEvent, sportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityExtinguish(org.bukkit.event.entity.EntityExtinguishEvent sportEvent) {
    EntityExtinguishEvent pgmEvent = new EntityExtinguishEvent(sportEvent.getEntity());
    handleCall(pgmEvent, sportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onDispenseEntity(org.bukkit.event.block.BlockDispenseEntityEvent sportEvent) {
    BlockDispenseEntityEvent pgmEvent =
        new BlockDispenseEntityEvent(
            sportEvent.getBlock(),
            sportEvent.getItem(),
            sportEvent.getVelocity(),
            sportEvent.getEntity());
    handleCall(pgmEvent, sportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerSpawnEntity(org.bukkit.event.player.PlayerSpawnEntityEvent sportEvent) {
    PlayerSpawnEntityEvent pgmEvent =
        new PlayerSpawnEntityEvent(
            sportEvent.getPlayer(),
            sportEvent.getEntity(),
            sportEvent.getLocation(),
            sportEvent.getItem());
    handleCall(pgmEvent, sportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onExplosionPrimeByEntity(
      org.bukkit.event.entity.ExplosionPrimeByEntityEvent sportEvent) {
    ExplosionPrimeByEntityEvent pgmEvent =
        new ExplosionPrimeByEntityEvent(
            sportEvent.getEntity(),
            sportEvent.getRadius(),
            sportEvent.getFire(),
            sportEvent.getPrimer());
    handleCall(pgmEvent, sportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDespawn(org.bukkit.event.entity.EntityDespawnInVoidEvent sportEvent) {
    EntityDespawnInVoidEvent pgmEvent = new EntityDespawnInVoidEvent(sportEvent.getEntity());
    handleCall(pgmEvent, sportEvent);
  }
}
