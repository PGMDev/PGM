package tc.oc.pgm.menu;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextTranslations.translateLegacy;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.inventory.ItemBuilder;

/** A generic inventory menu * */
public abstract class InventoryMenu implements InventoryProvider {

  private final SmartInventory inventory;
  private final MatchPlayer viewer;

  public InventoryMenu(String titleKey, NamedTextColor titleColor, int rows, MatchPlayer viewer) {
    this(titleKey, titleColor, rows, viewer, null);
  }

  public InventoryMenu(
      String titleKey,
      NamedTextColor titleColor,
      int rows,
      MatchPlayer viewer,
      @Nullable SmartInventory parent) {
    this(translatable(titleKey, titleColor, TextDecoration.BOLD), rows, viewer, parent);
  }

  public InventoryMenu(
      Component title, int rows, MatchPlayer viewer, @Nullable SmartInventory parent) {
    this.viewer = viewer;

    SmartInventory.Builder builder = SmartInventory.builder()
        .manager(PGM.get().getInventoryManager())
        .title(translateLegacy(title, getBukkit()))
        .size(rows, 9)
        .provider(this);

    if (parent != null) {
      builder.parent(parent);
    }

    this.inventory = builder.build();
  }

  public void open() {
    inventory.open(getBukkit());
  }

  public Player getBukkit() {
    return viewer.getBukkit();
  }

  public MatchPlayer getViewer() {
    return viewer;
  }

  public SmartInventory getInventory() {
    return inventory;
  }

  public int lastRow() {
    return inventory.getRows() - 1;
  }

  public void addBackButton(
      InventoryContents contents, Component parentMenuTitle, int row, int col) {
    Component back = translatable("menu.page.return", NamedTextColor.GRAY, parentMenuTitle);

    contents.set(
        row,
        col,
        ClickableItem.of(
            new ItemBuilder().material(Material.BARRIER).name(getBukkit(), back).build(),
            c -> getInventory().getParent().ifPresent(parent -> parent.open(getBukkit()))));
  }

  @Override
  public void update(Player player, InventoryContents contents) {}
}
