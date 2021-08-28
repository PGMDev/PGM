package tc.oc.pgm.observers;

import com.google.common.collect.Lists;
import fr.minuskube.inv.content.InventoryContents;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.menu.InventoryMenu;
import tc.oc.pgm.menu.MenuItem;
import tc.oc.pgm.observers.tools.FlySpeedTool;
import tc.oc.pgm.observers.tools.GamemodeTool;
import tc.oc.pgm.observers.tools.NightVisionTool;
import tc.oc.pgm.observers.tools.SettingsTool;

public class ObserverToolsMenu extends InventoryMenu {

  private List<MenuItem> items;

  public ObserverToolsMenu(MatchPlayer viewer) {
    super("setting.title", NamedTextColor.AQUA, 1, viewer);
    this.items =
        Lists.newArrayList(
            new FlySpeedTool(), new NightVisionTool(), new SettingsTool(), new GamemodeTool());
    open();
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    int col = 1;
    for (MenuItem item : items) {
      contents.set(0, col, item.getClickableItem(player));
      col += 2;
    }
  }
}
