package tc.oc.pgm.api.match;

import com.google.common.collect.Iterables;
import javax.annotation.Nullable;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import tc.oc.pgm.api.chat.MultiAudience;
import tc.oc.pgm.api.player.MatchPlayerResolver;

/** A manager of {@link Match}es. */
public interface MatchManager extends MatchPlayerResolver, MultiAudience {

  /**
   * Get the {@link Match} for the specified {@link World}.
   *
   * @param world The {@link World} to lookup.
   * @return A {@link Match} or {@code null} if not found.
   */
  @Nullable
  Match getMatch(@Nullable World world);

  /**
   * Get all the {@link Match}es currently registered and loaded.
   *
   * @return All {@link Match}es, excludes those that are unloaded or destroyed.
   */
  Iterable<Match> getMatches();

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
    if (sender instanceof Entity) return getMatch((Entity) sender);
    if (sender instanceof ConsoleCommandSender) return Iterables.getFirst(getMatches(), null);
    return null;
  }
}
