package tc.oc.pgm.points;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.EmptyRegion;

public class NullPointProvider implements PointProvider {

  public static final NullPointProvider INSTANCE = new NullPointProvider();

  private NullPointProvider() {}

  @Override
  public Location getPoint(Match match, @Nullable Entity entity) {
    return null;
  }

  @Override
  public Region getRegion() {
    return EmptyRegion.INSTANCE;
  }

  @Override
  public boolean canFail() {
    return true;
  }
}
