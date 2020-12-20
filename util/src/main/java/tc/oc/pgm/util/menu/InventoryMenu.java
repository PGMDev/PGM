package tc.oc.pgm.util.menu;

import static com.google.common.base.Preconditions.checkArgument;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.menu.InventoryMenuUtils.howManyRows;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.util.WeakCollection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.menu.pattern.MenuArranger;
import tc.oc.pgm.util.text.TextTranslations;

public class InventoryMenu implements Listener {

  protected static final int ROW_WIDTH = 9; // Number of columns per row
  protected static final int MAX_ROWS = 6; // Max allowed row size

  private final List<InventoryMenuItem> inventoryMenuItems;

  private final WeakCollection<Player> viewing = new WeakCollection<>();

  private final Component title; // Title of the inventory
  private final int rows; // The # of rows in the inventory
  private final World world; // The world this inventory exists in;

  /**
   * InventoryMenu: An easy way to make an GUI menu that users can interact with.
   *
   * <p>Note: Code here was initially extracted from PickerMatchModule to allow for reuse
   *
   * @param world - The world this inventory should exist it
   * @param title - the inventory title
   * @param items - The items this inventory will contain, null counts as spaces
   */
  public InventoryMenu(
      World world, Component title, List<InventoryMenuItem> items, MenuArranger menuArranger) {
    this.title = title;
    this.inventoryMenuItems = applyPatternAndAddPages(items, menuArranger);
    this.rows = howManyRows(inventoryMenuItems);
    checkArgument(rows > 0 && rows <= MAX_ROWS, "Row size must be between 1 and " + MAX_ROWS);
    this.world = world;
    enableInventory();
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
    return viewing.contains(player);
  }

  public void display(Player player) {
    this.showWindow(player);
    this.viewing.add(player);
  }

  public boolean remove(Player player) {
    return this.viewing.remove(player);
  }

  public void refreshAll() {
    viewing.forEach(this::refreshWindow);
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
    viewing.add(player);
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
  public void onInventoryClose(final InventoryCloseEvent event) {
    // Remove viewing of menu upon inventory close
    Player player = (Player) event.getPlayer();
    this.remove(player);
  }

  @EventHandler
  public void onWorldUnload(WorldUnloadEvent event) {
    if (this.world != event.getWorld()) return;
    disableInventory();
  }

  public void disableInventory() {
    viewing.forEach(this::closeWindow);
    viewing.clear();
    HandlerList.unregisterAll(this);
  }

  public void enableInventory() {
    Bukkit.getPluginManager().registerEvents(this, BukkitUtils.getPlugin());
  }

  private List<InventoryMenuItem> applyPatternAndAddPages(
      List<InventoryMenuItem> items, MenuArranger menuArranger) {
    // Quick exit if we dont need any pages
    if (menuArranger.automatedPaginationLimit() >= items.size())
      return menuArranger.arrangeItems(items);

    List<List<InventoryMenuItem>> pages = new ArrayList<>();

    List<InventoryMenuItem> page = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) {
      page.add(items.remove(0));
      if (i == menuArranger.automatedPaginationLimit()) pages.add(new ArrayList<>(page));
      page.clear();
      i = 0;
    }

    for (int i = 0; i < pages.size(); i++) {
      List<InventoryMenuItem> currentPage = pages.get(i);
      currentPage.add(null);
      currentPage.add(null);
      currentPage.add(
          i == 0 ? null : new PageInventoryMenuItem(pages.get(i - 1), menuArranger, false));
      currentPage.add(null);
      currentPage.add(null);
      currentPage.add(null);
      currentPage.add(
          pages.size() - 1 == i
              ? null
              : new PageInventoryMenuItem(pages.get(i + 1), menuArranger, true));
      currentPage.add(null);
      currentPage.add(null);
    }

    return pages.get(0);
  }

  private class PageInventoryMenuItem implements InventoryMenuItem {
    private final InventoryMenu inventoryMenu;
    private final boolean next; // Does this represent the next page

    PageInventoryMenuItem(List<InventoryMenuItem> items, MenuArranger menuArranger, boolean next) {
      this.inventoryMenu = new InventoryMenu(world, title, items, menuArranger);
      this.next = next;
    }

    @Override
    public Component getName() {
      return translatable(next ? "misc.nextPage" : "misc.previousPage");
    }

    @Override
    public ChatColor getColor() {
      return ChatColor.WHITE;
    }

    @Override
    public List<String> getLore(Player player) {
      return null;
    }

    @Override
    public Material getMaterial(Player player) {
      return Material.SKULL_ITEM;
    }

    @Override
    public void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType) {
      inventoryMenu.display(player);
    }

    @Override
    public ItemStack createItem(Player player) {
      ItemStack stack = new ItemStack(getMaterial(player), 1, (byte) 3);
      SkullMeta meta = (SkullMeta) stack.getItemMeta();

      meta.setOwner("MHF_" + (next ? "ArrowRight" : "ArrowLeft"));

      meta.setDisplayName(
          getColor()
              + ChatColor.BOLD.toString()
              + TextTranslations.translateLegacy(getName(), player));
      meta.setLore(getLore(player));
      meta.addItemFlags(ItemFlag.values());

      stack.setItemMeta(meta);

      return stack;
    }
  }
}