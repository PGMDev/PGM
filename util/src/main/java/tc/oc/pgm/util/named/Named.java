package tc.oc.pgm.util.named;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface Named {

  Component getName(NameStyle style, Player viewer);

  default Component getName(NameStyle style, CommandSender sender) {
    return getName(style, sender instanceof Player ? (Player) sender : null);
  }

  default Component getName(NameStyle style) {
    return getName(style, null);
  }

  default Component getName() {
    return getName(NameStyle.FANCY);
  }

  // TODO: Maybe add a note here explaining to prefer Named#getName()
  String getNameLegacy();
}
