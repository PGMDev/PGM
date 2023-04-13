package tc.oc.pgm.api.player;

import java.util.List;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.attribute.Attribute;
import tc.oc.pgm.util.attribute.AttributeInstance;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.named.Named;

/**
 * {@link MatchPlayer} is the "core" player object that lasts the duration of a {@link Match}.
 *
 * <p>In the future, we want to avoid the use of {@link #getBukkit()}. Typically the only reason
 * this is used is to check if a {@link MatchPlayer} is involved in an event. If you need to access
 * or modify a {@link Player}, there should be a method added to {@link MatchPlayer}.
 */
public interface MatchPlayer
    extends Audience, Named, Tickable, InventoryHolder, Filterable<PlayerQuery>, PlayerQuery {

  /**
   * Get the {@link Match} of the {@link MatchPlayer}.
   *
   * @return The {@link Match}.
   */
  Match getMatch();

  /**
   * Get the {@link Party} that owns the {@link MatchPlayer}.
   *
   * @return The {@link Party}.
   */
  Party getParty();

  /**
   * Get the {@link Competitor} that owns the {@link MatchPlayer}.
   *
   * @return The {@link Competitor}, or {@code null} if not participating.
   */
  @Nullable
  Competitor getCompetitor();

  /**
   * Get the unique identifier of the {@link MatchPlayer}.
   *
   * @return The unique identifier.
   */
  UUID getId();

  /**
   * Take a "snapshot" of the current {@link MatchPlayer} and return its {@link MatchPlayerState}.
   *
   * @return A "snapshot" of the {@link MatchPlayer}.
   */
  MatchPlayerState getState();

  /**
   * Take a "snapshot" of the current {@link MatchPlayer} and return its {@link ParticipantState}.
   *
   * @return A "snapshot" of the {@link MatchPlayer}, or {@code null} if not participating.
   */
  @Nullable
  ParticipantState getParticipantState();

  /**
   * Get the underlying {@link Player} that is associated with the {@link MatchPlayer}.
   *
   * <p>This method has been kept since a significant portion of the codebase depends on it. Going
   * forward, we should avoid using it and add new methods to operate on the {@link Player} without
   * needing to expose {@link Player} to callers.
   *
   * <p>Another option is having {@link MatchPlayer} extend {@link Player} as a delegate.
   *
   * @return The {@link Player}, or {@code null} if offline.
   */
  Player getBukkit();

  /**
   * Get whether the {@link MatchPlayer}, and its {@link Party}, are participating.
   *
   * @return Whether the {@link MatchPlayer} is participating.
   */
  boolean isParticipating();

  /**
   * Get whether the {@link MatchPlayer}, and its {@link Party}, are observing.
   *
   * @return Whether the {@link MatchPlayer} is observing.
   */
  boolean isObserving();

  /**
   * Get whether the {@link MatchPlayer} is dead.
   *
   * <p>Note that {@link MatchPlayer} does not use {@link Player#isDead()} because it uses a special
   * spawn screen. Therefore, this method could be {@code true} and {@link Player#isDead()} would be
   * {@code false}.
   *
   * @return Whether the {@link MatchPlayer} is dead.
   */
  boolean isDead();

  /**
   * Get whether the {@link MatchPlayer} is alive.
   *
   * @return Whether the {@link MatchPlayer} is alive.
   */
  boolean isAlive();

  /**
   * Get whether the {@link MatchPlayer} can be seen in the {@link Match}.
   *
   * @return Whether the {@link MatchPlayer} can be seen.
   */
  boolean isVisible();

  /**
   * Get whether the {@link MatchPlayer} is currently frozen and cannot move.
   *
   * @return Whether the {@link MatchPlayer} is frozen.
   */
  boolean isFrozen();

  /**
   * Get whether the {@link MatchPlayer} is using a legacy version (1.7.X)
   *
   * @return Whether the {@link MatchPlayer} is using a legacy version
   */
  default boolean isLegacy() {
    return getProtocolVersion() <= ViaUtils.VERSION_1_7;
  }

  /**
   * Get whether the {@link MatchPlayer} can interact with things in the {@link Match}.
   *
   * @return Whether the {@link MatchPlayer} can interact.
   */
  boolean canInteract();

  /**
   * Get whether the {@link MatchPlayer} can see another {@link MatchPlayer}.
   *
   * @param other The other {@link MatchPlayer} to see.
   * @return Whether the {@link MatchPlayer} can see the other one.
   */
  boolean canSee(MatchPlayer other);

  /** Reset the {@link MatchPlayer} ability to interact with the world . */
  void resetInteraction();

  /** Reset the {@link #getInventory()} of the {@link MatchPlayer}. */
  void resetInventory();

  /** Reset the {@link #canSee(MatchPlayer)} visibility of other {@link MatchPlayer}s. */
  void resetVisibility();

  /** Reset all {@link Player} state related to the {@link MatchPlayer}. */
  void reset();

  /**
   * Mark the {@link MatchPlayer} as dead.
   *
   * @param dead Whether the {@link MatchPlayer} should be dead.
   * @see #isDead()
   */
  void setDead(boolean dead);

  /**
   * Change the visibility of the {@link MatchPlayer}.
   *
   * @param visible Whether to be visible.
   */
  void setVisible(boolean visible);

  /**
   * Mark the {@link MatchPlayer} as frozen.
   *
   * @param frozen Whether to be frozen.
   */
  void setFrozen(boolean frozen);

  /**
   * Apply a {@link Kit} to the {@link MatchPlayer}.
   *
   * @param kit The {@link Kit} to apply.
   * @param force Whether to apply with force.
   */
  void applyKit(Kit kit, boolean force);

  /**
   * Set the {@link MatchPlayer}'s {@link GameMode}.
   *
   * @param gameMode - The gamemode to set
   */
  void setGameMode(GameMode gameMode);

  /**
   * Get the protocol version of the {@link MatchPlayer}'s client
   *
   * @return The protocol version of the {@link MatchPlayer}'s client
   */
  int getProtocolVersion();

  /**
   * Get the {@link Settings} of the {@link MatchPlayer}.
   *
   * @return The cached {@link Settings}.
   */
  Settings getSettings();

  String getPrefixedName();

  AttributeInstance getAttribute(Attribute attribute);

  /**
   * Get the {@link GameMode} of the {@link MatchPlayer}.
   *
   * @return the current {@link GameMode}
   */
  GameMode getGameMode();

  @Override
  PlayerInventory getInventory();

  /**
   * Get the current spectator target of the {@link MatchPlayer} if any
   *
   * @return the current spectator target if any
   */
  @Nullable
  MatchPlayer getSpectatorTarget();

  /**
   * Get the players currently spectating the {@link MatchPlayer}, if any
   *
   * @return the players currently spectating, if any
   */
  List<MatchPlayer> getSpectators();

  @Deprecated
  void internalSetParty(Party party);

  @Override
  default Class<? extends Entity> getEntityType() {
    return Player.class;
  }

  @Nullable
  @Override
  default MatchPlayer getPlayer() {
    return this;
  }
}
