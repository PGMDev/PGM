package tc.oc.pgm.util.menu.pattern;

import java.util.ArrayList;
import java.util.List;
import tc.oc.pgm.util.menu.InventoryMenuItem;

/** Fits 0-4 items */
public class SingleRowMenuArranger extends MenuArranger {

  @Override
  public List<InventoryMenuItem> arrangeItems(List<InventoryMenuItem> itemsWithoutSpaces) {
    final List<InventoryMenuItem> itemsWithSpaces = new ArrayList<>();

    final int size = itemsWithoutSpaces.size();

    itemsWithSpaces.add(null);
    itemsWithSpaces.add(size >= 4 ? itemsWithoutSpaces.get(0) : null);
    itemsWithSpaces.add(size == 2 || size == 3 ? itemsWithoutSpaces.get(0) : null);
    itemsWithSpaces.add(size >= 4 ? itemsWithoutSpaces.get(1) : null);
    itemsWithSpaces.add(size == 1 || size == 3 ? itemsWithoutSpaces.get(size == 3 ? 1 : 0) : null);
    itemsWithSpaces.add(size >= 4 ? itemsWithoutSpaces.get(2) : null);
    itemsWithSpaces.add(size == 2 || size == 3 ? itemsWithoutSpaces.get(size == 3 ? 2 : 1) : null);
    itemsWithSpaces.add(size >= 4 ? itemsWithoutSpaces.get(3) : null);
    itemsWithSpaces.add(null);

    return itemsWithSpaces;
  }
}
