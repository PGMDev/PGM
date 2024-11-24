package tc.oc.pgm.api.party;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.Named;

/**
 * A group of {@link MatchPlayer}s.
 *
 * @see Competitor
 */
public interface Party extends Audience, Named, Filterable<PartyQuery>, PartyQuery {

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

  NamedTextColor getTextColor();

  /**
   * Gets the {@link Color} of the party.
   *
   * @return a color
   */
  Color getFullColor();

  /**
   * Gets the {@link DyeColor} of the party.
   *
   * @return a dye color
   */
  DyeColor getDyeColor();

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
   * Tests if the party is a {@link Competitor} &amp; match is running.
   *
   * @return if the party is currently competing
   */
  boolean isParticipating();

  /**
   * Tests if the party is not participating in the match.
   *
   * @return if the party observer, or match isn't running
   */
  boolean isObserving();

  @Override
  @Nullable
  default Match getFilterableParent() {
    return this.getMatch();
  }

  @Override
  default Collection<? extends Filterable<? extends PlayerQuery>> getFilterableChildren() {
    return this.getPlayers();
  }

  @Override
  @SuppressWarnings("unchecked")
  default <R extends Filterable<?>> Collection<? extends R> getFilterableDescendants(
      Class<R> type) {
    Collection<R> result = new ArrayList<>();
    if (type.isAssignableFrom(Party.class)) {
      result.add((R) this);
    }
    if (type.isAssignableFrom(MatchPlayer.class)) {
      result.addAll((Collection<? extends R>) getPlayers());
    }
    return result;
  }

  @Override
  default Party getParty() {
    return this;
  }
}
