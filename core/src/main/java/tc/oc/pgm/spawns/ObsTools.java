package tc.oc.pgm.spawns;

import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import tc.oc.pgm.util.inventory.ItemBuilder;

/** Creates some of the items for the observer hotbar */
public class ObsTools {

  public static final String EDIT_WAND_PERMISSION = "worldedit.wand";

  public static ItemStack getTeleportTool(Player player) {
    return new ItemBuilder()
        .material(Material.COMPASS)
        .name(player, translatable("misc.teleportTool", NamedTextColor.BLUE, TextDecoration.BOLD))
        .build();
  }

  public static ItemStack getEditWand(Player player) {
    return new ItemBuilder()
        .material(Material.RABBIT_FOOT)
        .name(
            player, translatable("misc.editWand", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
        .build();
  }

  public static boolean canUseEditWand(Permissible permissible) {
    return permissible.hasPermission(EDIT_WAND_PERMISSION);
  }
}
