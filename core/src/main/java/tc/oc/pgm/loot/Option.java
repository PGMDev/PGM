package tc.oc.pgm.loot;

import java.util.List;
import tc.oc.pgm.api.filter.Filter;

public class Option {
  private final double weight;
  private final Filter filter;
  private final List<Loot> lootables;
  private final List<Any> anyChildren;
  private final List<Maybe> maybeChildren;

  public Option(
      double weight,
      Filter filter,
      List<Loot> lootables,
      List<Any> anyChildren,
      List<Maybe> maybeChildren) {
    this.weight = weight;
    this.filter = filter;
    this.lootables = lootables;
    this.anyChildren = anyChildren;
    this.maybeChildren = maybeChildren;
  }

  public double getWeight() {
    return weight;
  }

  public Filter getFilter() {
    return filter;
  }

  public List<Loot> getLootables() {
    return lootables;
  }

  public List<Any> getAnyChildren() {
    return anyChildren;
  }

  public List<Maybe> getMaybeChildren() {
    return maybeChildren;
  }
}
