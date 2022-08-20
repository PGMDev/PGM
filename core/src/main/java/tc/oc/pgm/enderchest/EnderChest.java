package tc.oc.pgm.enderchest;

import com.google.common.collect.Maps;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.text.TextTranslations;

public class EnderChest {

  private final Map<Party, Inventory> chests;
  private final int rows;

  public EnderChest(int rows) {
    this.chests = Maps.newHashMap();
    this.rows = rows;
  }

  public void clear() {
    this.chests.clear();
  }

  public void open(MatchPlayer player) {
    Party party = player.getParty();
    Inventory contents = chests.get(player.getParty());
    if (contents == null) {
      contents =
          Bukkit.createInventory(
              player.getBukkit(),
              rows * 9,
              party.getColor()
                  + player.getNameLegacy()
                  + "'s "
                  + ChatColor.GRAY
                  + TextTranslations.translate("misc.chest", player.getBukkit()));
      chests.put(player.getParty(), contents);
    }
    player.getBukkit().openInventory(contents);
  }
}
