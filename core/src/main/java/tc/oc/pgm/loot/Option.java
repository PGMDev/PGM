package tc.oc.pgm.loot;

import tc.oc.pgm.api.filter.Filter;

public class Option {
  private final int weight;
  private final Filter filter;
  private final Loot item;

  public Option(int weight, Filter filter, Loot item) {
    this.weight = weight;
    this.filter = filter;
    this.item = item;
  }

  public int getWeight() {
    return weight;
  }

  public Filter getFilter() {
    return filter;
  }

  public Loot getItem() {
    return item;
  }
}
