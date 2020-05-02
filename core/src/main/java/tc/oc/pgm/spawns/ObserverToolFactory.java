package tc.oc.pgm.spawns;

import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.text.TextTranslations;

/** Creates some of the items for the observer hotbar */
public class ObserverToolFactory {

  public static final String EDIT_WAND_PERMISSION = "worldedit.wand";

  private final Logger logger;
  private final PGM plugin;

  public ObserverToolFactory(PGM plugin) {
    this.plugin = plugin;
    this.logger = ClassLogger.get(plugin.getLogger(), getClass());
  }

  public ItemStack getTeleportTool(Player player) {
    return new ItemBuilder()
        .material(Material.COMPASS)
        .name(
            ChatColor.BLUE.toString()
                + ChatColor.BOLD
                + TextTranslations.translate("misc.teleportTool", player))
        .build();
  }

  public ItemStack getEditWand(Player player) {
    return new ItemBuilder()
        .material(Material.RABBIT_FOOT)
        .name(
            ChatColor.DARK_PURPLE.toString()
                + ChatColor.BOLD
                + TextTranslations.translate("misc.editWand", player))
        .build();
  }

  public boolean canUseEditWand(Permissible permissible) {
    return permissible.hasPermission(EDIT_WAND_PERMISSION);
  }
}
