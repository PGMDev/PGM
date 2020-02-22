package tc.oc.pgm.flag.event;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.flag.Net;
import tc.oc.pgm.goals.events.GoalCompleteEvent;

public class FlagCaptureEvent extends GoalCompleteEvent {

  private final Net net;
  private final MatchPlayer carrier;
  private final boolean allFlagsCaptured;

  public FlagCaptureEvent(Flag flag, MatchPlayer carrier, Net net) {
    super(flag.getMatch(), flag, (Competitor) checkNotNull(carrier).getParty(), true);
    this.net = net;
    this.carrier = carrier;

    boolean allFlagsCaptured = true;
    for (FlagDefinition def : this.net.getCapturableFlags()) {
      if (!def.getGoal(getMatch()).isCaptured()) allFlagsCaptured = false;
    }
    this.allFlagsCaptured = allFlagsCaptured;
  }

  @Override
  public Flag getGoal() {
    return (Flag) super.getGoal();
  }

  public Net getNet() {
    return net;
  }

  public MatchPlayer getCarrier() {
    return carrier;
  }

  /**
   * True if all the flags that can be captured in this net are currently in the {@link
   * tc.oc.pgm.flag.state.Captured} state, as of the moment the event was fired. (they may not
   * necessarily be in that state when the listener receives the event).
   */
  public boolean areAllFlagsCaptured() {
    return allFlagsCaptured;
  }
}
