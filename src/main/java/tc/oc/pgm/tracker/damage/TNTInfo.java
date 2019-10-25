package tc.oc.pgm.tracker.damage;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.Location;
import tc.oc.pgm.match.ParticipantState;

public class TNTInfo extends OwnerInfoBase implements RangedInfo {

  private final Location origin;

  public TNTInfo(ParticipantState owner, Location origin) {
    super(owner);
    this.origin = checkNotNull(origin);
  }

  @Override
  public Location getOrigin() {
    return origin;
  }
}
