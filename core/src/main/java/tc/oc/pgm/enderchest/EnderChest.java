package tc.oc.pgm.enderchest;

import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.text.TextTranslations;

public class EnderChest {

  private final int rows;
  private Inventory contents;

  public EnderChest(int rows) {
    this.rows = rows;
  }

  @Nullable
  public Inventory getContents() {
    return contents;
  }

  public void clear() {
    this.contents = null;
  }

  public void drop(Location location) {
    for (ItemStack item : contents.getContents()) {
      if (item != null && item.getType() != Material.AIR) {
        location.getWorld().dropItem(location, item);
      }
    }
    clear();
  }

  public void open(MatchPlayer player) {
    if (contents == null) {
      contents =
          Bukkit.createInventory(
              player.getBukkit(),
              rows * 9,
              player.getParty().getColor()
                  + player.getNameLegacy()
                  + "'s "
                  + ChatColor.GRAY
                  + TextTranslations.translate("misc.chest", player.getBukkit()));
    }
    player.getBukkit().openInventory(contents);
  }
}
