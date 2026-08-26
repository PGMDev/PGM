package tc.oc.pgm.platform.modern.packets;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import io.papermc.paper.event.player.PlayerFailMoveEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.event.player.PlayerOnGroundEvent;
import tc.oc.pgm.util.nms.packets.PacketEventsUtil;

@NullMarked
@SuppressWarnings("deprecation")
public class OnGroundListener implements Listener {

  private final PacketListenerCommon listener;
  private final Map<UUID, Boolean> pendingOnGround = new ConcurrentHashMap<>();
  private final Map<UUID, Boolean> lastOnGround = new ConcurrentHashMap<>();

  public OnGroundListener() {
    this.listener = PacketEventsUtil.registerReceive(
        PacketListenerPriority.MONITOR,
        Map.of(
            PacketType.Play.Client.PLAYER_FLYING, this::handlePlayerMovement,
            PacketType.Play.Client.PLAYER_POSITION, this::handlePlayerMovement,
            PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, this::handlePlayerMovement,
            PacketType.Play.Client.PLAYER_ROTATION, this::handlePlayerMovement));
  }

  public void unregister() {
    PacketEventsUtil.unregister(listener);
    pendingOnGround.clear();
    lastOnGround.clear();
  }

  private void handlePlayerMovement(PacketReceiveEvent event) {
    if (event.isCancelled()) return;

    Player player = event.getPlayer();
    if (player == null) return;

    var wrapper = new WrapperPlayClientPlayerFlying(event);
    boolean onGround = wrapper.isOnGround();
    UUID playerId = player.getUniqueId();
    pendingOnGround.put(playerId, onGround);
    Bukkit.getScheduler().runTask(PGM.get(), () -> validate(playerId, onGround));
  }

  private void validate(UUID playerId, boolean onGround) {
    if (!pendingOnGround.remove(playerId, onGround)) return;

    Player player = Bukkit.getPlayer(playerId);
    if (player == null) {
      removeAll(playerId);
      return;
    }

    emitIfChanged(player, onGround);
  }

  private void emitIfChanged(Player player, boolean onGround) {
    UUID playerId = player.getUniqueId();
    if (!player.isOnline()) {
      removeAll(playerId);
      return;
    }

    boolean currentOnGround = player.isOnGround();
    Boolean previousOnGround = lastOnGround.put(playerId, currentOnGround);

    if (player.isInsideVehicle() || player.isSleeping()) return;
    if (currentOnGround != onGround) return;
    if (previousOnGround == null || previousOnGround == onGround) return;

    new PlayerOnGroundEvent(player, onGround).callEvent();
  }

  private void removeAll(UUID playerId) {
    pendingOnGround.remove(playerId);
    lastOnGround.remove(playerId);
  }

  private void resetPending(Player player) {
    UUID playerId = player.getUniqueId();
    pendingOnGround.remove(playerId);

    if (player.isOnline()) {
      lastOnGround.put(playerId, player.isOnGround());
    } else {
      lastOnGround.remove(playerId);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    lastOnGround.put(event.getPlayer().getUniqueId(), event.getPlayer().isOnGround());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    removeAll(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerFailMove(PlayerFailMoveEvent event) {
    if (!event.isAllowed()) {
      resetPending(event.getPlayer());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJump(PlayerJumpEvent event) {
    if (event.isCancelled()) {
      resetPending(event.getPlayer());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(PlayerMoveEvent event) {
    if (event instanceof PlayerTeleportEvent) return;

    if (event.isCancelled()) {
      resetPending(event.getPlayer());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerTeleport(PlayerTeleportEvent event) {
    resetPending(event.getPlayer());
  }
}
