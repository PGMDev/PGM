package tc.oc.pgm.join;

import javax.annotation.Nullable;
import tc.oc.pgm.api.party.Competitor;

/** Encapsulates the result of a match join operation or query */
public class GenericJoinResult implements JoinResult {

  private final @Nullable Competitor competitor;
  private final Status status;
  private final boolean priorityKick;

  public GenericJoinResult(Status status, @Nullable Competitor competitor, boolean priorityKick) {
    this.status = status;
    this.competitor = competitor;
    this.priorityKick = priorityKick;
  }

  /** Classification of the result */
  public Status getStatus() {
    return status;
  }

  /** The {@link Competitor} that was joined, or attempted to be joined */
  public @Nullable Competitor getCompetitor() {
    return competitor;
  }

  /**
   * Did the join succeed? If this is true, the player will be a member of whatever is returned from
   * {@link #getCompetitor()}, or queued to become one.
   */
  @Override
  public boolean isSuccess() {
    return getStatus().success;
  }

  /** Was a priority kick required for the operation to succeed? */
  public boolean priorityKickRequired() {
    return priorityKick;
  }

  public enum Status {
    JOINED(true), // Join succeeded
    REJOINED(true), // Rejoined permanent team
    REDUNDANT(true), // Join was redundant
    QUEUED(true), // Join was queued until match start
    NO_PERMISSION(false), // No permission to join
    MATCH_FINISHED(false), // Match is already over
    MATCH_STARTED(false), // Match has started and mid-match join is disabled
    SWITCH_DISABLED(false), // Tried to switch teams and team switching is disabled
    CHOICE_DISABLED(false), // Tried to choose a specific team and team choosing is disabled
    CHOICE_DENIED(false), // Tried to choose a specific team without choose-team permission
    FULL(false), // Match/team is full
    VANISHED(false); // Player is vanished, therefore unable to join
    ;

    public final boolean success;

    Status(boolean success) {
      this.success = success;
    }

    public GenericJoinResult toResult() {
      return new GenericJoinResult(this, null, false);
    }

    public GenericJoinResult toResult(Competitor competitor) {
      return new GenericJoinResult(this, competitor, false);
    }

    public GenericJoinResult toResult(Competitor competitor, boolean priorityKick) {
      return new GenericJoinResult(this, competitor, priorityKick);
    }
  }
}
