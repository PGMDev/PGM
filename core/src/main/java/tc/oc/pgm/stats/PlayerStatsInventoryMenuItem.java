package tc.oc.pgm.stats;

import static tc.oc.pgm.stats.StatsMatchModule.numberComponent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
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
  private final TextColor RESET = TextColor.GRAY;

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
        TranslatableComponent.of(
            "match.stats.concise",
            RESET,
            numberComponent(stats.getKills(), TextColor.GREEN),
            numberComponent(stats.getDeaths(), TextColor.RED),
            numberComponent(stats.getKD(), TextColor.GREEN));
    Component killstreakLore =
        TranslatableComponent.of(
            "match.stats.killstreak.concise",
            RESET,
            numberComponent(stats.getMaxKillstreak(), TextColor.GREEN));
    Component damageDealtLore =
        TranslatableComponent.of(
            "match.stats.damage.dealt",
            RESET,
            numberComponent(stats.getDamageDone(), TextColor.GREEN),
            numberComponent(stats.getBowDamage(), TextColor.YELLOW));
    Component damageReceivedLore =
        TranslatableComponent.of(
            "match.stats.damage.received",
            RESET,
            numberComponent(stats.getDamageTaken(), TextColor.RED));
    Component bowLore =
        TranslatableComponent.of(
            "match.stats.bow",
            RESET,
            numberComponent(stats.getShotsHit(), TextColor.YELLOW),
            numberComponent(stats.getShotsTaken(), TextColor.YELLOW),
            numberComponent(stats.getArrowAccuracy(), TextColor.YELLOW));

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
                TranslatableComponent.of(
                    "match.stats.flaghold.concise",
                    RESET,
                    PeriodFormats.briefNaturalApproximate(stats.getLongestFlagHold())
                        .color(TextColor.AQUA)
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
          TranslatableComponent.of(key, RESET, numberComponent(stat, TextColor.AQUA));
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
