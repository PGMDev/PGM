package tc.oc.pgm.loot;


import java.util.List;

public class Any {
  private final List<Loot> anyItems;
  private final int count;
  private final boolean unique;

  public Any(List<Loot> anyItems, int count, boolean unique) {
    this.anyItems = anyItems;
    this.count = count;
    this.unique = unique;
  }

  public List<Loot> getAnyItems() {
    return anyItems;
  }

  public int getCount() {
    return count;
  }

  public boolean isUnique() {
    return unique;
  }
}
