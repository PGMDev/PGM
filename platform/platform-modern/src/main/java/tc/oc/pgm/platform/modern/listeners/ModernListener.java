package tc.oc.pgm.platform.modern.listeners;

import static tc.oc.pgm.util.event.EventUtil.handleCall;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.event.block.BlockFallEvent;
import tc.oc.pgm.util.event.entity.EntityDespawnInVoidEvent;
import tc.oc.pgm.util.event.entity.PotionEffectAddEvent;
import tc.oc.pgm.util.event.entity.PotionEffectRemoveEvent;
import tc.oc.pgm.util.event.player.PlayerAttackEntityEvent;
import tc.oc.pgm.util.event.player.PlayerLocaleChangeEvent;
import tc.oc.pgm.util.event.player.PlayerSkinPartsChangeEvent;
import tc.oc.pgm.util.event.player.PlayerSpawnLocationEvent;

/**
 * TODO: fix unsupported events: <br>
 * - EntityExtinguishEvent <br>
 */
@NullMarked
public class ModernListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onBlockFall(EntitySpawnEvent event) {
    if (event.getEntity() instanceof FallingBlock fb) {
      BlockFallEvent pgmEvent = new BlockFallEvent(event.getLocation().getBlock(), fb);
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
          .runTask(PGM.get(), () -> pgmEvents.forEach(pgmEv -> handleCall(pgmEv, event)));
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDespawn(org.bukkit.event.entity.EntityRemoveEvent modernEvent) {
    if (modernEvent.getCause() == org.bukkit.event.entity.EntityRemoveEvent.Cause.OUT_OF_WORLD) {
      EntityDespawnInVoidEvent pgmEvent = new EntityDespawnInVoidEvent(modernEvent.getEntity());
      handleCall(pgmEvent, modernEvent);
    }
  }

  @EventHandler
  public void onMatchLoad(WorldLoadEvent event) {
    event.getWorld().setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerTeleport(final PlayerTeleportEvent event) {
    switch (event.getCause()) {
      case NETHER_PORTAL, END_PORTAL -> event.setCancelled(true);
    }
  }

  private final ServerboundPlayerLoadedPacket PLAYER_LOADED_PACKET =
      new ServerboundPlayerLoadedPacket();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    // Fix inability to interact after death due to the world considered being unloaded by the
    // player (the server ignores any player action packets coming from players who are considered
    // "unloaded"), since players don't really die in PGM. This has to be done the next tick, as
    // ServerGamePacketListenerImpl#waitingForRespawn is set after the event.
    Bukkit.getScheduler().runTask(PGM.get(), () -> {
      var conn = ((CraftPlayer) event.getEntity()).getHandle().connection;
      conn.restartClientLoadTimerAfterRespawn();
      conn.handleAcceptPlayerLoad(PLAYER_LOADED_PACKET);
    });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onInitialSpawn(io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent event) {
    var pgmEvent = new PlayerSpawnLocationEvent(event.getSpawnLocation());
    handleCall(pgmEvent, event);
    event.setSpawnLocation(pgmEvent.getSpawnLocation());
  }

  // When a player in legacy versions breaks a block but that break is denied,
  // they do not receive a block update and thus the block is broken clientside.
  // This sends a fake block update in that specific case.
  private void resendBlock(Player player, Block block) {
    player.sendBlockChange(block.getLocation(), block.getBlockData());

    BlockState state = block.getState();
    if (state instanceof TileState tileState) {
      player.sendBlockUpdate(block.getLocation(), tileState);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.LEFT_CLICK_BLOCK
        || (event.useInteractedBlock() != Event.Result.DENY
            && event.useItemInHand() != Event.Result.DENY)) {
      return;
    }

    Block block = event.getClickedBlock();
    if (block == null) return;

    resendBlock(event.getPlayer(), block);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onBlockDamage(BlockDamageEvent event) {
    if (!event.isCancelled()) return;

    resendBlock(event.getPlayer(), event.getBlock());
  }
}
