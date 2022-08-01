package tc.oc.pgm.shops.menu;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import com.google.common.collect.Lists;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.shops.Shop;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.text.TextTranslations;

public class ShopMenu extends InventoryMenu {

  private Shop shop;
  private Category category;
  private ClickableItem[] categories;
  private int highlightSlot = 1;

  public ShopMenu(Shop shop, MatchPlayer viewer) {
    super(text(colorize(shop.getName())), 6, viewer, null);
    this.shop = shop;
    this.categories = getCategoryItems();
    open();
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    render(player, contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    render(player, contents);
  }

  private void render(Player player, InventoryContents contents) {
    this.renderHeader(contents);
    this.renderIcons(contents);
  }

  private void renderHeader(InventoryContents contents) {
    Pagination page = contents.pagination();
    page.setItems(categories);
    page.setItemsPerPage(7);
    page.addToIterator(
        contents.newIterator(SlotIterator.Type.HORIZONTAL, 0, categories.length == 1 ? 4 : 1));

    // Previous button
    if (!page.isFirst()) contents.set(0, 0, getPageItem(getBukkit(), page.getPage() - 1, false));

    // Next button
    if (!page.isLast()) contents.set(0, 8, getPageItem(getBukkit(), page.getPage() + 1, true));

    if (categories.length == 1) {
      this.highlightSlot = 4;
    }

    // Menu divider & highlight
    contents.fillRow(1, getMenuSeperator(DyeColor.GRAY));
    contents.set(1, highlightSlot, getMenuSeperator(DyeColor.GREEN));
  }

  private void renderIcons(InventoryContents contents) {
    for (int i = 2; i < 4; i++) {
      contents.fillRow(i, null);
    }

    int row = 2;
    int col = 1;
    for (Icon icon : getCategory().getIcons()) {
      contents.set(row, col, getPurchasableItem(icon));
      col++;
      if (col > 7) {
        col = 1;
        row++;
      }
    }
  }

  private Category getCategory() {
    if (category == null) {
      category = shop.getCategories().get(0);
    }

    return category;
  }

  private void setCategory(Category category, int slot) {
    this.category = category;
    this.highlightSlot = slot;
  }

  private ClickableItem[] getCategoryItems() {
    ClickableItem[] items = new ClickableItem[shop.getCategories().size()];
    for (int i = 0; i < shop.getCategories().size(); i++) {
      items[i] = getCategoryItem(shop.getCategories().get(i));
    }
    return items;
  }

  private ClickableItem getCategoryItem(Category category) {
    return ClickableItem.of(category.getCategoryIcon(), c -> setCategory(category, c.getSlot()));
  }

  private ClickableItem getMenuSeperator(DyeColor color) {
    return ClickableItem.empty(
        new ItemBuilder()
            .material(Material.STAINED_GLASS_PANE)
            .color(color)
            .name(" ")
            .flags(ItemFlag.values())
            .build());
  }

  private ClickableItem getPurchasableItem(Icon icon) {
    boolean canPurchase = shop.canPurchase(icon, getViewer());
    Component materialName = text(getMaterial(icon.getCurrency())).color(NamedTextColor.GOLD);
    NamedTextColor purchaseColor = canPurchase ? NamedTextColor.GREEN : NamedTextColor.RED;
    Component cost =
        text()
            .append(translatable("shop.lore.cost", NamedTextColor.GRAY))
            .append(text(": ", NamedTextColor.DARK_GRAY))
            .append(text(icon.getPrice(), purchaseColor))
            .append(space())
            .append(materialName)
            .build();
    Component click =
        translatable("shop.lore." + (canPurchase ? "purchase" : "insufficient"), purchaseColor);

    String costLore = TextTranslations.translateLegacy(cost, getBukkit());
    String clickLore = TextTranslations.translateLegacy(click, getBukkit());

    ItemStack item = icon.getItem().clone();
    ItemMeta meta = item.getItemMeta();
    List<String> lore = Lists.newArrayList();
    if (meta.getLore() != null) {
      lore.addAll(meta.getLore());
    }
    lore.add(costLore);
    lore.add(clickLore);
    meta.setLore(lore);
    meta.addItemFlags(ItemFlag.values());
    item.setItemMeta(meta);

    return ClickableItem.of(
        item,
        c -> {
          shop.purchase(icon, getViewer());
          getViewer().getBukkit().updateInventory();
        });
  }

  private ClickableItem getPageItem(Player player, int page, boolean next) {
    return ClickableItem.of(
        getPageIcon(page, next),
        c -> {
          getInventory().open(player, page);
          this.highlightSlot = 1;
        });
  }

  private final ItemStack getPageIcon(int page, boolean next) {
    return new ItemBuilder()
        .material(Material.ARROW)
        .amount(page)
        .name(
            ChatColor.YELLOW
                + TextTranslations.translate(
                    "menu.page." + (next ? "next" : "previous"), getBukkit()))
        .flags(ItemFlag.values())
        .build();
  }

  private String getMaterial(Material material) {
    return WordUtils.capitalizeFully(material.name().toLowerCase().replace("_", " "));
  }
}
