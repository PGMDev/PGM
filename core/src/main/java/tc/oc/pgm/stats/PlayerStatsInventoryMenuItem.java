package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.stats.StatsMatchModule.damageComponent;
import static tc.oc.pgm.stats.StatsMatchModule.numberComponent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuItem;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextTranslations;

public class PlayerStatsInventoryMenuItem implements InventoryMenuItem {

  private final TextColor RESET = NamedTextColor.GRAY;
  private final UUID uuid;
  private final PlayerStats stats;
  private final Skin skin;
  private final String name;
  private final TextColor color;

  PlayerStatsInventoryMenuItem(
      UUID uuid, PlayerStats stats, Skin skin, String name, TextColor color) {
    this.uuid = uuid;
    this.stats = stats;
    this.skin = skin;
    this.name = name;
    this.color = color;
  }

  @Override
  public Component getDisplayName() {
    return text(name == null ? "Unknown" : name, color);
  }

  @Override
  public List<String> getLore(Player player) {
    List<String> lore = new ArrayList<>();

    Component statLore =
        translatable(
            "match.stats.concise",
            RESET,
            numberComponent(stats.getKills(), NamedTextColor.GREEN),
            numberComponent(stats.getDeaths(), NamedTextColor.RED),
            numberComponent(stats.getKD(), NamedTextColor.GREEN));
    Component killstreakLore =
        translatable(
            "match.stats.killstreak.concise",
            RESET,
            numberComponent(stats.getMaxKillstreak(), NamedTextColor.GREEN));
    Component damageDealtLore =
        translatable(
            "match.stats.damage.dealt",
            RESET,
            damageComponent(stats.getDamageDone(), NamedTextColor.GREEN),
            damageComponent(stats.getBowDamage(), NamedTextColor.YELLOW));
    Component damageReceivedLore =
        translatable(
            "match.stats.damage.received",
            RESET,
            damageComponent(stats.getDamageTaken(), NamedTextColor.RED));
    Component bowLore =
        translatable(
            "match.stats.bow",
            RESET,
            numberComponent(stats.getShotsHit(), NamedTextColor.YELLOW),
            numberComponent(stats.getShotsTaken(), NamedTextColor.YELLOW),
            numberComponent(stats.getArrowAccuracy(), NamedTextColor.YELLOW).append(text('%')));

    lore.add(TextTranslations.translateLegacy(statLore, player));
    lore.add(TextTranslations.translateLegacy(killstreakLore, player));
    lore.add(TextTranslations.translateLegacy(damageDealtLore, player));
    lore.add(TextTranslations.translateLegacy(damageReceivedLore, player));
    lore.add(TextTranslations.translateLegacy(bowLore, player));

    if (!optionalStat(
        lore, stats.getFlagsCaptured(), "match.stats.flagsCaptured.concise", player)) {
      if (!stats.getLongestFlagHold().equals(Duration.ZERO)) {
        lore.add(null);
        lore.add(
            TextTranslations.translateLegacy(
                translatable(
                    "match.stats.flaghold.concise",
                    RESET,
                    TemporalComponent.briefNaturalApproximate(stats.getLongestFlagHold())
                        .color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.BOLD, true)),
                player));
      }
    }
    optionalStat(lore, stats.getDestroyablePiecesBroken(), "match.stats.broken.concise", player);

    return lore;
  }

  private boolean optionalStat(List<String> lore, Number stat, String key, Player player) {
    if (stat.doubleValue() > 0) {
      lore.add(null);
      Component loreComponent =
          translatable(key, RESET, numberComponent(stat, NamedTextColor.AQUA));
      lore.add(TextTranslations.translateLegacy(loreComponent, player));
      return true;
    }
    return false;
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.SKULL_ITEM;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType) {}

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    SkullMeta skullMeta = (SkullMeta) meta;

    NMSHacks.setSkullMetaOwner(skullMeta, name, uuid, skin);

    return skullMeta;
  }

  @Override
  public ItemStack createItem(Player player) {
    ItemStack stack = new ItemStack(getMaterial(player), 1, (byte) 3);
    ItemMeta meta = stack.getItemMeta();

    meta.setDisplayName(
        TextTranslations.translateLegacy(
            getDisplayName().decoration(TextDecoration.BOLD, true), player));
    meta.setLore(getLore(player));
    meta.addItemFlags(ItemFlag.values());

    stack.setItemMeta(modifyMeta(meta));

    return stack;
  }
}
