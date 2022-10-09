package tc.oc.pgm.tracker.info;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;

/**
 * Returned by the master damage resolver to indicate that the damage is invalid, i.e. because one
 * of the players involved was not participating.
 */
public class NullDamageInfo implements DamageInfo {
  @Override
  public @Nullable ParticipantState getAttacker() {
    return null;
  }
}
