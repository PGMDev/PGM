package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.inventory.ViewInventoryMatchModule;

public class InventoryCommands {

  @Command(
      aliases = {"inventory", "inv", "vi"},
      desc = "View a player's inventory",
      usage = "<player>")
  public static void inventory(CommandSender sender, Player holder, Match match)
      throws CommandException {
    final Player viewer = (Player) sender;
    final ViewInventoryMatchModule vimm = match.getModule(ViewInventoryMatchModule.class);

    if (vimm.canPreviewInventory(viewer, holder)) {
      vimm.previewInventory((Player) sender, holder.getInventory());
    } else {
      throw new CommandException(
          ComponentRenderers.toLegacyText(
              new PersonalizedTranslatable("player.inventoryPreview.notViewable"), sender));
    }
  }
}
