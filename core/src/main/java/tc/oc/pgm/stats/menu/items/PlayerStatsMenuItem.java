package tc.oc.pgm.stats.menu.items;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static tc.oc.pgm.stats.StatsMatchModule.damageComponent;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;
import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.NumberComponent.number;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextTranslations;

/** Represents a player's stats via player head & lore * */
public class PlayerStatsMenuItem implements MenuItem {

  private final UUID uuid;
  private final PlayerStats stats;
  private final Skin skin;

  public PlayerStatsMenuItem(UUID uuid, PlayerStats stats, Skin skin) {
    this.uuid = uuid;
    this.stats = stats;
    this.skin = skin;
  }

  @Override
  public Component getDisplayName() {
    return stats.getPlayerComponent() != null
        ? stats.getPlayerComponent()
        : player(uuid, NameStyle.VERBOSE);
  }

  @Override
  public List<String> getLore(Player player) {
    List<String> lore = new ArrayList<>();

    Component statLore = translatable(
        "match.stats.concise",
        GRAY,
        number(stats.getKills(), NamedTextColor.GREEN),
        number(stats.getDeaths(), NamedTextColor.RED),
        number(stats.getKD(), NamedTextColor.GREEN));
    Component killstreakLore = translatable(
        "match.stats.killstreak.concise",
        GRAY,
        number(stats.getMaxKillstreak(), NamedTextColor.GREEN));
    Component damageDealtLore = translatable(
        "match.stats.damage.dealt",
        GRAY,
        damageComponent(stats.getDamageDone(), NamedTextColor.GREEN),
        damageComponent(stats.getBowDamage(), NamedTextColor.YELLOW));
    Component damageReceivedLore = translatable(
        "match.stats.damage.received",
        GRAY,
        damageComponent(stats.getDamageTaken(), NamedTextColor.RED),
        damageComponent(stats.getBowDamageTaken(), NamedTextColor.GOLD));
    Component bowLore = translatable(
        "match.stats.bow",
        GRAY,
        number(stats.getShotsHit(), NamedTextColor.YELLOW),
        number(stats.getShotsTaken(), NamedTextColor.YELLOW),
        number(stats.getArrowAccuracy(), NamedTextColor.YELLOW).append(text('%')));

    lore.add(TextTranslations.translateLegacy(statLore, player));
    lore.add(TextTranslations.translateLegacy(killstreakLore, player));
    lore.add(TextTranslations.translateLegacy(damageDealtLore, player));
    lore.add(TextTranslations.translateLegacy(damageReceivedLore, player));
    lore.add(TextTranslations.translateLegacy(bowLore, player));

    if (!optionalStat(
        lore, stats.getFlagsCaptured(), "match.stats.flagsCaptured.concise", player)) {
      if (!stats.getLongestFlagHold().equals(Duration.ZERO)) {
        lore.add(null);
        lore.add(TextTranslations.translateLegacy(
            translatable(
                "match.stats.flaghold.concise",
                GRAY,
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
      Component loreComponent = translatable(key, GRAY, number(stat, NamedTextColor.AQUA));
      lore.add(TextTranslations.translateLegacy(loreComponent, player));
      return true;
    }
    return false;
  }

  @Override
  public Material getMaterial(Player player) {
    return Materials.PLAYER_HEAD;
  }

  @Override
  public short getData() {
    return 3; // Player head
  }

  @Override
  public void onClick(Player player, ClickType clickType) {}

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    SkullMeta skullMeta = (SkullMeta) meta;
    Skin playerSkin = skin;

    // Fetch fake skin if using one
    Player bukkitPlayer = Bukkit.getPlayer(uuid);
    if (bukkitPlayer != null) {
      playerSkin = PLAYER_UTILS.getPlayerSkin(bukkitPlayer);
    }

    NMS_HACKS.setSkullMetaOwner(skullMeta, "name", uuid, playerSkin);

    return skullMeta;
  }
}
