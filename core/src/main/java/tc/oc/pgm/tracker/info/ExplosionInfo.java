package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.CauseInfo;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.api.tracker.info.RangedInfo;
import tc.oc.pgm.api.tracker.info.TrackerInfo;

public class ExplosionInfo implements DamageInfo, RangedInfo, CauseInfo {

  private final PhysicalInfo explosive;

  public ExplosionInfo(PhysicalInfo explosive) {
    this.explosive = assertNotNull(explosive);
  }

  public PhysicalInfo getExplosive() {
    return explosive;
  }

  @Override
  public TrackerInfo getCause() {
    return getExplosive();
  }

  @Override
  public @Nullable Location getOrigin() {
    return explosive instanceof RangedInfo ? ((RangedInfo) explosive).getOrigin() : null;
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return explosive == null ? null : explosive.getOwner();
  }
}
