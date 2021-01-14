package tc.oc.pgm.util.menu;

import static com.google.common.base.Preconditions.checkArgument;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.menu.InventoryMenuUtils.howManyRows;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.menu.pattern.MenuArranger;
import tc.oc.pgm.util.text.TextTranslations;

/**
 * A way to make a GUI menu that users can interact with.
 *
 * <p>The {@link MenuArranger} is what decides whether a menu will be paginated, and how the items
 * will be scattered throughout the GUI. This means any list passed in the constructor of an {@link
 * InventoryMenu} should only have <b>non-{@code null}</b> elements.
 *
 * <p>Whenever an item in this inventory is clicked it will automatically call the items {@code
 * onInventoryClick} method. If that (or any) method changes something in the GUI {@link
 * #refreshWindow(Player)} or {@link #refreshAll()} should be called to rebuild the GUI for the
 * relevant players.
 */
public class InventoryMenu implements Listener {

  protected static final int ROW_WIDTH = 9; // Number of columns per row
  protected static final int MAX_ROWS = 6; // Max allowed row size

  private final List<InventoryMenuItem> inventoryMenuItems;

  private final WeakHashMap<Player, Boolean> viewing = new WeakHashMap<>();

  private final Component title; // Title of the inventory
  private int rows; // The # of rows in the inventory
  private final World world; // The world this inventory exists in;

  /**
   * InventoryMenu: An easy way to make an GUI menu that users can interact with.
   *
   * <p>Note: Code here was initially extracted from PickerMatchModule to allow for reuse
   *
   * @param world - The world this inventory should exist it
   * @param title - the inventory title
   * @param items - The items this inventory will contain, null counts as spaces
   * @param menuArranger arranges the items in different ways while also assisting pagination, is
   *     null when called by page items
   * @param rows - The maximum amount of rows the menu can have
   * @param pagesPossible internal boolean used to prevent infinite pages
   */
  private InventoryMenu(
      World world,
      Component title,
      List<InventoryMenuItem> items,
      @Nullable MenuArranger menuArranger,
      int rows,
      boolean pagesPossible) {
    this.title = title;
    this.rows = rows;
    this.inventoryMenuItems = applyPatternAndAddPages(items, menuArranger, pagesPossible);
    // This argument check needs to be under the applyPattern call because it can increase the row
    // number(pagination)
    checkArgument(rows > 0 && rows <= MAX_ROWS, "Row size must be between 1 and " + MAX_ROWS);
    this.world = world;
    enableInventory();
  }

  public InventoryMenu(
      World world,
      Component title,
      List<InventoryMenuItem> items,
      MenuArranger menuArranger,
      int rows) {
    this(world, title, items, menuArranger, rows, true);
  }

  public InventoryMenu(
      World world, Component title, List<InventoryMenuItem> items, MenuArranger menuArranger) {
    this(world, title, items, menuArranger, menuArranger.rows(), true);
  }

  private InventoryMenu(World world, Component title, List<InventoryMenuItem> items) {
    this(world, title, items, null, howManyRows(items), false);
  }

  public ItemStack[] createWindowContents(final Player player) {
    List<ItemStack> items = Lists.newArrayList();
    for (InventoryMenuItem item : this.inventoryMenuItems) {
      if (item == null) items.add(null);
      else items.add(item.createItem(player));
    }

    return items.toArray(new ItemStack[0]);
  }

  public String getTranslatedTitle(Player player) {
    return TextTranslations.translateLegacy(title, player);
  }

  public boolean isViewing(Player player) {
    return viewing.containsKey(player);
  }

  public void display(Player player) {
    this.showWindow(player);
    this.viewing.put(player, true);
  }

  public boolean remove(Player player) {
    return this.viewing.remove(player);
  }

  public void refreshAll() {
    viewing.keySet().forEach(this::refreshWindow);
  }

  private int getInventorySize() {
    return ROW_WIDTH * rows;
  }

  /**
   * Open the window for the given player, or refresh its contents if they already have it open, and
   * return the current contents.
   *
   * <p>If the window is currently open but too small to hold the current contents, it will be
   * closed and reopened.
   *
   * <p>If the player is not currently allowed to have the window open, close any window they have
   * open and return null.
   */
  private Inventory showWindow(Player player) {
    ItemStack[] contents = createWindowContents(player);
    Inventory inv = getOpenWindow(player);
    if (inv != null && inv.getSize() < contents.length) {
      inv = null;
      closeWindow(player);
    }
    if (inv == null) {
      inv = openWindow(player, contents);
    } else {
      inv.setContents(contents);
    }
    return inv;
  }

  /**
   * If the given player currently has the window open, refresh its contents and return the updated
   * inventory. The window will be closed and reopened if it is too small to hold the current
   * contents.
   *
   * <p>If the window is open but should be closed, close it and return null.
   *
   * <p>If the player does not have the window open, return null.
   */
  public @Nullable Inventory refreshWindow(Player player) {
    Inventory inv = getOpenWindow(player);
    if (inv != null) {
      ItemStack[] contents = createWindowContents(player);
      if (inv.getSize() < contents.length) {
        closeWindow(player);
        inv = openWindow(player, contents);
      } else {
        inv.setContents(contents);
      }
    }
    return inv;
  }

  /**
   * Return the inventory of the given player's currently open window, or null if the player does
   * not have the window open.
   */
  private @Nullable Inventory getOpenWindow(Player player) {
    if (isViewing(player)) {
      return player.getOpenInventory().getTopInventory();
    }
    return null;
  }

  /** Close any window that is currently open for the given player */
  private void closeWindow(Player player) {
    if (isViewing(player)) {
      player.closeInventory();
    }
  }

  /** Open a new window for the given player displaying the given contents */
  private Inventory openWindow(Player player, ItemStack[] contents) {
    closeWindow(player);
    Inventory inv =
        Bukkit.createInventory(
            player, getInventorySize(), StringUtils.truncate(getTranslatedTitle(player), 32));

    inv.setContents(contents);
    player.openInventory(inv);
    viewing.put(player, true);
    return inv;
  }

  @EventHandler
  public void onInventoryClick(final InventoryClickEvent event) {
    if (inventoryMenuItems == null
        || this.world != event.getWorld()
        || event.getCurrentItem() == null
        || event.getCurrentItem().getItemMeta() == null
        || event.getCurrentItem().getItemMeta().getDisplayName() == null) return;

    if (event.getWhoClicked() instanceof Player) {
      Player player = ((Player) event.getWhoClicked());
      if (isViewing(player)) {
        ItemStack clicked = event.getCurrentItem();
        for (InventoryMenuItem item : this.inventoryMenuItems) {
          if (item == null) continue;

          if (clicked.equals(item.createItem(player))) {
            item.onInventoryClick(this, player, event.getClick());
          }
        }
      }
    }
  }

  @EventHandler
  public void onWorldUnload(WorldUnloadEvent event) {
    if (this.world != event.getWorld()) return;
    disableInventory();
  }

  public void disableInventory() {
    viewing.keySet().forEach(this::closeWindow);
    viewing.clear();
    HandlerList.unregisterAll(this);
  }

  public void enableInventory() {
    Bukkit.getPluginManager().registerEvents(this, BukkitUtils.getPlugin());
  }

  private List<InventoryMenuItem> applyPatternAndAddPages(
      List<InventoryMenuItem> items, MenuArranger menuArranger, boolean pagesPossible) {

    List<InventoryMenuItem> mutableItems = new ArrayList<>(items);
    if (!pagesPossible) return items;

    // Quick exit if we dont need any pages
    if (menuArranger.automatedPaginationLimit() > mutableItems.size())
      return menuArranger.arrangeItems(mutableItems);

    // We need pages!!
    rows++;

    List<List<InventoryMenuItem>> pages = new ArrayList<>();

    // Put items into pages
    List<InventoryMenuItem> page = new ArrayList<>();
    for (int i = 0; !mutableItems.isEmpty(); i++) {
      page.add(mutableItems.remove(0));
      if (i + 1 == menuArranger.automatedPaginationLimit() || mutableItems.isEmpty()) { // new page
        pages.add(new ArrayList<>(menuArranger.arrangeItems(page)));
        page.clear();
        i = 0;
      }
    }

    // Insert pagination items on every page
    for (int i = 0; i < pages.size(); i++) {
      List<InventoryMenuItem> currentPage = pages.get(i);
      for (int item = 0; item < ROW_WIDTH; item++) currentPage.add(null);
      if (i > 0) // Is there a previous page?
      currentPage.set(
            2 + (ROW_WIDTH * (rows - 1)), new PageInventoryMenuItem(pages.get(i - 1), false));

      if (i < pages.size() - 1) // Is there a next page?
      currentPage.set(
            6 + (ROW_WIDTH * (rows - 1)), new PageInventoryMenuItem(pages.get(i + 1), true));
    }

    return pages.get(0);
  }

  private class PageInventoryMenuItem implements InventoryMenuItem {
    private final List<InventoryMenuItem> inventoryMenu;
    private final boolean next; // Does this represent the next page

    PageInventoryMenuItem(List<InventoryMenuItem> items, boolean next) {
      this.inventoryMenu = items;
      this.next = next;
    }

    @Override
    public Component getDisplayName() {
      return translatable(next ? "misc.nextPage" : "misc.previousPage", NamedTextColor.WHITE);
    }

    @Override
    public List<String> getLore(Player player) {
      return null;
    }

    @Override
    public Material getMaterial(Player player) {
      return Material.ARROW;
    }

    @Override
    public void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType) {
      new InventoryMenu(world, title, inventoryMenu).display(player);
    }
  }
}
