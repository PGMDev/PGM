package tc.oc.pgm.util.menu.pattern;

import java.util.List;
import tc.oc.pgm.util.menu.InventoryMenuItem;

// Does nothing
public class IdentityMenuArranger extends MenuArranger {

  private final int rows;
  private final int paginationLimit;

  public IdentityMenuArranger(int rows) {
    this.rows = rows;
    this.paginationLimit = rows * 9;
  }

  @Override
  public List<InventoryMenuItem> arrangeItems(List<InventoryMenuItem> itemsWithoutSpaces) {
    return itemsWithoutSpaces;
  }

  @Override
  public int rows() {
    return rows;
  }

  @Override
  public int automatedPaginationLimit() {
    return paginationLimit;
  }
}
