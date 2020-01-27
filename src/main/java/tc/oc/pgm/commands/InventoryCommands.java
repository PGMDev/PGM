package tc.oc.pgm.commands;

import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.inventory.ViewInventoryMatchModule;
@CommandContainer
public class InventoryCommands {

  @Command(
          name = "inventory",
          aliases = {"inv", "vi"},
          desc = "View a player's inventory",
          descFooter = "<player>")
  public static void inventory(CommandSender sender, Player holder, Match match)
  {
    final Player viewer = (Player) sender;
    final ViewInventoryMatchModule vimm = match.getMatchModule(ViewInventoryMatchModule.class);

    if (vimm.canPreviewInventory(viewer, holder)) {
      vimm.previewInventory((Player) sender, holder.getInventory());
    } else {
      throw new IllegalStateException(
          ComponentRenderers.toLegacyText(
              new PersonalizedTranslatable("player.inventoryPreview.notViewable"), sender));
    }
  }
}
