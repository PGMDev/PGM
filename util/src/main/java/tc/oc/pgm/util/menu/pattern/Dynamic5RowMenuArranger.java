package tc.oc.pgm.util.menu.pattern;

import java.util.List;
import tc.oc.pgm.util.menu.InventoryMenuItem;
import tc.oc.pgm.util.menu.InventoryMenuUtils;

public class Dynamic5RowMenuArranger extends MenuArranger {

  @Override
  public List<InventoryMenuItem> arrangeItems(List<InventoryMenuItem> itemsWithoutSpaces) {

    // Figure out which slots to use
    int[] slots = new int[Math.min(itemsWithoutSpaces.size(), 18)];

    switch (slots.length) {
      case 1:
        slots[0] = 22;
        break;
      case 2:
        slots[0] = 20;
        slots[1] = 24;
        break;
      case 3:
        slots[0] = 20;
        slots[1] = 22;
        slots[2] = 24;
        break;
      case 4:
        slots[0] = 11;
        slots[1] = 15;
        slots[2] = 29;
        slots[3] = 33;
        break;
      case 5:
        slots[0] = 11;
        slots[1] = 15;
        slots[2] = 22;
        slots[3] = 29;
        slots[4] = 33;
        break;
      case 6:
        slots[0] = 11;
        slots[1] = 13;
        slots[2] = 15;
        slots[3] = 29;
        slots[4] = 31;
        slots[5] = 33;
        break;
      case 7:
        slots[0] = 11;
        slots[1] = 15;
        slots[2] = 20;
        slots[3] = 22;
        slots[4] = 24;
        slots[5] = 29;
        slots[6] = 33;
        break;
      case 8:
        slots[0] = 10;
        slots[1] = 12;
        slots[2] = 14;
        slots[3] = 16;
        slots[4] = 28;
        slots[5] = 30;
        slots[6] = 32;
        slots[7] = 34;
        break;
      case 9:
        slots[0] = 11;
        slots[1] = 13;
        slots[2] = 15;
        slots[3] = 19;
        slots[4] = 22;
        slots[5] = 25;
        slots[6] = 29;
        slots[7] = 31;
        slots[8] = 33;
        break;
      case 10:
        slots[0] = 11;
        slots[1] = 13;
        slots[2] = 15;
        slots[3] = 19;
        slots[4] = 21;
        slots[5] = 23;
        slots[6] = 25;
        slots[7] = 29;
        slots[8] = 31;
        slots[9] = 33;
        break;
      case 11:
        slots[0] = 10;
        slots[1] = 12;
        slots[2] = 14;
        slots[3] = 16;
        slots[4] = 20;
        slots[5] = 22;
        slots[6] = 24;
        slots[7] = 28;
        slots[8] = 30;
        slots[9] = 32;
        slots[10] = 34;
        break;
      case 12:
        slots[0] = 4;
        slots[1] = 12;
        slots[2] = 13;
        slots[3] = 14;
        slots[4] = 20;
        slots[5] = 22;
        slots[6] = 24;
        slots[7] = 28;
        slots[8] = 30;
        slots[9] = 31;
        slots[10] = 32;
        slots[11] = 34;
        break;
      case 13:
        slots[0] = 10;
        slots[1] = 12;
        slots[2] = 14;
        slots[3] = 16;
        slots[4] = 18;
        slots[5] = 20;
        slots[6] = 22;
        slots[7] = 24;
        slots[8] = 26;
        slots[9] = 28;
        slots[10] = 30;
        slots[11] = 32;
        slots[12] = 34;
        break;
      case 14:
        slots[0] = 9;
        slots[1] = 11;
        slots[2] = 13;
        slots[3] = 15;
        slots[4] = 17;
        slots[5] = 19;
        slots[6] = 21;
        slots[7] = 23;
        slots[8] = 25;
        slots[9] = 27;
        slots[10] = 29;
        slots[11] = 31;
        slots[12] = 33;
        slots[13] = 35;
        break;
      case 15:
        slots[0] = 4;
        slots[1] = 12;
        slots[2] = 14;
        slots[3] = 20;
        slots[4] = 21;
        slots[5] = 22;
        slots[6] = 23;
        slots[7] = 24;
        slots[8] = 28;
        slots[9] = 29;
        slots[10] = 30;
        slots[11] = 31;
        slots[12] = 32;
        slots[13] = 33;
        slots[14] = 34;
        break;
      case 16:
        slots[0] = 4;
        slots[1] = 12;
        slots[2] = 13;
        slots[3] = 14;
        slots[4] = 20;
        slots[5] = 21;
        slots[6] = 22;
        slots[7] = 23;
        slots[8] = 24;
        slots[9] = 28;
        slots[10] = 29;
        slots[11] = 30;
        slots[12] = 31;
        slots[13] = 32;
        slots[14] = 33;
        slots[15] = 34;
        break;
      case 17:
        slots[0] = 10;
        slots[1] = 11;
        slots[2] = 12;
        slots[3] = 13;
        slots[4] = 14;
        slots[5] = 15;
        slots[6] = 16;
        slots[7] = 21;
        slots[8] = 22;
        slots[9] = 23;
        slots[10] = 28;
        slots[11] = 29;
        slots[12] = 30;
        slots[13] = 31;
        slots[14] = 32;
        slots[15] = 33;
        slots[16] = 34;
        break;
      case 18:
        slots[0] = 0;
        slots[1] = 2;
        slots[2] = 4;
        slots[3] = 6;
        slots[4] = 8;
        slots[5] = 10;
        slots[6] = 12;
        slots[7] = 14;
        slots[8] = 16;
        slots[9] = 18;
        slots[10] = 20;
        slots[11] = 22;
        slots[12] = 24;
        slots[13] = 26;
        slots[14] = 28;
        slots[15] = 30;
        slots[16] = 32;
        slots[17] = 34;
        break;
    }

    // Put items in the slots, this menu will always have 5 rows
    return InventoryMenuUtils.itemsInSlots(itemsWithoutSpaces, slots, 5);
  }

  @Override // Only fits 18 items per screen
  public int automatedPaginationLimit() {
    return 18;
  }
}
