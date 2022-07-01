package tc.oc.pgm.loot;

import tc.oc.pgm.api.filter.Filter;

public class Option {
  private final int weight;
  private final Filter filter;

  public Option(int weight, Filter filter) {
    this.weight = weight;
    this.filter = filter;
  }

  public int getWeight() {
    return weight;
  }

  public Filter getFilter() {
    return filter;
  }
}
