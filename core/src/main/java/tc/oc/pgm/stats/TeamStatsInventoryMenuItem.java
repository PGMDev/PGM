package tc.oc.pgm.stats;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.stats.StatsMatchModule.damageComponent;
import static tc.oc.pgm.stats.StatsMatchModule.numberComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.menu.InventoryMenu;
import tc.oc.pgm.util.menu.InventoryMenuItem;
import tc.oc.pgm.util.menu.pattern.DoubleRowMenuArranger;
import tc.oc.pgm.util.menu.pattern.IdentityMenuArranger;
import tc.oc.pgm.util.text.TextTranslations;

public class TeamStatsInventoryMenuItem implements InventoryMenuItem {

  private final Competitor team;
  private final InventoryMenu teamSubGUI;
  private final Match match;

  private final NamedTextColor RESET = NamedTextColor.GRAY;

  TeamStatsInventoryMenuItem(Match match, Competitor team) {
    this.team = team;
    this.teamSubGUI =
        new InventoryMenu(
            match.getWorld(),
            translatable("match.stats.title"),
            team.getPlayers().stream()
                .map(PlayerStatsInventoryMenuItem::new)
                .collect(Collectors.toList()),
            team.getPlayers().size() > 10
                ? new IdentityMenuArranger(5)
                : new DoubleRowMenuArranger());
    this.match = match;
  }

  @Override
  public Component getDisplayName() {
    return translatable("match.stats.team", team.getName().color(), team.getName());
  }

  @Override
  public List<String> getLore(Player player) {

    StatsMatchModule smm = match.needModule(StatsMatchModule.class);
    List<String> lore = new ArrayList<>();
    int teamKills = 0;
    int teamDeaths = 0;
    double damageDone = 0;
    double damageTaken = 0;
    double bowDamage = 0;
    int shotsTaken = 0;
    int shotsHit = 0;
    for (MatchPlayer teamPlayer : team.getPlayers()) {
      PlayerStats stats = smm.getPlayerStat(teamPlayer.getId());
      teamKills += stats.getKills();
      teamDeaths += stats.getDeaths();
      damageDone += stats.getDamageDone();
      damageTaken += stats.getDamageTaken();
      bowDamage += stats.getBowDamage();
      shotsTaken += stats.getShotsTaken();
      shotsHit += stats.getShotsHit();
    }

    double teamKD = teamDeaths == 0 ? teamKills : teamKills / (double) teamDeaths;
    double teamBowAcc = shotsTaken == 0 ? Double.NaN : shotsHit / (shotsTaken / (double) 100);

    Component statLore =
        translatable(
            "match.stats.concise",
            RESET,
            numberComponent(teamKills, NamedTextColor.GREEN),
            numberComponent(teamDeaths, NamedTextColor.RED),
            numberComponent(teamKD, NamedTextColor.GREEN));

    Component damageDealtLore =
        translatable(
            "match.stats.damage.dealt",
            RESET,
            damageComponent(damageDone, NamedTextColor.GREEN),
            damageComponent(bowDamage, NamedTextColor.YELLOW));
    Component damageReceivedLore =
        translatable(
            "match.stats.damage.received", RESET, damageComponent(damageTaken, NamedTextColor.RED));
    Component bowLore =
        translatable(
            "match.stats.bow",
            RESET,
            numberComponent(shotsHit, NamedTextColor.YELLOW),
            numberComponent(shotsTaken, NamedTextColor.YELLOW),
            numberComponent(teamBowAcc, NamedTextColor.YELLOW).append(text('%')));

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
  public void onInventoryClick(InventoryMenu menu, Player player, ClickType clickType) {
    teamSubGUI.display(player);
  }

  @Override
  public ItemMeta modifyMeta(ItemMeta meta) {
    LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) meta;

    leatherArmorMeta.setColor(team.getFullColor());

    return leatherArmorMeta;
  }
}
