package tc.oc.pgm.modules;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.PlayerVanishEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;

@ListenerScope(MatchScope.LOADED)
public class SpectateMatchModule implements MatchModule, Listener {

  private final Match match;

  // Stores which players (the list) is spectating a player(the uuid)
  private final Multimap<UUID, UUID> spectators = ArrayListMultimap.create();

  public SpectateMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler
  public void onPlayerLeave(PlayerLeaveMatchEvent event) {
    event.getPlayer().getSpectators().forEach(p -> p.getBukkit().setSpectatorTarget(null));
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    spectators.values().removeIf(event.getPlayer().getUniqueId()::equals);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerVanish(PlayerVanishEvent event) {
    if (!event.isVanished()) return;
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
          spectators.entries().removeIf(e -> e.getValue().equals(player.getId()));
        }
      }
    }
  }

  /**
   * Get the {@link MatchPlayer}s currently spectating the {@link MatchPlayer} that holds the given
   * {@link UUID}, if any.
   */
  public List<MatchPlayer> getSpectating(UUID player) {
    final Collection<UUID> list = spectators.get(player);
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
