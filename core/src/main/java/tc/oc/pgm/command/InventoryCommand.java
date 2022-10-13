package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.inventory.ViewInventoryMatchModule;

public final class InventoryCommand {
  @CommandMethod("inventory|inv|vi <player>")
  @CommandDescription("View a player's inventory")
  public void inventory(Match match, MatchPlayer viewer, @Argument("player") MatchPlayer holder) {
    final ViewInventoryMatchModule inventories = match.needModule(ViewInventoryMatchModule.class);

    if (inventories.canPreviewInventory(viewer, holder)) {
      inventories.previewInventory(viewer.getBukkit(), holder.getInventory());
    } else {
      throw exception("preview.notViewable");
    }
  }
}
