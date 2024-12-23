package tc.oc.pgm.stats.menu;

import static net.kyori.adventure.text.Component.translatable;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.SlotPos;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.PagedInventoryMenu;
import tc.oc.pgm.stats.menu.items.PlayerStatsMenuItem;
import tc.oc.pgm.util.text.TextFormatter;

/** Detailed menu of a single team's stats - populated with {@link PlayerStatsMenuItem} * */
public class TeamStatsMenu extends PagedInventoryMenu {

  private static final int TOTAL_ROWS = 6;
  private static final int PER_PAGE = 9 * 3;
  private static final int STARTING_ROW = 1;
  private static final int STARTING_COL = 0;

  private final Competitor team;
  private final List<PlayerStatsMenuItem> members;
  private final ClickableItem teamItem;

  public TeamStatsMenu(
      Competitor team,
      List<PlayerStatsMenuItem> members,
      MatchPlayer viewer,
      ClickableItem teamItem,
      SmartInventory parent) {
    super(
        translatable("match.stats.team", TextFormatter.convert(team.getColor()), team.getName()),
        TOTAL_ROWS,
        viewer,
        parent,
        PER_PAGE,
        STARTING_ROW,
        STARTING_COL);
    this.team = team;
    this.members = members;
    this.teamItem = teamItem;
  }

  public Competitor getTeam() {
    return team;
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    contents.set(0, 4, teamItem);
    this.setupPageContents(player, contents);
    this.addBackButton(
        contents,
        translatable("match.stats.title", NamedTextColor.GOLD, TextDecoration.BOLD),
        lastRow(),
        4);
  }

  @Override
  public ClickableItem[] getPageContents(Player viewer) {
    return members.stream().map(p -> p.getClickableItem(viewer)).toArray(ClickableItem[]::new);
  }

  @Override
  public SlotPos getEmptyPageSlot() {
    return SlotPos.of(2, 4);
  }
}
