package tc.oc.pgm.api.party;

import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.query.Query;
import tc.oc.pgm.match.ObservingParty;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.Named;

/**
 * A group of related {@link MatchPlayer}s in a {@link Match}.
 *
 * @see Competitor for participating {@link MatchPlayer}s.
 * @see ObservingParty for observing {@link MatchPlayer}s.
 */
public interface Party extends Audience, Named {

  /**
   * Get the {@link Match} that the {@link Party} is in.
   *
   * @return The {@link Match}.
   */
  Match getMatch();

  /**
   * Get all {@link MatchPlayer}s that are in the {@link Party}.
   *
   * @return All the {@link MatchPlayer}s in the {@link Party}.
   */
  Collection<MatchPlayer> getPlayers();

  /**
   * Get a {@link MatchPlayer} member, based on its unique identifier.
   *
   * @param playerId The unique identifier of the {@link MatchPlayer}.
   * @return The {@link MatchPlayer} or {@code null} if not in the {@link Party}.
   */
  @Nullable
  MatchPlayer getPlayer(UUID playerId);

  /**
   * Get a filter {@link Query} that only matches this {@link Party}.
   *
   * @return The exclusive {@link Query}.
   */
  PartyQuery getQuery();

  /**
   * Get the name of the {@link Party}, which cannot change during {@link Match} time.
   *
   * @return The constant name of the {@link Party}.
   */
  String getDefaultName();

  /**
   * Get whether {@link #getDisplayName()} is grammatically a plural word.
   *
   * @return Whether {@link #getDisplayName()} is plural.
   */
  boolean isNamePlural();

  /**
   * Get the Minecraft {@link ChatColor} of the {@link Party}.
   *
   * @return The {@link ChatColor}.
   */
  ChatColor getColor();

  /**
   * Get the RGB {@link Color} of the {@link Party}.
   *
   * @return The {@link Color}.
   */
  Color getFullColor();

  /**
   * Get the prefix in chat for all {@link MatchPlayer}s in the {@link Party}.
   *
   * @return The chat prefix.
   */
  Component getChatPrefix();

  /**
   * Get whether {@link Match} should automatically add or remove {@link MatchPlayer}s from the
   * {@link Party}.
   *
   * <p>Otherwise, the {@link MatchModule} that registers the {@link Party} must handle that logic.
   *
   * @see Match#setParty(MatchPlayer, Party)
   * @return Whether the {@link Party} is automatically managed.
   */
  boolean isAutomatic();

  /**
   * Get whether this {@link Party} is a {@link Competitor}.
   *
   * @return Whether the {@link Party} is participating.
   */
  default boolean isParticipating() {
    return this instanceof Competitor;
  }

  /**
   * Get whether this {@link Party} is not a {@link Competitor}.
   *
   * @return Whether the {@link Party} is observing.
   */
  default boolean isObserving() {
    return !isParticipating();
  }

  /**
   * Adds a {@link MatchPlayer} to the {@link Party}'s internal state.
   *
   * @see Match#setParty(MatchPlayer, Party)
   * @param player The {@link MatchPlayer} to add.
   */
  void internalAddPlayer(MatchPlayer player);

  /**
   * Removes a {@link MatchPlayer} from the {@link Party}'s internal state.
   *
   * @see Match#setParty(MatchPlayer, Party)
   * @param player The {@link MatchPlayer} to remove.
   */
  void internalRemovePlayer(MatchPlayer player);
}
