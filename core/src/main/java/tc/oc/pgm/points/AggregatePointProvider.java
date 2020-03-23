package tc.oc.pgm.points;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.regions.Union;

public abstract class AggregatePointProvider implements PointProvider {

  protected final List<PointProvider> children;
  private Region region;

  public AggregatePointProvider(Collection<? extends PointProvider> children) {
    this.children = ImmutableList.copyOf(children);
  }

  @Override
  public Region getRegion() {
    if (region == null) {
      Region[] regions = new Region[children.size()];
      for (int i = 0; i < regions.length; i++) {
        regions[i] = children.get(i).getRegion();
      }
      region = Union.of(regions);
    }
    return region;
  }

  protected boolean allChildrenCanFail() {
    for (PointProvider child : children) {
      if (!child.canFail()) return false;
    }
    return true;
  }

  protected boolean anyChildrenCanFail() {
    for (PointProvider child : children) {
      if (child.canFail()) return true;
    }
    return false;
  }
}
