package tc.oc.pgm.modules;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.pgm.spawns.events.ParticipantPostSpawnEvent;
import tc.oc.pgm.util.collection.DefaultMapAdapter;

@ListenerScope(MatchScope.LOADED)
public class SpectateMatchModule implements MatchModule, Listener {

  private final Match match;

  // Stores which players (the list) is spectating a player(the uuid)
  private final Map<UUID, List<UUID>> spectators = new DefaultMapAdapter<>(new ArrayList<>(), true);

  public SpectateMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler
  public void onPlayerAppear(ParticipantPostSpawnEvent event) {
    List<MatchPlayer> spectators = getSpectating(event.getPlayer());
    if (spectators == null) return;

    spectators.forEach(
        player -> {
          // This is kinda hacky, but seems to fix bukkit weirdness.
          // The manual teleport is necessary because it seems like the teleport done when
          // a new spectator target is set can set the spectating player into a buggy state where
          // the server thinks the player is moving too fast(dont ask me why)
          // It also seems like leaving and entering spectate mode on the same tick does not work.
          // While testing delaying by 1 tick also sometimes bugged out, so the
          // re-enter to spectate is delayed by 3 ticks for safety.
          player.getBukkit().setSpectatorTarget(null);
          player.getBukkit().teleport(event.getPlayer().getBukkit());
          match
              .getExecutor(MatchScope.LOADED)
              .schedule(
                  () -> player.getBukkit().setSpectatorTarget(event.getPlayer().getBukkit()),
                  3 * 50,
                  TimeUnit.MILLISECONDS);
        });
  }

  @EventHandler
  public void onPlayerLeave(PlayerLeaveMatchEvent event) {
    event.getPlayer().getSpectators().forEach(p -> p.getBukkit().setSpectatorTarget(null));
  }

  @EventHandler
  public void onPlayerTeleport(PlayerTeleportEvent event) {
    if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
      MatchPlayer player = match.getPlayer(event.getPlayer());
      if (player != null) {
        MatchPlayer spectating = player.getSpectatorTarget();
        if (spectating != null) { // Player is going into spectate
          spectators.get(spectating.getId()).add(player.getId());
        } else {
          spectators.forEach(
              (spectatee, spectators) -> spectators.removeIf(uuid -> uuid == player.getId()));
        }
      }
    }
  }

  /**
   * Get the {@link MatchPlayer}s currently spectating the {@link MatchPlayer} that holds the given
   * {@link UUID}, if any.
   */
  public List<MatchPlayer> getSpectating(UUID player) {
    final List<UUID> list = spectators.get(player);
    if (list == null) return ImmutableList.of();
    return Collections.unmodifiableList(
        list.stream().map(match::getPlayer).filter(Objects::nonNull).collect(Collectors.toList()));
  }

  /** Get the {@link MatchPlayer}s currently spectating the given {@link MatchPlayer}, if any. */
  public List<MatchPlayer> getSpectating(MatchPlayer player) {
    return getSpectating(player.getId());
  }

  /** Get the {@link MatchPlayer}s currently spectating the given {@link Entity}, if any. */
  public List<MatchPlayer> getSpectating(Entity entity) {
    return getSpectating(entity.getUniqueId());
  }
}
