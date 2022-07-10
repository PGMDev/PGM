package tc.oc.pgm.loot;

import java.util.List;
import tc.oc.pgm.api.filter.Filter;

public class Maybe {
  private final List<Loot> maybeItems;
  private final Filter filter;
  private final List<Maybe> maybeChildren;
  private final List<Any> anyChildren;

  public Maybe(
      List<Loot> maybeItems, Filter filter, List<Maybe> maybeChildren, List<Any> anyChildren) {
    this.maybeItems = maybeItems;
    this.filter = filter;
    this.maybeChildren = maybeChildren;
    this.anyChildren = anyChildren;
  }

  public List<Loot> getMaybeItems() {
    return maybeItems;
  }

  public List<Maybe> getMaybeChildren() {
    return maybeChildren;
  }

  public List<Any> getAnyChildren() {
    return anyChildren;
  }

  public Filter getFilter() {
    return filter;
  }
}
