package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.stats.StatsMatchModule.damageComponent;
import static tc.oc.pgm.stats.StatsMatchModule.numberComponent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.InventoryMenuItem;
import tc.oc.pgm.util.text.PeriodFormats;
import tc.oc.pgm.util.text.TextTranslations;

public class PlayerStatsInventoryMenuItem implements InventoryMenuItem {

  private final MatchPlayer player;
  private final TextColor RESET = NamedTextColor.GRAY;

  PlayerStatsInventoryMenuItem(MatchPlayer player) {
    this.player = player;
  }

  @Override
  public Component getName() {
    return player.getName();
  }

  @Override
  public ChatColor getColor() {
    return ChatColor.GOLD;
  }

  @Override
  public List<String> getLore(MatchPlayer player) {
    List<String> lore = new ArrayList<>();
    StatsMatchModule smm = player.getMatch().needModule(StatsMatchModule.class);
    PlayerStats stats = smm.getPlayerStat(this.player.getId());

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
            numberComponent(stats.getArrowAccuracy(), NamedTextColor.YELLOW));

    Player bukkit = player.getBukkit();

    lore.add(TextTranslations.translateLegacy(statLore, bukkit));
    lore.add(TextTranslations.translateLegacy(killstreakLore, bukkit));
    lore.add(TextTranslations.translateLegacy(damageDealtLore, bukkit));
    lore.add(TextTranslations.translateLegacy(damageReceivedLore, bukkit));
    lore.add(TextTranslations.translateLegacy(bowLore, bukkit));

    if (!optionalStat(
        lore, stats.getFlagsCaptured(), "match.stats.flagsCaptured.concise", bukkit)) {
      if (!stats.getLongestFlagHold().equals(Duration.ZERO)) {
        lore.add(null);
        lore.add(
            TextTranslations.translateLegacy(
                translatable(
                    "match.stats.flaghold.concise",
                    RESET,
                    PeriodFormats.briefNaturalApproximate(stats.getLongestFlagHold())
                        .color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.BOLD, true)),
                bukkit));
      }
    }
    optionalStat(lore, stats.getDestroyablePiecesBroken(), "match.stats.broken.concise", bukkit);

    return lore;
  }

  private boolean optionalStat(List<String> lore, Number stat, String key, Player bukkit) {
    if (stat.doubleValue() > 0) {
      lore.add(null);
      Component loreComponent =
          translatable(key, RESET, numberComponent(stat, NamedTextColor.AQUA));
      lore.add(TextTranslations.translateLegacy(loreComponent, bukkit));
      return true;
    }
    return false;
  }

  @Override
  public Material getMaterial(MatchPlayer player) {
    return Material.SKULL_ITEM;
  }

  @Override
  public void onInventoryClick(InventoryMenu menu, MatchPlayer player, ClickType clickType) {}

  @Override
  public ItemStack createItem(MatchPlayer player) {
    ItemStack stack = new ItemStack(getMaterial(player), 1, (byte) 3);
    SkullMeta meta = (SkullMeta) stack.getItemMeta();

    meta.setOwner(this.player.getNameLegacy());

    meta.setDisplayName(
        getColor()
            + ChatColor.BOLD.toString()
            + TextTranslations.translateLegacy(getName(), player.getBukkit()));
    meta.setLore(getLore(player));
    meta.addItemFlags(ItemFlag.values());

    stack.setItemMeta(meta);

    return stack;
  }
}
