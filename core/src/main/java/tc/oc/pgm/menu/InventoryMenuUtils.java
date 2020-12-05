package tc.oc.pgm.menu;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.match.Match;

/** A collection of some static methods for building {@link InventoryMenu}s */
// TODO: Abstract out adding a row for pagination?
public class InventoryMenuUtils {

  /**
   * A {@link InventoryMenu} that fits any amount of {@link InventoryMenuItem}s Will always be 5
   * rows, fills in items in the top 4 rows, the bottom row is for page items(if necessary)
   *
   * @param match the match the inventory should exist in
   * @param title the title of the inventory
   * @param itemsWithoutSpaces items to put in this menu
   * @return a pretty menu that fits any amount of {@link InventoryMenuItem}s
   */
  public static InventoryMenu prettyMenu(
      Match match, Component title, List<InventoryMenuItem> itemsWithoutSpaces) {
    return prettyMenu(match, title, itemsWithoutSpaces, 0);
  }

  /** An internal implementation used for keeping track of pages */
  protected static InventoryMenu prettyMenu(
      Match match, Component inventoryTitle, List<InventoryMenuItem> itemsWithoutSpaces, int page) {
    final List<InventoryMenuItem> itemsWithSpaces = new ArrayList<>();

    final int offset = 18 * page;

    final int size = itemsWithoutSpaces.size() - offset; // The amount of items left to place

    if (size > 13)
      for (int i = 13; i < 18; i++) {
        itemsWithSpaces.add(size > i ? itemsWithoutSpaces.get(i + offset) : null);
        if (i != 17) itemsWithSpaces.add(null);
      }
    else emptyRow(itemsWithSpaces);

    if (size > 0)
      for (int i = 0; i < 4; i++) {
        itemsWithSpaces.add(null);
        itemsWithSpaces.add(size > i ? itemsWithoutSpaces.get(i + offset) : null);
        if (i == 3) itemsWithSpaces.add(null);
      }
    else emptyRow(itemsWithSpaces);

    if (size > 8)
      for (int i = 8; i < 13; i++) {
        itemsWithSpaces.add(size > i ? itemsWithoutSpaces.get(i + offset) : null);
        if (i != 12) itemsWithSpaces.add(null);
      }
    else emptyRow(itemsWithSpaces);

    if (size > 4)
      for (int i = 4; i < 8; i++) {
        itemsWithSpaces.add(null);
        itemsWithSpaces.add(size > i ? itemsWithoutSpaces.get(i + offset) : null);
        if (i == 7) itemsWithSpaces.add(null);
      }
    else emptyRow(itemsWithSpaces);

    itemsWithSpaces.add(null);
    itemsWithSpaces.add(null);
    itemsWithSpaces.add(
        page > 0
            ? new PageInventoryMenuItem(inventoryTitle, itemsWithoutSpaces, page, false)
            : null);
    itemsWithSpaces.add(null);
    itemsWithSpaces.add(null);
    itemsWithSpaces.add(null);
    itemsWithSpaces.add(
        itemsWithoutSpaces.size() > offset + 18 // Does the menu need another page?
            ? new PageInventoryMenuItem(inventoryTitle, itemsWithoutSpaces, page, true)
            : null);
    itemsWithSpaces.add(null);
    itemsWithSpaces.add(null);

    return new InventoryMenu(match, inventoryTitle, 5, itemsWithSpaces);
  }

  /**
   * A single row {@link InventoryMenu} that fits 0-4 {@link InventoryMenuItem}s Fills a pretty
   * pattern based on the amount of items it gets passed
   *
   * @param match the match the inventory should exist in
   * @param inventoryTitle the title of the inventory
   * @param itemsWithoutSpaces items to put in this menu
   * @return a small menu that fits 0-4 {@link InventoryMenuItem}s
   */
  public static InventoryMenu smallMenu(
      Match match, Component inventoryTitle, List<InventoryMenuItem> itemsWithoutSpaces) {
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

    return new InventoryMenu(match, inventoryTitle, 1, itemsWithSpaces);
  }

  /**
   * A 1-5 row {@link InventoryMenu} that fits 0-23 {@link InventoryMenuItem}s. Fills top to bottom
   * with 1 space between each item, only uses the required amount of rows
   *
   * @param match the match the inventory should exist in
   * @param inventoryTitle the title of the inventory
   * @param itemsWithoutSpaces items to put in this menu
   * @return a menu that fits 0-23 {@link InventoryMenuItem}s
   */
  public static InventoryMenu progressiveMenu(
      Match match, Component inventoryTitle, List<InventoryMenuItem> itemsWithoutSpaces) {
    final List<InventoryMenuItem> itemsWithSpaces = new ArrayList<>();

    int size = itemsWithoutSpaces.size();

    int rows = 1;
    for (int i = 0, slot = 0, itemsLeft = size - 1; -1 < itemsLeft; slot++, i++) {
      if (slot % 2 == 0) {
        itemsWithSpaces.add(rows % 2 == 0 ? null : itemsWithoutSpaces.get(itemsLeft));
      } else {
        itemsWithSpaces.add(rows % 2 == 0 ? itemsWithoutSpaces.get(itemsLeft) : null);
      }

      if (itemsWithSpaces.get(i) != null) itemsLeft--;

      if (slot == 8 && itemsLeft > -1) {
        slot = -1;
        rows++;
        if (rows == 7) break; // Ruediger overload
      }
    }

    return new InventoryMenu(match, inventoryTitle, rows, itemsWithSpaces);
  }

  /**
   * Simulates an empty row in a list that will be used to create a {@link InventoryMenu} (9 empty
   * slots)
   *
   * @param content The list you want an empty row in
   */
  public static void emptyRow(List<?> content) {
    for (int i = 0; i < 9; i++) {
      content.add(null);
    }
  }
}
