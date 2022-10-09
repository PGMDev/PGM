package tc.oc.pgm.blitz;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

/**
 * Called when a player is eliminated in the blitz game mode.
 *
 * <p>Implementation note: This method will be called shortly after the player dies and before the
 * player is transferred to an observing team.
 */
public class BlitzPlayerEliminatedEvent extends MatchPlayerEvent {
  public BlitzPlayerEliminatedEvent(
      @NotNull MatchPlayer player,
      @NotNull Competitor competitor,
      @NotNull Location deathLocation) {
    super(player);

    Preconditions.checkNotNull(player, "match player");
    Preconditions.checkNotNull(competitor, "competitor");
    Preconditions.checkNotNull(deathLocation, "death location");

    this.player = player;
    this.competitor = competitor;
    this.deathLocation = deathLocation;
  }

  /**
   * Gets the player who died and was eliminated.
   *
   * @return Eliminated player
   */
  public @NotNull MatchPlayer getPlayer() {
    return this.player;
  }

  /**
   * Gets the team that the player was eliminated from.
   *
   * @return Player team
   */
  public @NotNull Competitor getCompetitor() {
    return this.competitor;
  }

  /**
   * Gets the location where the player died and was eliminated.
   *
   * @return Death location
   */
  public @NotNull Location getDeathLocation() {
    return this.deathLocation;
  }

  private final @NotNull MatchPlayer player;
  private final @NotNull Competitor competitor;
  private final @NotNull Location deathLocation;

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
