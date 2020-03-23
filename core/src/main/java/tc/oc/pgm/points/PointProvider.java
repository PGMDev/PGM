package tc.oc.pgm.points;

import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;

public interface PointProvider {

  Location getPoint(Match match, @Nullable Entity entity);

  Region getRegion();

  boolean canFail();
}
