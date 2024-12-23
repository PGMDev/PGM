package tc.oc.pgm.stats.menu.items;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.stats.StatsMatchModule.damageComponent;
import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.NumberComponent.number;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import tc.oc.pgm.api.Datastore;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.TeamStats;
import tc.oc.pgm.stats.menu.TeamStatsMenu;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.text.TextTranslations;

/** Represents a team with same color & lore. Clicking will open {@link TeamStatsMenu} * */
public class TeamStatsMenuItem implements MenuItem {

  private final Competitor team;
  private final Match match;
  private final TeamStats stats;
  private final List<PlayerStatsMenuItem> members;

  public TeamStatsMenuItem(Match match, Competitor team, Map<UUID, PlayerStats> playerStats) {
    this.team = team;
    this.stats = new TeamStats(playerStats.values());

    Datastore datastore = PGM.get().getDatastore();

    this.members = playerStats.entrySet().stream()
        .map(entry -> {
          UUID id = entry.getKey();
          MatchPlayer p = match.getPlayer(id);
          Skin skin = p != null ? PLAYER_UTILS.getPlayerSkin(p.getBukkit()) : datastore.getSkin(id);
          return new PlayerStatsMenuItem(id, entry.getValue(), skin);
        })
        .toList();

    this.match = match;
  }

  @Override
  public Component getDisplayName() {
    return translatable("match.stats.team", team.getName().color(), team.getName());
  }

  @Override
  public List<String> getLore(Player player) {
    List<String> lore = Lists.newArrayList();

    Component statLore = translatable(
        "match.stats.concise",
        NamedTextColor.GRAY,
        number(stats.getTeamKills(), NamedTextColor.GREEN),
        number(stats.getTeamDeaths(), NamedTextColor.RED),
        number(stats.getTeamKD(), NamedTextColor.GREEN));

    Component damageDealtLore = translatable(
        "match.stats.damage.dealt",
        NamedTextColor.GRAY,
        damageComponent(stats.getDamageDone(), NamedTextColor.GREEN),
        damageComponent(stats.getBowDamage(), NamedTextColor.YELLOW));
    Component damageReceivedLore = translatable(
        "match.stats.damage.received",
        NamedTextColor.GRAY,
        damageComponent(stats.getDamageTaken(), NamedTextColor.RED),
        damageComponent(stats.getBowDamageTaken(), NamedTextColor.GOLD));
    Component bowLore = translatable(
        "match.stats.bow",
        NamedTextColor.GRAY,
        number(stats.getShotsHit(), NamedTextColor.YELLOW),
        number(stats.getShotsTaken(), NamedTextColor.YELLOW),
        number(stats.getTeamBowAcc(), NamedTextColor.YELLOW).append(text('%')));

    lore.add(TextTranslations.translateLegacy(statLore, player));
    lore.add(TextTranslations.translateLegacy(damageDealtLore, player));
    lore.add(TextTranslations.translateLegacy(damageReceivedLore, player));
    lore.add(TextTranslations.translateLegacy(bowLore, player));

    return lore;
  }

  @Override
  public Material getMaterial(Player player) {
    return Material.LEATHER_CHESTPLATE;
  }

  @Override
  public void onClick(Player player, ClickType clickType) {
    new TeamStatsMenu(
            team,
            members,
            match.getPlayer(player),
            getClickableItem(player),
            PGM.get().getInventoryManager().getInventory(player).orElse(null))
        .open();
  }

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) meta;
    leatherArmorMeta.setColor(team.getFullColor());
    return leatherArmorMeta;
  }
}
