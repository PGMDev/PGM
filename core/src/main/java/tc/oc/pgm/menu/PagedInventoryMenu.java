package tc.oc.pgm.menu;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextTranslations.translateLegacy;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import fr.minuskube.inv.content.SlotPos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.ItemBuilder;

/** A specialized inventory menu for easy pagination * */
public abstract class PagedInventoryMenu extends InventoryMenu {

  private static final Material PAGE_MATERIAL = Material.ARROW;

  private final int itemsPerPage;
  private final int startingRow;
  private final int startingCol;

  public PagedInventoryMenu(
      Component title,
      int rows,
      MatchPlayer viewer,
      SmartInventory parent,
      int itemsPerPage,
      int startingRow,
      int startingCol) {
    super(title, rows, viewer, parent);
    this.itemsPerPage = itemsPerPage;
    this.startingRow = startingRow;
    this.startingCol = startingCol;
  }

  /**
   * Automatic setup of page contents
   *
   * <p>To enable paged setup call in {@link #init(Player, InventoryContents)}
   *
   * @param player {@link Player} viewing the inventory
   * @param contents {@link InventoryContents} of menu
   */
  public void setupPageContents(Player player, InventoryContents contents) {
    ClickableItem[] items = getPageContents(player);

    // If no items are found, display empty contents button
    if (items == null || items.length == 0) {
      contents.set(getEmptyPageSlot(), getEmptyContentsButton(player));
      return;
    }

    // Setup pagination
    Pagination page = contents.pagination();
    page.setItems(items);
    page.setItemsPerPage(itemsPerPage);
    page.addToIterator(
        contents.newIterator(SlotIterator.Type.HORIZONTAL, startingRow, startingCol));

    // Previous button
    if (!page.isFirst()) {
      contents.set(
          getPreviousPageSlot(), getPageItem(player, page.getPage() - 1, "menu.page.previous"));
    }

    // Next button
    if (!page.isLast()) {
      contents.set(getNextPageSlot(), getPageItem(player, page.getPage() + 1, "menu.page.next"));
    }
  }

  /**
   * The position of the previous page button.
   *
   * @return a {@link SlotPos} where previous page button will be located.
   */
  public SlotPos getPreviousPageSlot() {
    return SlotPos.of(lastRow(), 0);
  }

  /**
   * The position of the next page button.
   *
   * @return a {@link SlotPos} where next page button will be located.
   */
  public SlotPos getNextPageSlot() {
    return SlotPos.of(lastRow(), 8);
  }

  /**
   * The position of the no page contents button, only displayed when page contents is empty or
   * null.
   *
   * @see #getPageContents(Player)
   * @return a {@link SlotPos} where no page button will be located.
   */
  public abstract SlotPos getEmptyPageSlot();

  /**
   * An array of {@link ClickableItem}s that will populate the paged inventory
   *
   * @param viewer The {@link Player} viewer who each item will be rendered for
   * @return an array of clickable items
   */
  public abstract ClickableItem[] getPageContents(Player viewer);

  protected ClickableItem getEmptyContentsButton(Player viewer) {
    Component name = translatable("menu.page.empty", NamedTextColor.DARK_RED, TextDecoration.BOLD);
    return ClickableItem.empty(new ItemBuilder()
        .material(Material.BARRIER)
        .name(translateLegacy(name, viewer))
        .build());
  }

  protected ClickableItem getPageItem(Player player, int page, String key) {
    Component text = translatable(key, NamedTextColor.YELLOW, TextDecoration.BOLD);
    return ClickableItem.of(
        getPageIcon(translateLegacy(text, player), page + 1),
        c -> getInventory().open(player, page));
  }

  private final ItemStack getPageIcon(String text, int page) {
    return getNamedItem(text, PAGE_MATERIAL, page);
  }

  private ItemStack getNamedItem(String text, Material material, int amount) {
    return new ItemBuilder()
        .material(material)
        .amount(amount)
        .name(BukkitUtils.colorize(text))
        .flags(ItemFlag.values())
        .build();
  }
}
