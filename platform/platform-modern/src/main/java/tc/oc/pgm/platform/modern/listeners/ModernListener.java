package tc.oc.pgm.platform.modern.listeners;

import static tc.oc.pgm.util.event.EventUtil.handleCall;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import tc.oc.pgm.util.bukkit.BukkitUtils;
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
 * - EntityExtinguishEvent <br>
 */
public class ModernListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onBlockFall(EntitySpawnEvent event) {
    if (event.getEntity() instanceof FallingBlock fb) {
      BlockFallEvent pgmEvent = new BlockFallEvent(event.getLocation().getBlock(), fb);
      handleCall(pgmEvent, event);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerOnGround(EntityPoseChangeEvent event) {
    if (event.getEntity() instanceof Player p)
      handleCall(new PlayerOnGroundEvent(p, p.isOnGround()), event);
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
    List<Event> pgmEvents = new ArrayList<>(2);
    if (event.hasSkinPartsChanged()) {
      pgmEvents.add(new PlayerSkinPartsChangeEvent(event.getPlayer()));
    }
    if (event.hasLocaleChanged()) {
      pgmEvents.add(new PlayerLocaleChangeEvent(
          event.getPlayer(),
          event.getPlayer().getClientOption(ClientOption.LOCALE),
          event.getLocale()));
    }
    if (!pgmEvents.isEmpty()) {
      Bukkit.getScheduler()
          .runTask(
              BukkitUtils.getPlugin(), () -> pgmEvents.forEach(pgmEv -> handleCall(pgmEv, event)));
    }
  }

  @EventHandler(ignoreCancelled = true)
  @SuppressWarnings("removal")
  public void onEntityDespawn(org.bukkit.event.entity.EntityRemoveEvent modernEvent) {
    if (modernEvent.getCause() == org.bukkit.event.entity.EntityRemoveEvent.Cause.OUT_OF_WORLD) {
      EntityDespawnInVoidEvent pgmEvent = new EntityDespawnInVoidEvent(modernEvent.getEntity());
      handleCall(pgmEvent, modernEvent);
    }
  }

  @EventHandler
  public void onMatchLoad(WorldLoadEvent event) {
    event.getWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
  }
}
