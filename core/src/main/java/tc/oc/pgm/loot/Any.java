package tc.oc.pgm.loot;

import java.util.List;

public class Any {
  private final List<Loot> anyItems;
  private final List<Option> options;
  private final int count;
  private final boolean unique;

  public Any(List<Loot> anyItems, List<Option> options, int count, boolean unique) {
    this.anyItems = anyItems;
    this.options = options;
    this.count = count;
    this.unique = unique;
  }

  public List<Loot> getAnyItems() {
    return anyItems;
  }

  public int getCount() {
    return count;
  }

  public List<Option> getOptions() {
    return options;
  }

  public boolean isUnique() {
    return unique;
  }
}
