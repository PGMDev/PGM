package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.Location;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.CauseInfo;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.api.tracker.info.RangedInfo;
import tc.oc.pgm.api.tracker.info.TrackerInfo;

public record ExplosionInfo(PhysicalInfo explosive) implements DamageInfo, RangedInfo, CauseInfo {

  public ExplosionInfo {
    assertNotNull(explosive);
  }

  @Override
  public @Nullable PhysicalInfo damager() {
    return explosive();
  }

  @Override
  public TrackerInfo cause() {
    return explosive();
  }

  @Deprecated
  public PhysicalInfo getExplosive() {
    return explosive();
  }

  @Override
  public @Nullable Location origin() {
    return explosive instanceof RangedInfo r ? r.origin() : null;
  }

  @Override
  public @Nullable ParticipantState attacker() {
    return explosive == null ? null : explosive.owner();
  }
}
