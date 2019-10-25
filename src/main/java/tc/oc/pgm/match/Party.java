package tc.oc.pgm.match;

import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import tc.oc.chat.Audience;
import tc.oc.component.Component;
import tc.oc.named.Named;
import tc.oc.pgm.filters.query.IPartyQuery;

public interface Party extends Audience, Named {

  /** Use (instanceof Competitor) */
  @Deprecated
  enum Type {
    Participating,
    Observing
  }

  Match getMatch();

  /** Return a filter query that matches this competitor and no other */
  IPartyQuery getQuery();

  /**
   * The name of this party at match load time (for privileged viewers). This cannot change at any
   * time during the match.
   */
  String getDefaultName();

  /**
   * The current name of the party (for privileged viewers). May change at any time during the
   * match.
   */
  String getName();

  /** The current name of the party. May change at any time during the match. */
  String getName(@Nullable CommandSender viewer);

  /**
   * Should the name of the party be treated as a plural word grammatically? This applies to all
   * possible names returned from any of the name methods.
   */
  boolean isNamePlural();

  ChatColor getColor();

  Color getFullColor();

  /**
   * The party's name (for privileged viewers) with color and no other decorations, legacy
   * formatting
   */
  String getColoredName();

  /** The party's name with color and no other decorations, legacy formatting */
  String getColoredName(@Nullable CommandSender viewer);

  /** The party's name with color and no other decorations */
  Component getComponentName();

  /** Everything before the player's name in chat output */
  Component getChatPrefix();

  /** All players currently in this party */
  Set<MatchPlayer> getPlayers();

  /** Party member with the given ID, or null if they are not in this party */
  @Nullable
  MatchPlayer getPlayer(UUID playerId);

  /**
   * Called by {@link Match#setPlayerParty} to add a player to this party. This method should not be
   * called from anywhere else. This method only needs to modify the party's internal state.
   * Everything else is handled by {@link Match}.
   */
  boolean addPlayer(MatchPlayer player);

  /**
   * Called by {@link Match#setPlayerParty} to remove a player from this party. This method should
   * not be called from anywhere else. This method only needs to modify the party's internal state.
   * Everything else is handled by {@link Match}.
   */
  boolean removePlayer(MatchPlayer player);

  /**
   * If true, the party will be automatically added to the match when the first player joins it, and
   * automatically removed when it becomes empty. If false, the party will never be added or removed
   * by core PGM, and the providing module must handle this.
   */
  boolean isAutomatic();

  Type getType();

  /** Use (instanceof Competitor) */
  boolean isParticipatingType();

  /** Use (instanceof Competitor) */
  boolean isParticipating();

  /** Use (instanceof Competitor) */
  boolean isObservingType();

  /** Use (instanceof Competitor) */
  boolean isObserving();
}
