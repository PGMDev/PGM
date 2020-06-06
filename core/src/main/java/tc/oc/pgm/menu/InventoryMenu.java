package tc.oc.pgm.menu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.kyori.text.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.observers.ObserverToolsMatchModule;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.text.TextTranslations;

public abstract class InventoryMenu {

  protected static final int ROW_WIDTH = 9; // Number of columns per row
  protected static final int MAX_ROWS = 6; // Max allowed row size

  private final WeakHashMap<MatchPlayer, InventoryMenu> viewing =
      new WeakHashMap<>(); // Map of players who are viewing the gui, along with the menu
  private final String title; // Title of the inventory
  private final int rows; // The # of rows in the inventory

  /**
   * InventoryMenu: An easy way to make an GUI menu that users can interact with.
   *
   * <p>See {@link ObserverToolsMatchModule} for an example on implementation
   *
   * <p>Note: Code here was extracted from PickerMatchModule to allow for reuse
   *
   * @param title - A string that will be translated and made the inventory title
   * @param rows - The amount of rows the inventory will be created with
   */
  public InventoryMenu(String title, int rows) {
    checkArgument(rows > 0 && rows <= MAX_ROWS, "Row size must be between 1 and " + MAX_ROWS);
    this.title = checkNotNull(title);
    this.rows = rows;
  }

  /** Defines how the GUI will display the layout */
  public abstract ItemStack[] createWindowContents(final MatchPlayer player);

  public String getTranslatedTitle(MatchPlayer player) {
    return TextTranslations.translateLegacy(TranslatableComponent.of(title), player.getBukkit());
  }

  public boolean isViewing(MatchPlayer player) {
    return viewing.containsKey(player);
  }

  public void display(MatchPlayer player) {
    this.showWindow(player);
    this.viewing.put(player, this);
  }

  public void remove(MatchPlayer player) {
    this.viewing.remove(player);
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
  private @Nullable Inventory showWindow(MatchPlayer player) {
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
}
