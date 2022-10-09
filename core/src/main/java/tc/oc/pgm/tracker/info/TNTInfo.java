package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.Location;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.RangedInfo;

public class TNTInfo extends OwnerInfoBase implements RangedInfo {

  private final Location origin;

  public TNTInfo(ParticipantState owner, Location origin) {
    super(owner);
    this.origin = assertNotNull(origin);
  }

  @Override
  public Location getOrigin() {
    return origin;
  }
}
