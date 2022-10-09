package tc.oc.pgm.points;

import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;

/** Try each child once, in order */
public class SequentialPointProvider extends AggregatePointProvider {

  public SequentialPointProvider(Collection<? extends PointProvider> children) {
    super(children);
  }

  @Override
  public Location getPoint(Match match, @Nullable Entity entity) {
    for (PointProvider child : children) {
      Location loc = child.getPoint(match, entity);
      if (loc != null) return loc;
    }
    return null;
  }

  @Override
  public boolean canFail() {
    return allChildrenCanFail();
  }
}
