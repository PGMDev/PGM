package tc.oc.pgm.loot;

import tc.oc.pgm.api.filter.Filter;

public class Option {
  private final double weight;
  private final Filter filter;
  private final Loot item;

  public Option(double weight, Filter filter, Loot item) {
    this.weight = weight;
    this.filter = filter;
    this.item = item;
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
}
