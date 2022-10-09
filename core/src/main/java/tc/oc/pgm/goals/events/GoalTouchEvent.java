package tc.oc.pgm.goals.events;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.time.Instant;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.goals.TouchableGoal;

/** Raised when a player touches a goal. */
public class GoalTouchEvent extends GoalEvent {
  private final TouchableGoal goal;
  private final @Nullable Competitor competitor;
  private final boolean firstForCompetitor;
  private final @Nullable ParticipantState player;
  private final boolean firstForPlayer;
  private final boolean firstForPlayerLife;
  private final Instant time;
  private boolean cancelToucherMessage;

  /**
   * Creates a new {@link GoalTouchEvent}.
   *
   * @param goal The {@link TouchableGoal} that was touched.
   * @param competitor Team that touched the goal, only if it was their first
   * @param firstForCompetitor
   * @param player The player that touched the goal.
   * @param firstForPlayer
   * @param firstForPlayerLife
   * @param time The time at which the touch occurred.
   */
  public GoalTouchEvent(
      TouchableGoal goal,
      @Nullable Competitor competitor,
      boolean firstForCompetitor,
      @Nullable ParticipantState player,
      boolean firstForPlayer,
      boolean firstForPlayerLife,
      Instant time) {

    super(goal, competitor);
    this.competitor = competitor;
    this.firstForCompetitor = firstForCompetitor;
    this.firstForPlayer = firstForPlayer;
    this.firstForPlayerLife = firstForPlayerLife;
    this.goal = assertNotNull(goal, "Goal");
    this.player = player;
    this.time = assertNotNull(time, "Time");
  }

  public GoalTouchEvent(TouchableGoal goal, Instant time) {
    this(goal, null, false, null, false, false, time);
  }

  public Instant getTime() {
    return this.time;
  }

  @Override
  public @NotNull Competitor getCompetitor() { // remove @Nullable
    //noinspection ConstantConditions
    return super.getCompetitor();
  }

  public boolean isFirstForCompetitor() {
    return firstForCompetitor;
  }

  public @Nullable ParticipantState getPlayer() {
    return this.player;
  }

  public boolean isFirstForPlayer() {
    return firstForPlayer;
  }

  public boolean isFirstForPlayerLife() {
    return firstForPlayerLife;
  }

  @Override
  public TouchableGoal getGoal() {
    return this.goal;
  }

  public boolean getCancelToucherMessage() {
    return cancelToucherMessage;
  }

  public void setCancelToucherMessage(boolean cancelToucherMessage) {
    this.cancelToucherMessage = cancelToucherMessage;
  }

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
