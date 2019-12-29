package tc.oc.pgm.api.match;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Physical;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.map.MapNotFoundException;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.rotation.PGMMapOrder;

/** A manager that creates, loads, unloads, and cycles {@link Match}es. */
public interface MatchManager extends MatchPlayerResolver, Audience {

  /**
   * Starts the creation of a new {@link Match} and {@link World} from a {@link PGMMap}, without
   * loading it.
   *
   * @param map The {@link PGMMap} for the {@link Match}.
   * @throws Throwable If the match cannot be created.
   */
  void createPreMatchAsync(PGMMap map) throws Throwable;

  /**
   * Creates a new {@link Match} and {@link World} from a {@link PGMMap}.
   *
   * @param map The {@link PGMMap} for the {@link Match}.
   * @return The {@link Match} in an unloaded, idle state.
   * @throws Throwable If the match cannot be created.
   */
  Match createMatch(PGMMap map) throws Throwable;

  /**
   * Get the {@link Match} for the specified {@link World}.
   *
   * @param world The {@link World} to lookup.
   * @return The {@link Match} or {@code null} if not found.
   */
  @Nullable
  Match getMatch(@Nullable World world);

  /**
   * Get the {@link Match} for the specified {@link Entity}.
   *
   * @param entity The {@link Entity} to lookup.
   * @return The {@link Match} or {@code null} if not found.
   */
  @Nullable
  default Match getMatch(@Nullable Entity entity) {
    if (entity == null) return null;
    return getMatch(entity.getWorld());
  }

  /**
   * Get the {@link Match} for the specified {@link CommandSender}.
   *
   * <p>If the {@link CommandSender} is a {@link ConsoleCommandSender}, then the first {@link Match}
   * is returned.
   *
   * @param sender The {@link CommandSender} to lookup.
   * @return The {@link Match} or {@code null} if not found.
   */
  @Nullable
  default Match getMatch(@Nullable CommandSender sender) {
    if (sender instanceof Physical) return getMatch((Entity) sender);
    if (sender instanceof ConsoleCommandSender) return getMatches().iterator().next();
    return null;
  }

  /**
   * Get all the {@link Match}es currently registered.
   *
   * @return All the {@link Match}es.
   */
  Collection<Match> getMatches();

  /**
   * Get all the {@link MatchPlayer}s in all {@link Match}es.
   *
   * @return All the {@link MatchPlayer}s.
   */
  default Collection<MatchPlayer> getPlayers() {
    return getMatches().stream()
        .flatMap(match -> match.getPlayers().stream())
        .collect(Collectors.toList());
  }

  /**
   * Unload and and remove a {@link Match} from the registry.
   *
   * @param id The match id to remove.
   */
  void unloadMatch(@Nullable String id);

  /**
   * Unload the old {@link Match} and move all players to a new {@link Match}.
   *
   * @param oldMatch The old match to unload.
   * @param nextMap The map to set next.
   * @param retry Whether to retry loading new maps.
   * @return The new match, or empty if a failure occurred.
   */
  Optional<Match> cycleMatch(@Nullable Match oldMatch, PGMMap nextMap, boolean retry);

  // TODO: Move to either MapLibrary or MapLoader, this is an orthogonal concern
  @Deprecated
  Collection<PGMMap> loadNewMaps() throws MapNotFoundException;

  void setMapOrder(PGMMapOrder pgmMapOrder);

  PGMMapOrder getMapOrder();
}
