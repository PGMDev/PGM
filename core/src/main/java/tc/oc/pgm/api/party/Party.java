package tc.oc.pgm.api.party;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.query.Query;
import tc.oc.pgm.match.ObservingParty;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.Named;

/**
 * A group of {@link MatchPlayer}s.
 *
 * @see Competitor
 */
public interface Party extends Audience, Named, Filterable<PartyQuery> {

  /**
   * Gets the match.
   *
   * @return a match
   */
  Match getMatch();

  /**
   * Gets the collection of party members.
   *
   * @return a collection of players
   */
  Collection<MatchPlayer> getPlayers();

  /**
   * Gets the {@link MatchPlayer} of a member.
   *
   * @param playerId a player id
   * @return a player or {@code null} if not a member
   */
  @Nullable
  MatchPlayer getPlayer(final UUID playerId);

  /**
   * Adds a {@link MatchPlayer} to the party.
   *
   * @param player a player
   */
  void addPlayer(final MatchPlayer player);

  /**
   * Removes a {@link MatchPlayer} from the party.
   *
   * @param playerId a player id
   */
  void removePlayer(final UUID playerId);

  /**
   * Gets a query that matches the party.
   *
   * @return a party query
   */
  PartyQuery getQuery();

  /**
   * Sets the party name.
   *
   * @param name a name
   */
  void setName(final String name);

  /**
   * Gets the initial party name, which cannot change.
   *
   * @return a name
   */
  String getDefaultName();

  /**
   * Checks if {@link #getName()} is grammatically plural.
   *
   * @return if the party name is plural
   */
  boolean isNamePlural();

  /**
   * Gets the {@link ChatColor} of the party.
   *
   * @return a chat color
   */
  ChatColor getColor();

  /**
   * Gets the {@link Color} of the party.
   *
   * @return a color
   */
  Color getFullColor();

  /**
   * Gets a chat prefix for the party.
   *
   * @return a component
   */
  Component getChatPrefix();

  /**
   * Checks if players should automatically be added to the party.
   *
   * @return if the party is default
   */
  boolean isAutomatic();

  /**
   * Tests if the party is a {@link Competitor}.
   *
   * @return if the party is a competitor
   * @deprecated {@code x instanceof Competitor}
   */
  @Deprecated
  default boolean isParticipating() {
    return this instanceof Competitor;
  }

  /**
   * Tests if the party is not a {@link Competitor}.
   *
   * @return if the party is not a competitor
   * @deprecated {@code !(x instanceof Competitor)}
   */
  @Deprecated
  default boolean isObserving() {
    return !this.isParticipating();
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

  @Override
  default Optional<? extends Filterable<? super PartyQuery>> filterableParent() {
    return Optional.of(getMatch());
  }

  @Override
  default Stream<? extends Filterable<? extends PlayerQuery>> filterableChildren() {
    return getPlayers().stream().map(MatchPlayer::getQuery);
  }

  @Nullable
  default Event getEvent() {
    return null;
  }
}
