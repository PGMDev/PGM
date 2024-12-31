package tc.oc.pgm.stats.menu.items;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static tc.oc.pgm.stats.StatType.*;
import static tc.oc.pgm.stats.StatsMatchModule.damageComponent;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;
import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.NumberComponent.number;

import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextTranslations;

/** Represents a player's stats via player head & lore * */
public class PlayerStatsMenuItem implements MenuItem {
  private static final JoinConfiguration TWO_SPACES = separator(text("  "));

  private final UUID uuid;
  private final PlayerStats stats;
  private final Skin skin;

  public PlayerStatsMenuItem(MatchPlayer player, PlayerStats stats) {
    this(player.getId(), stats, PLAYER_UTILS.getPlayerSkin(player.getBukkit()));
  }

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
    List<Component> lore = new ArrayList<>();

    lore.add(stats.spaceSeparated(KILLS, DEATHS, KILL_DEATH_RATIO));
    lore.add(stats.spaceSeparated(ASSISTS, KILL_STREAK));
    lore.add(translatable(
        "match.stats.damage.dealt",
        damageComponent(stats.getDamageDone(), NamedTextColor.GREEN),
        damageComponent(stats.getBowDamage(), NamedTextColor.YELLOW)));
    lore.add(translatable(
        "match.stats.damage.received",
        damageComponent(stats.getDamageTaken(), NamedTextColor.RED),
        damageComponent(stats.getBowDamageTaken(), NamedTextColor.GOLD)));
    lore.add(translatable(
        "match.stats.bow",
        number(stats.getShotsHit(), NamedTextColor.YELLOW),
        number(stats.getShotsTaken(), NamedTextColor.YELLOW),
        number(stats.getArrowAccuracy(), NamedTextColor.YELLOW).append(text('%'))));

    if (!optionalStat(lore, stats.getFlagsCaptured(), "match.stats.flagsCaptured.concise")) {
      if (!stats.getLongestFlagHold().equals(Duration.ZERO)) {
        lore.add(empty());
        lore.add(translatable(
            "match.stats.flaghold.concise",
            TemporalComponent.duration(stats.getLongestFlagHold(), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)));
      }
    }
    optionalStat(lore, stats.getDestroyablePiecesBroken(), "match.stats.broken.concise");

    return Lists.transform(lore, c -> TextTranslations.translateLegacy(c.color(GRAY), player));
  }

  private boolean optionalStat(List<Component> lore, Number stat, String key) {
    if (stat.doubleValue() > 0) {
      lore.add(empty());
      lore.add(translatable(key, number(stat, NamedTextColor.AQUA)));
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
