package tc.oc.pgm.join;

/** Encapsulates the result of a match join operation or query */
public enum JoinResultOption implements JoinResult {
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

  public final boolean success;

  JoinResultOption(boolean success) {
    this.success = success;
  }

  @Override
  public boolean isSuccess() {
    return success;
  }

  @Override
  public JoinResultOption getOption() {
    return this;
  }
}
