package tc.oc.pgm.util.menu;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import tc.oc.pgm.util.menu.item.InventoryItem;
import tc.oc.pgm.util.menu.item.InventoryItemBuilder;
import tc.oc.pgm.util.menu.item.ItemHolder;

public class InventoryMenuBuilder {

  private final InventoryMenuListener listener;
  private final int rows;
  private final List<ItemHolder> items;
  private Component name;

  /**
   * Creates a new inventory builder
   *
   * @param listener
   * @param rows the number of rows the inventory should have
   */
  public InventoryMenuBuilder(InventoryMenuListener listener, int rows) {
    this.listener = listener;
    this.rows = rows;
    this.name = TextComponent.empty();
    this.items = Lists.newArrayList();
  }

  /**
   * Adds an item to the {@link InventoryMenu} being built by this builder at the next free slot.
   *
   * @param builder the builder of the item to add
   * @return this builder
   */
  public InventoryMenuBuilder addItem(InventoryItemBuilder builder) {
    return addItem(builder.build());
  }

  /**
   * Adds an item to the {@link InventoryMenu} at the next free slot
   *
   * @param item the item to add
   * @return this builder
   */
  public InventoryMenuBuilder addItem(InventoryItem item) {
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < 9; x++) {
        boolean matched = false;
        for (ItemHolder itemHolder : items) {
          if (itemHolder.x == x && itemHolder.y == y) {
            matched = true;
            break;
          }
        }

        if (!matched) {
          return addItem(y, x, item);
        }
      }
    }

    throw new IllegalStateException("Ran out of space in inventory!");
  }

  /**
   * Adds an item to the {@link InventoryMenu} being built by this builder at a certain coordinate
   *
   * @param y the y coordinate to add the item
   * @param x the x coordinate to add the item
   * @param builder the builder of the item to add
   * @return this builder
   */
  public InventoryMenuBuilder addItem(int y, int x, InventoryItemBuilder builder) {
    return addItem(y, x, builder.build());
  }

  /**
   * Adds an item to the {@link InventoryMenu} being built by this builder at a certain coordinate
   *
   * @param y the y coordinate to add the item
   * @param x the x coordinate to add the item
   * @param item the item to add add to the inventory
   * @return this builder
   */
  public InventoryMenuBuilder addItem(int y, int x, InventoryItem item) {
    items.add(new ItemHolder(y, x, item));
    return this;
  }

  /**
   * Sets the name of the {@link InventoryMenu}
   *
   * @param name the name to set it to
   * @return this builder
   */
  public InventoryMenuBuilder setName(Component name) {
    this.name = name;
    return this;
  }

  /**
   * Constructs the {@link InventoryMenu} that this builder has been building
   *
   * @return the constructed inventory
   */
  public InventoryMenu build() {
    return new InventoryMenuImpl(listener, items, rows, name);
  }
}
