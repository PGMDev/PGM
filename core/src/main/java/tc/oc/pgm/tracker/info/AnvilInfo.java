package tc.oc.pgm.tracker.info;

import org.checkerframework.checker.nullness.qual.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;

public class AnvilInfo extends OwnerInfoBase implements DamageInfo {
  public AnvilInfo(ParticipantState owner) {
    super(owner);
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return getOwner();
  }
}
