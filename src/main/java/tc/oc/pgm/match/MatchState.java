package tc.oc.pgm.match;

/** Finite state machine for Match instances. */
public enum MatchState {
  /** Indicates a match that is waiting to start. */
  Idle() {
    @Override
    public boolean canTransitionTo(MatchState newState) {
      return newState == Starting || newState == Running;
    }
  },

  /** Indicates a match that is going to start. */
  Starting() {
    @Override
    public boolean canTransitionTo(MatchState newState) {
      return newState == Idle || newState == Running || newState == Starting;
    }
  },

  /** Indicates a match that is running. */
  Running() {
    @Override
    public boolean canTransitionTo(MatchState newState) {
      return newState == Finished;
    }
  },

  /** Indicates a match that is completed. */
  Finished() {
    @Override
    public boolean canTransitionTo(MatchState newState) {
      return false;
    }
  };

  /** Indicates whether this existing state can transition to the new state. */
  public abstract boolean canTransitionTo(MatchState newState);
}
