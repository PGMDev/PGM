package tc.oc.pgm.gui;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.util.StringUtils;

public abstract class InventoryGUI {

  /*
   * Code extracted from PickerMatchModule to allow for reuse
   */

  public Set<MatchPlayer> viewing = Sets.newHashSet();

  public String title; // Title of the inventory
  private int size; // Size of inventory

  public InventoryGUI(String title, int size) {
    this.title = title;
    this.size = size;
  }

  /** Defines how the GUI will display the layout */
  public abstract ItemStack[] createWindowContents(final MatchPlayer player);

  public String getTranslatedTitle(MatchPlayer player) {
    return ComponentRenderers.toLegacyText(new PersonalizedTranslatable(title), player.getBukkit());
  }

  public boolean isViewing(MatchPlayer player) {
    return viewing.contains(player);
  }

  public void display(MatchPlayer player) {
    this.showWindow(player);
    this.viewing.add(player);
  }

  public void remove(MatchPlayer player) {
    this.viewing.remove(player);
  }

  public void refreshAll() {
    viewing.forEach(this::refreshWindow);
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
            player.getBukkit(), size, StringUtils.truncate(getTranslatedTitle(player), 32));

    inv.setContents(contents);
    player.getBukkit().openInventory(inv);
    viewing.add(player);
    return inv;
  }
}
