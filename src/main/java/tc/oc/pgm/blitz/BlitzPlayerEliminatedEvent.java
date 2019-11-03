package tc.oc.pgm.blitz;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Called when a player is eliminated in the blitz game mode.
 *
 * <p>Implementation note: This method will be called shortly after the player dies and before the
 * player is transferred to an observing team.
 */
public class BlitzPlayerEliminatedEvent extends MatchEvent {
  public BlitzPlayerEliminatedEvent(
      @Nonnull Match match,
      @Nonnull MatchPlayer player,
      @Nonnull Competitor competitor,
      @Nonnull Location deathLocation) {
    super(match);

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
  public @Nonnull MatchPlayer getPlayer() {
    return this.player;
  }

  /**
   * Gets the team that the player was eliminated from.
   *
   * @return Player team
   */
  public @Nonnull Competitor getCompetitor() {
    return this.competitor;
  }

  /**
   * Gets the location where the player died and was eliminated.
   *
   * @return Death location
   */
  public @Nonnull Location getDeathLocation() {
    return this.deathLocation;
  }

  private final @Nonnull MatchPlayer player;
  private final @Nonnull Competitor competitor;
  private final @Nonnull Location deathLocation;

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
