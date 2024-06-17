package tc.oc.pgm.platform.v1_20_6;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import tc.oc.pgm.util.event.block.BlockFallEvent;
import tc.oc.pgm.util.event.entity.EntityDespawnInVoidEvent;
import tc.oc.pgm.util.event.entity.PotionEffectAddEvent;
import tc.oc.pgm.util.event.entity.PotionEffectRemoveEvent;
import tc.oc.pgm.util.event.player.PlayerAttackEntityEvent;
import tc.oc.pgm.util.event.player.PlayerLocaleChangeEvent;
import tc.oc.pgm.util.event.player.PlayerOnGroundEvent;
import tc.oc.pgm.util.event.player.PlayerSkinPartsChangeEvent;

/**
 * TODO: fix unsupported events: <br>
 * - BlockDispenseEntityEvent <br>
 * - ExplosionPrimeByEntityEvent <br>
 * - EntityExtinguishEvent <br>
 */
public class ModernListener implements Listener {

  private static void handleCall(Event pgmEvent, Event modernEvent) {
    if (pgmEvent == null) return;
    if (modernEvent instanceof Cancellable modernCancel
        && pgmEvent instanceof Cancellable pgmCancel) {
      pgmCancel.setCancelled(modernCancel.isCancelled());
      Bukkit.getServer().getPluginManager().callEvent(pgmEvent);
      modernCancel.setCancelled(pgmCancel.isCancelled());
    } else {
      Bukkit.getServer().getPluginManager().callEvent(pgmEvent);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockFall(EntitySpawnEvent event) {
    if (event.getEntity() instanceof FallingBlock fb) {
      BlockFallEvent pgmEvent = new BlockFallEvent(event.getLocation().getBlock(), fb);
      handleCall(pgmEvent, event);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerOnGround(EntityPoseChangeEvent event) {
    var entity = event.getEntity();
    if (entity instanceof Player p) {
      var pgmEvent = new PlayerOnGroundEvent(p, p.isOnGround());
      handleCall(pgmEvent, event);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerAttackEntity(PrePlayerAttackEntityEvent event) {
    var pgmEvent = new PlayerAttackEntityEvent(event.getPlayer(), event.getAttacked());
    handleCall(pgmEvent, event);
  }

  @EventHandler(ignoreCancelled = true)
  public void onEffectChange(EntityPotionEffectEvent event) {
    handleCall(
        switch (event.getAction()) {
          case ADDED -> new PotionEffectAddEvent(event.getEntity(), event.getNewEffect());
          case REMOVED -> new PotionEffectRemoveEvent(event.getEntity(), event.getOldEffect());
          default -> null;
        },
        event);
  }

  @EventHandler(ignoreCancelled = true)
  public void onSkinPartsChange(PlayerClientOptionsChangeEvent event) {
    if (event.hasSkinPartsChanged()) {
      var pgmEvent = new PlayerSkinPartsChangeEvent(event.getPlayer());
      handleCall(pgmEvent, event);
    }
    if (event.hasLocaleChanged()) {
      var pgmEvent = new PlayerLocaleChangeEvent(
          event.getPlayer(),
          event.getPlayer().getClientOption(ClientOption.LOCALE),
          event.getLocale());
      handleCall(pgmEvent, event);
    }
  }

  @EventHandler(ignoreCancelled = true)
  @SuppressWarnings("removal")
  public void onEntityDespawn(org.bukkit.event.entity.EntityRemoveEvent sportEvent) {
    if (sportEvent.getCause() == org.bukkit.event.entity.EntityRemoveEvent.Cause.OUT_OF_WORLD) {
      EntityDespawnInVoidEvent pgmEvent = new EntityDespawnInVoidEvent(sportEvent.getEntity());
      handleCall(pgmEvent, sportEvent);
    }
  }

  @EventHandler
  public void onMatchLoad(WorldLoadEvent event) {
    event.getWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
  }
}
