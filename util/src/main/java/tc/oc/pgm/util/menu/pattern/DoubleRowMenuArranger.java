package tc.oc.pgm.util.menu.pattern;

import static tc.oc.pgm.util.menu.InventoryMenuUtils.emptyRow;

import java.util.ArrayList;
import java.util.List;
import tc.oc.pgm.util.menu.InventoryMenuItem;

// Includes a space, so technically three rows
/** Fits 0-10 items */
public class DoubleRowMenuArranger extends MenuArranger {

  @Override
  public List<InventoryMenuItem> arrangeItems(List<InventoryMenuItem> itemsWithoutSpaces) {

    List<InventoryMenuItem> itemsWithSpaces = new ArrayList<>(27);

    int size = itemsWithoutSpaces.size();

    SingleRowMenuArranger arranger = new SingleRowMenuArranger();

    itemsWithSpaces.addAll(arranger.arrangeItems(itemsWithoutSpaces.subList(0, Math.min(size, 4))));
    itemsWithSpaces.addAll(emptyRow());
    if (size >= 6)
      itemsWithSpaces.addAll(
          arranger.arrangeItems(itemsWithoutSpaces.subList(5, Math.min(size, 9))));
    else itemsWithSpaces.addAll(emptyRow());

    return itemsWithSpaces;
  }

  @Override
  public int rows() {
    return 3;
  }

  @Override // Only fits 10 items per screen
  public int automatedPaginationLimit() {
    return 10;
  }
}
