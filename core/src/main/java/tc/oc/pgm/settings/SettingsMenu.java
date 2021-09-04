package tc.oc.pgm.settings;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextTranslations.translateLegacy;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.text.TextFormatter;

public class SettingsMenu extends InventoryMenu {

  private static final Component DESC_DIV = text("\u00BB ", NamedTextColor.GOLD);

  public SettingsMenu(MatchPlayer viewer) {
    super("settings.menu.title", NamedTextColor.AQUA, 5, viewer);
    open();
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    setup(contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    setup(contents);
  }

  // Custom layout:
  // Each key shows their related icon w/ description
  // followed with by the value represented in a
  // colorful way one row under.
  //
  // Clicking either button will toggle a setting
  private void setup(InventoryContents contents) {
    int row = 0;
    int col = 1;
    for (SettingKey setting : SettingKey.values()) {
      contents.set(row, col, getSettingKey(setting));
      contents.set(row + 1, col, getSettingValue(setting));
      col++;
      if (col > 7) {
        row += 3;
        col = 1;
      }
    }
  }

  private ClickableItem getSettingKey(SettingKey key) {
    return getSettingToggleItem(key, getSettingKeyItem(key));
  }

  private ClickableItem getSettingValue(SettingKey key) {
    return getSettingToggleItem(key, getSettingValueItem(key));
  }

  private ClickableItem getSettingToggleItem(SettingKey key, ItemStack item) {
    return ClickableItem.of(
        item,
        c -> {
          getViewer().getSettings().toggleValue(key);
          key.update(getViewer());
        });
  }

  private ItemStack getSettingKeyItem(SettingKey key) {
    Component lore = translatable("settings." + key.name().toLowerCase(), NamedTextColor.GRAY);
    return new ItemBuilder()
        .material(key.getIconMaterial())
        .name(ChatColor.YELLOW + ChatColor.BOLD.toString() + WordUtils.capitalize(key.getName()))
        .lore(translateLegacy(lore, getBukkit()))
        .flags(ItemFlag.values())
        .build();
  }

  private ItemStack getSettingValueItem(SettingKey key) {
    SettingValue value = getViewer().getSettings().getValue(key);
    Component current =
        translatable(
            "setting.get", NamedTextColor.GRAY, getSettingKeyName(key), getSettingValueName(value));
    Component desc = getSettingDescription(value);
    Component toggle = translatable("settings.menu.toggle", NamedTextColor.GRAY);

    return new ItemBuilder()
        .material(Material.INK_SACK)
        .color(value.getColor())
        .name(ChatColor.YELLOW + ChatColor.BOLD.toString() + WordUtils.capitalize(key.getName()))
        .lore(
            translateLegacy(current, getBukkit()),
            translateLegacy(desc, getBukkit()),
            "",
            translateLegacy(toggle, getBukkit()))
        .flags(ItemFlag.values())
        .build();
  }

  private Component getSettingKeyName(SettingKey key) {
    return text(key.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD);
  }

  private Component getSettingValueName(SettingValue value) {
    return text(
        WordUtils.capitalize(value.getName()),
        TextFormatter.convert(BukkitUtils.dyeColorToChatColor(value.getColor())));
  }

  private Component getSettingDescription(SettingValue value) {
    return text()
        .append(DESC_DIV)
        .append(translatable(getSettingTranslationKey(value), NamedTextColor.GRAY))
        .build();
  }

  private String getSettingTranslationKey(SettingValue value) {
    String valueKey =
        value
            .name()
            .replace(value.getKey().name().toUpperCase(), "")
            .replace("_", "")
            .toLowerCase();
    return String.format("settings.%s.%s", value.getKey().name().toLowerCase(), valueKey);
  }
}
