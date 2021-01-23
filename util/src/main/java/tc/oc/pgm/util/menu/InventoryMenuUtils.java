package tc.oc.pgm.util.menu;

import java.util.ArrayList;
import java.util.List;

/** A collection of some static methods for building {@link InventoryMenu}s */
public class InventoryMenuUtils {

  /**
   * Simulates an empty row in a list that will be used to create a {@link InventoryMenu} (9 empty
   * slots)
   *
   * @param content The list you want an empty row in
   */
  public static List<InventoryMenuItem> putEmptyRow(List<InventoryMenuItem> content) {
    for (int i = 0; i < 9; i++) {
      content.add(null);
    }
    return content;
  }

  public static List<InventoryMenuItem> emptyRow() {
    List<InventoryMenuItem> row = new ArrayList<>();
    return putEmptyRow(row);
  }

  // Puts the given items in the given slots
  public static List<InventoryMenuItem> itemsInSlots(
      List<InventoryMenuItem> items, int[] slots, int rows) {
    if (items.isEmpty()) return emptyRow();
    List<InventoryMenuItem> inventory = new ArrayList<>();

    int highestSlot = rows * InventoryMenu.ROW_WIDTH;

    for (int i = 0, j = 0; i < highestSlot; i++) {
      boolean placeItem = false;

      for (int slot : slots) {
        if (i == slot) {
          placeItem = true;
          break;
        }
      }

      if (placeItem) {
        inventory.add(items.get(j));
        j++;
      } else inventory.add(null);
    }

    return inventory;
  }

  // Figures out the smallest amount of required rows for the given menu
  static int howManyRows(List<InventoryMenuItem> menu) {
    int rows = 0;
    int size = menu.size();
    do {
      rows++;
      size -= 9;
    } while (size > 0);

    return rows;
  }
}
