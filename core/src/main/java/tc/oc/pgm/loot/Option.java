package tc.oc.pgm.loot;

import java.util.List;
import tc.oc.pgm.api.filter.Filter;

public class Option {
  private final double weight;
  private final Filter filter;
  private final Loot item;
  private final List<Any> anyChildren;
  private final List<Maybe> maybeChildren;

  public Option(
      double weight, Filter filter, Loot item, List<Any> anyChildren, List<Maybe> maybeChildren) {
    this.weight = weight;
    this.filter = filter;
    this.item = item;
    this.anyChildren = anyChildren;
    this.maybeChildren = maybeChildren;
  }

  public double getWeight() {
    return weight;
  }

  public Filter getFilter() {
    return filter;
  }

  public Loot getItem() {
    return item;
  }

  public List<Any> getAnyChildren() {
    return anyChildren;
  }

  public List<Maybe> getMaybeChildren() {
    return maybeChildren;
  }
}
