package tc.oc.pgm.points;

import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;

/** Try a random child up to 100 times */
public class RandomPointProvider extends AggregatePointProvider {

  private static final int MAX_ATTEMPTS = 100;

  public RandomPointProvider(Collection<? extends PointProvider> children) {
    super(children);
  }

  @Override
  public Location getPoint(Match match, @Nullable Entity entity) {
    if (children.isEmpty()) return null;

    for (int i = 0; i < MAX_ATTEMPTS; i++) {
      Location location =
          children.get(match.getRandom().nextInt(children.size())).getPoint(match, entity);
      if (location != null) return location;
    }

    return null;
  }

  @Override
  public boolean canFail() {
    return anyChildrenCanFail();
  }
}
