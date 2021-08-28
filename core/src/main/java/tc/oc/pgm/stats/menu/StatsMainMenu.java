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
import tc.oc.pgm.menu.PagedInventoryMenu;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.stats.menu.items.TeamStatsMenuItem;
import tc.oc.pgm.stats.menu.items.VerboseStatsMenuItem;

/**
 * Menu overview of match stats - populated with {@link TeamStatsMenuItem} which lead to more
 * detailed team stats *
 */
public class StatsMainMenu extends PagedInventoryMenu {

  // GUI values
  private static final int TOTAL_ROWS = 4;
  private static final int PER_PAGE = 18;

  // How to populate the inventory slots when within fancy slot max
  private static final int MAX_FANCY_SLOTS = 13;
  private static final int[][] FANCY_SLOTS = {{3, 5, 1, 7}, {4, 0, 8, 2, 6}};

  private final StatsMatchModule stats;
  private final VerboseStatsMenuItem item;
  private final List<TeamStatsMenuItem> teams;

  public StatsMainMenu(MatchPlayer viewer, List<TeamStatsMenuItem> teams, StatsMatchModule stats) {
    super(
        translatable("match.stats.title", NamedTextColor.GOLD),
        TOTAL_ROWS,
        viewer,
        null,
        PER_PAGE,
        1,
        0);
    this.stats = stats;
    this.teams = teams;
    this.item = new VerboseStatsMenuItem();
  }

  public ItemStack getItem() {
    return item.createItem(getBukkit());
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    contents.set(
        0, 4, ClickableItem.empty(stats.getPlayerStatsItem(getViewer()).createItem(getBukkit())));

    // Use pagination when too many teams are present
    if (teams.size() > MAX_FANCY_SLOTS) {
      this.setupPageContents(player, contents);
      return;
    }

    // Fancy Slots layout supports up to 13 teams. If a map contains more than this
    // menu will default to a paginated style with 18 teams per page.
    int slotCol = 0;
    int slotRow = 0;
    int row = 1;
    for (TeamStatsMenuItem team : teams) {
      contents.set(row, FANCY_SLOTS[slotRow][slotCol], team.getClickableItem(player));

      slotCol++;
      if (slotCol >= FANCY_SLOTS[slotRow].length) {
        slotCol = 0;
        slotRow++;
        row++;
      }

      if (slotRow >= FANCY_SLOTS.length) {
        slotRow = 0;
      }
    }
  }

  @Override
  public SlotPos getPreviousPageSlot() {
    return SlotPos.of(3, 0);
  }

  @Override
  public SlotPos getNextPageSlot() {
    return SlotPos.of(3, 8);
  }

  @Override
  public SlotPos getEmptyPageSlot() {
    return SlotPos.of(1, 4);
  }

  @Override
  public ClickableItem[] getPageContents(Player viewer) {
    if (teams.isEmpty() || teams == null) return null;
    return teams.stream().map(team -> team.getClickableItem(viewer)).toArray(ClickableItem[]::new);
  }
}
