package tc.oc.pgm.loot;

import java.util.List;
import tc.oc.pgm.api.filter.Filter;

public class Maybe {
  private final List<Loot> maybeItems;
  private final Filter filter;

  public Maybe(List<Loot> maybeItems, Filter filter) {
    this.maybeItems = maybeItems;
    this.filter = filter;
  }

  public List<Loot> getMaybeItems() {
    return maybeItems;
  }

  public Filter getFilter() {
    return filter;
  }
}
