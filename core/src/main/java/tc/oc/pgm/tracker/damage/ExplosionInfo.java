package tc.oc.pgm.tracker.damage;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.Location;
import tc.oc.pgm.api.player.ParticipantState;

public class ExplosionInfo implements DamageInfo, RangedInfo, CauseInfo {

  private final PhysicalInfo explosive;

  public ExplosionInfo(PhysicalInfo explosive) {
    this.explosive = checkNotNull(explosive);
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
