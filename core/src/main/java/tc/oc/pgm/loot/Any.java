package tc.oc.pgm.loot;

import java.util.List;

public class Any {
  private final List<Loot> anyItems;
  private final List<Option> options;
  private final int count;
  private final boolean unique;
  private final List<Any> anyChildren;
  private final List<Maybe> maybeChildren;

  public Any(
      List<Loot> anyItems,
      List<Option> options,
      int count,
      boolean unique,
      List<Any> anyChildren,
      List<Maybe> maybeChildren) {
    this.anyItems = anyItems;
    this.options = options;
    this.count = count;
    this.unique = unique;
    this.anyChildren = anyChildren;
    this.maybeChildren = maybeChildren;
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

  public List<Any> getAnyChildren() {
    return anyChildren;
  }

  public List<Maybe> getMaybeChildren() {
    return maybeChildren;
  }

  public boolean isUnique() {
    return unique;
  }
}
