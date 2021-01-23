package tc.oc.pgm.util.menu.pattern;

import java.util.ArrayList;
import java.util.List;
import tc.oc.pgm.util.menu.InventoryMenuItem;

/** Fits 0-4 items, never paginates */
public class SingleRowMenuArranger extends MenuArranger {

  @Override
  public List<InventoryMenuItem> arrangeItems(List<InventoryMenuItem> itemsWithoutSpaces) {
    final List<InventoryMenuItem> itemsWithSpaces = new ArrayList<>();

    final int s = itemsWithoutSpaces.size();

    itemsWithSpaces.add(s == 5 ? itemsWithoutSpaces.get(0) : null);
    itemsWithSpaces.add(s >= 4 ? itemsWithoutSpaces.get(0) : null);
    itemsWithSpaces.add(s == 2 || s == 3 || s == 5 ? itemsWithoutSpaces.get(s == 5 ? 1 : 0) : null);
    itemsWithSpaces.add(s >= 4 ? itemsWithoutSpaces.get(1) : null);
    itemsWithSpaces.add(
        s == 1 || s == 3 || s == 5 ? itemsWithoutSpaces.get(s == 3 ? 1 : s == 5 ? 2 : 0) : null);
    itemsWithSpaces.add(s >= 4 ? itemsWithoutSpaces.get(2) : null);
    itemsWithSpaces.add(
        s == 2 || s == 3 || s == 5 ? itemsWithoutSpaces.get(s == 3 ? 2 : s == 5 ? 3 : 1) : null);
    itemsWithSpaces.add(s >= 4 ? itemsWithoutSpaces.get(3) : null);
    itemsWithSpaces.add(s == 5 ? itemsWithoutSpaces.get(4) : null);

    return itemsWithSpaces;
  }

  @Override
  public int rows() {
    return 1;
  }
}
