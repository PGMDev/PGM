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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import tc.oc.pgm.kits.tag.ItemModifier;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.shops.Shop;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public class ShopMenu extends InventoryMenu {

  private static final String SYMBOL_SUFFICIENT = "\u2714 "; // ✔
  private static final String SYMBOL_INSUFFICIENT = "\u2715 "; // ✕

  private final Shop shop;
  private final List<CategoryItem> allCategories;
  private Category category;
  private ClickableItem[] categories;
  private int highlight;

  public ShopMenu(Shop shop, MatchPlayer viewer) {
    super(text(colorize(shop.getName())), 6, viewer, null);
    this.shop = shop;
    this.allCategories = shop.getCategories().stream().map(CategoryItem::new).toList();

    updateCategories();
    if (category == null) return;
    this.highlight = getStartingIndex();
    open();
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    render(contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    render(contents);
  }

  private void render(InventoryContents contents) {
    this.renderHeader(contents);
    this.renderIcons(contents);
  }

  private int getStartingIndex() {
    return categories.length == 1 ? 4 : (categories.length <= 9 ? 0 : 1);
  }

  private boolean isPaginated() {
    return categories.length > 9;
  }

  private void renderHeader(InventoryContents contents) {
    if (this.updateCategories()) {
      contents.fillRow(0, null);
    }

    if (isPaginated()) {
      Pagination page = contents.pagination();
      page.setItems(categories);
      page.setItemsPerPage(7);
      page.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 0, getStartingIndex()));

      // Previous button
      if (!page.isFirst()) contents.set(0, 0, getPageItem(getBukkit(), page.getPage() - 1, false));

      // Next button
      if (!page.isLast()) contents.set(0, 8, getPageItem(getBukkit(), page.getPage() + 1, true));
    } else {

      // Center icon if only one category is present
      if (categories.length == 1) {
        contents.set(0, 4, categories[0]);
      } else {
        for (int i = 0; i < categories.length; i++) {
          contents.set(0, i, categories[i]);
        }
      }
    }

    // Menu divider & highlight
    contents.fillRow(1, getMenuSeperator(DyeColor.GRAY));
    contents.set(1, Math.max(0, Math.min(8, highlight)), getMenuSeperator(DyeColor.GREEN));
  }

  private void renderIcons(InventoryContents contents) {
    List<Icon> icons = getCategory().getVisibleIcons(getViewer());

    for (int i = 2; i < 6; i++) {
      contents.fillRow(i, null);
    }

    if (icons.isEmpty()) {
      contents.set(4, 4, getNoItemsItem());
      return;
    }

    int row = 2;
    int col = 1;
    for (Icon icon : icons) {
      contents.set(row, col, getPurchasableItem(icon));
      col++;
      if (col > 7) {
        col = 1;
        row++;
      }
    }
  }

  private Category getCategory() {
    return category;
  }

  private void setCategory(Category category, int slot) {
    this.category = category;
    this.highlight = slot;
  }

  private boolean updateCategories() {
    boolean anyChanged = false;
    for (CategoryItem category : allCategories) {
      if (category.changed()) anyChanged = true;
    }
    if (categories != null && !anyChanged) return false;

    List<ClickableItem> newCategories = new ArrayList<>(allCategories.size());
    Category first = null;
    int curr = 0, active = -1;
    for (CategoryItem category : allCategories) {
      if (!category.isActive()) continue;
      if (first == null) first = category.category;
      if (category.category == this.category) active = curr;

      newCategories.add(category.item);
      curr++;
    }
    this.categories = newCategories.toArray(ClickableItem[]::new);

    if (newCategories.isEmpty()) {
      getViewer().sendWarning(translatable("shop.category.empty"));
      close();
      return true;
    }

    // Update selected category if it became inactive
    if (active != -1) setCategory(category, getStartingIndex() + active);
    else setCategory(first, getStartingIndex());
    return true;
  }

  private ClickableItem getMenuSeperator(DyeColor color) {
    return ClickableItem.empty(new ItemBuilder()
        .material(Materials.STAINED_GLASS_PANE)
        .color(color)
        .name(" ")
        .flags(ItemFlag.values())
        .build());
  }

  private ClickableItem getNoItemsItem() {
    return ClickableItem.empty(new ItemBuilder()
        .material(Material.BARRIER)
        .name(getBukkit(), translatable("shop.item.empty", NamedTextColor.RED))
        .flags(ItemFlag.values())
        .build());
  }

  private ClickableItem getPurchasableItem(Icon icon) {
    boolean canPurchase = icon.canPurchase(getViewer());
    NamedTextColor purchaseColor = canPurchase ? NamedTextColor.GREEN : NamedTextColor.RED;

    List<Component> price = Lists.newArrayList();

    if (icon.isFree()) {
      // Free item
      price.add(translatable("shop.lore.free", NamedTextColor.GREEN));
    } else {
      price = icon.getPayments().stream()
          .map(p -> {
            boolean hasPayment = p.hasPayment(getViewer().getInventory());

            Component currencyName = p.getItem() != null
                ? text(p.getItem().getItemMeta().getDisplayName())
                : text(getMaterial(p.getCurrency())).color(TextFormatter.convert(p.getColor()));

            Component prefix = icon.getPayments().size() == 1
                ? null
                : text(
                    hasPayment ? SYMBOL_SUFFICIENT : SYMBOL_INSUFFICIENT,
                    hasPayment ? NamedTextColor.GREEN : NamedTextColor.DARK_RED);

            TextComponent.Builder priceComponent = text();

            if (prefix != null) {
              priceComponent.append(prefix);
            }

            priceComponent
                .append(text(p.getPrice(), hasPayment ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(space())
                .append(currencyName);

            return priceComponent.build();
          })
          .collect(Collectors.toList());
    }

    TextComponent.Builder cost = text()
        .append(translatable("shop.lore.cost", NamedTextColor.GRAY))
        .append(text(": ", NamedTextColor.DARK_GRAY));

    // Display free or single item price on the same line as cost
    if (price.size() == 1) {
      cost.append(price.get(0));
    }

    Component click =
        translatable("shop.lore." + (canPurchase ? "purchase" : "insufficient"), purchaseColor);

    String costLore = TextTranslations.translateLegacy(cost.build(), getBukkit());
    String clickLore = TextTranslations.translateLegacy(click, getBukkit());

    ItemStack item = icon.getItem().clone();
    ItemModifier.apply(item, getViewer());
    ItemMeta meta = item.getItemMeta();
    List<String> lore = Lists.newArrayList();
    if (meta.getLore() != null) {
      lore.addAll(meta.getLore());
    }
    lore.add(costLore);
    // Display payment requirements on different lines
    if (price.size() > 1) {
      for (Component line : price) {
        lore.add(TextTranslations.translateLegacy(line, getBukkit()));
      }
    }
    lore.add(clickLore);
    meta.setLore(lore);
    meta.addItemFlags(ItemFlag.values());
    item.setItemMeta(meta);

    return ClickableItem.of(item, c -> shop.purchase(icon, getViewer()));
  }

  private ClickableItem getPageItem(Player player, int page, boolean next) {
    return ClickableItem.of(getPageIcon(page, next), c -> {
      getInventory().open(player, page);
      this.highlight += next ? -9 : 9;
    });
  }

  private ItemStack getPageIcon(int page, boolean next) {
    return new ItemBuilder()
        .material(Material.ARROW)
        .amount(page)
        .name(ChatColor.YELLOW
            + TextTranslations.translate("menu.page." + (next ? "next" : "previous"), getBukkit()))
        .flags(ItemFlag.values())
        .build();
  }

  private String getMaterial(Material material) {
    return WordUtils.capitalizeFully(material.name().toLowerCase().replace("_", " "));
  }

  private class CategoryItem {
    private final Category category;
    private final ClickableItem item;
    private boolean active = false;

    public CategoryItem(Category category) {
      this.category = category;
      this.item =
          ClickableItem.of(category.getCategoryIcon(), c -> setCategory(category, c.getSlot()));
    }

    boolean isActive() {
      return active;
    }

    boolean changed() {
      return active != (active = category.getFilter().query(getViewer()).isAllowed());
    }
  }
}
