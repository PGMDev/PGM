package tc.oc.pgm.stats.menu;

import static net.kyori.adventure.text.Component.translatable;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.SlotPos;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.menu.PagedInventoryMenu;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.stats.menu.items.TeamStatsMenuItem;
import tc.oc.pgm.stats.menu.items.VerboseStatsMenuItem;

/**
 * Menu overview of match stats - populated with {@link TeamStatsMenuItem} which lead to more
 * detailed team stats
 */
public class StatsMainMenu extends PagedInventoryMenu {

  // GUI values
  private static final int MIN_TEAM_ROWS = 2;
  private static final int MAX_TEAM_ROWS = 4;

  // How to populate the inventory slots when within fancy slot max
  private static final int[][] SLOTS = {{}, {4}, {3, 5}, {2, 4, 6}, {1, 3, 5, 7}, {0, 2, 4, 6, 8}};
  private static final int MAX_FANCY_SLOTS = SLOTS[SLOTS.length - 1].length * 2;

  private final StatsMatchModule stats;
  private final VerboseStatsMenuItem item;
  private final List<MenuItem> teams;

  public StatsMainMenu(MatchPlayer viewer, List<MenuItem> teams, StatsMatchModule stats) {
    super(
        translatable("match.stats.title", NamedTextColor.GOLD),
        2 + teamRows(teams.size()),
        viewer,
        null,
        teamRows(teams.size()) * 9,
        1,
        0);
    this.stats = stats;
    this.teams = teams;
    this.item = new VerboseStatsMenuItem();
  }

  private static int teamRows(int teams) {
    int requiredRows = (teams + 8) / 9;
    return Math.max(Math.min(requiredRows, MAX_TEAM_ROWS), MIN_TEAM_ROWS);
  }

  public ItemStack getItem() {
    return item.createItem(getBukkit());
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    contents.set(0, 4, stats.getPlayerStatsItem(getViewer()).getClickableItem(getBukkit()));

    // Use pagination when too many teams are present
    if (teams.size() > MAX_FANCY_SLOTS) {
      this.setupPageContents(player, contents);
      return;
    }

    int splitAt = teams.size() <= 4 ? teams.size() : (teams.size() + 1) >> 1;
    fancyLayoutRow(1, contents, teams.subList(0, splitAt), player);
    fancyLayoutRow(2, contents, teams.subList(splitAt, teams.size()), player);
  }

  private void fancyLayoutRow(
      int row, InventoryContents contents, List<MenuItem> items, Player player) {
    int[] slots = SLOTS[items.size()];
    for (int i = 0; i < items.size(); i++) {
      contents.set(row, slots[i], items.get(i).getClickableItem(player));
    }
  }

  @Override
  public SlotPos getEmptyPageSlot() {
    return SlotPos.of(1, 4);
  }

  @Override
  public ClickableItem[] getPageContents(Player viewer) {
    return teams.stream().map(team -> team.getClickableItem(viewer)).toArray(ClickableItem[]::new);
  }
}
