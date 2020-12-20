package tc.oc.pgm.util.menu.pattern;

import java.util.List;
import tc.oc.pgm.util.menu.InventoryMenuItem;

// Does nothing
public class IdentityMenuArranger extends MenuArranger {

  @Override
  public List<InventoryMenuItem> arrangeItems(List<InventoryMenuItem> itemsWithoutSpaces) {
    return itemsWithoutSpaces;
  }
}
