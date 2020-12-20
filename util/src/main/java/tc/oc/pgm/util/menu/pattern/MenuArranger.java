package tc.oc.pgm.util.menu.pattern;

import java.util.List;
import tc.oc.pgm.util.menu.InventoryMenuItem;

// Figures out how to place items in an inventory
public abstract class MenuArranger {

  public abstract List<InventoryMenuItem> arrangeItems(List<InventoryMenuItem> itemsWithoutSpaces);

  // If the items surpass this limit a pagination row will be added to the inventory using this
  // arranger
  public int automatedPaginationLimit() {
    return Integer.MAX_VALUE;
  }
}
