package tc.oc.pgm.menu;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.observers.ObserverToolsMatchModule;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.text.TextTranslations;

public class InventoryMenu implements Listener {

  protected static final int ROW_WIDTH = 9; // Number of columns per row
  protected static final int MAX_ROWS = 6; // Max allowed row size

  private final List<InventoryMenuItem> inventoryMenuItems;

  private final WeakHashMap<MatchPlayer, InventoryMenu> viewing =
      new WeakHashMap<>(); // Map of players who are viewing the gui, along with the menu
  private final Component title; // Title of the inventory
  private final int rows; // The # of rows in the inventory
  private final Match match; // The match this inventory exists in;

  /**
   * InventoryMenu: An easy way to make an GUI menu that users can interact with.
   *
   * <p>See {@link ObserverToolsMatchModule} for an example on implementation
   *
   * <p>Note: Code here was initially extracted from PickerMatchModule to allow for reuse
   *
   * @param match - The match this inventory should exist it
   * @param title - the inventory title
   * @param rows - The amount of rows the inventory will be created with
   * @param items - The items this inventory will contain
   */
  public InventoryMenu(Match match, Component title, int rows, List<InventoryMenuItem> items) {
    checkArgument(rows > 0 && rows <= MAX_ROWS, "Row size must be between 1 and " + MAX_ROWS);
    this.title = title;
    this.rows = rows;
    this.inventoryMenuItems = items;
    this.match = match;
    match.addListener(this, MatchScope.LOADED);
  }

  public ItemStack[] createWindowContents(final MatchPlayer player) {
    List<ItemStack> items = Lists.newArrayList();
    for (InventoryMenuItem item : this.inventoryMenuItems) {
      if (item == null) items.add(null);
      else items.add(item.createItem(player));
    }

    return items.toArray(new ItemStack[items.size()]);
  }

  public String getTranslatedTitle(MatchPlayer player) {
    return TextTranslations.translateLegacy(title, player.getBukkit());
  }

  public boolean isViewing(MatchPlayer player) {
    return viewing.containsKey(player);
  }

  public void display(MatchPlayer player) {
    this.showWindow(player);
    this.viewing.put(player, this);
  }

  public boolean remove(MatchPlayer player) {
    return this.viewing.remove(player) != null;
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
  private Inventory showWindow(MatchPlayer player) {
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
  public @Nullable Inventory refreshWindow(MatchPlayer player) {
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
  private @Nullable Inventory getOpenWindow(MatchPlayer player) {
    if (isViewing(player)) {
      return player.getBukkit().getOpenInventory().getTopInventory();
    }
    return null;
  }

  /** Close any window that is currently open for the given player */
  private void closeWindow(MatchPlayer player) {
    if (isViewing(player)) {
      player.getBukkit().closeInventory();
    }
  }

  /** Open a new window for the given player displaying the given contents */
  private Inventory openWindow(MatchPlayer player, ItemStack[] contents) {
    closeWindow(player);
    Inventory inv =
        Bukkit.createInventory(
            player.getBukkit(),
            getInventorySize(),
            StringUtils.truncate(getTranslatedTitle(player), 32));

    inv.setContents(contents);
    player.getBukkit().openInventory(inv);
    viewing.put(player, this);
    return inv;
  }

  @EventHandler
  public void onInventoryClick(final InventoryClickEvent event) {
    if (inventoryMenuItems == null) return;
    if (event.getCurrentItem() == null
        || event.getCurrentItem().getItemMeta() == null
        || event.getCurrentItem().getItemMeta().getDisplayName() == null) return;

    if (event.getWhoClicked() instanceof Player) {
      MatchPlayer player = match.getPlayer(event.getWhoClicked());
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
  public void onInventoryClose(final InventoryCloseEvent event) {
    // Remove viewing of menu upon inventory close
    MatchPlayer player = match.getPlayer(event.getPlayer());
    this.remove(player);
  }
}
