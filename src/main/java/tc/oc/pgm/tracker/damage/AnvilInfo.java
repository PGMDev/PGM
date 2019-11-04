package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

public class AnvilInfo extends OwnerInfoBase implements DamageInfo {
  public AnvilInfo(ParticipantState owner) {
    super(owner);
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return getOwner();
  }
}
