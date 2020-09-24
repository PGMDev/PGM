package tc.oc.pgm.payload;

import javax.annotation.Nullable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.goals.events.GoalEvent;

/** This event is called as the LAST thing before actually changing the current checkpoint */
public class PayloadReachCheckpointEvent extends GoalEvent {

  private final Payload payload;
  private final boolean forwards;

  protected PayloadReachCheckpointEvent(
      Payload payload, @Nullable Competitor currentOwner, boolean forwards) {
    super(payload.getMatch(), payload, currentOwner);
    this.payload = payload;
    this.forwards = forwards;
  }

  public Payload getPayload() {
    return payload;
  }

  public boolean wasForwards() {
    return forwards;
  }
}
