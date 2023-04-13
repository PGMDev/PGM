package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.inventory.ViewInventoryMatchModule;

public final class InventoryCommand {
  @CommandMethod("inventory|inv|vi <player>")
  @CommandDescription("View a player's inventory")
  @CommandPermission(Permissions.VIEW_INVENTORY)
  public void inventory(
      ViewInventoryMatchModule inventories,
      MatchPlayer viewer,
      @Argument("player") MatchPlayer holder) {
    if (inventories.canPreviewInventory(viewer, holder)) {
      inventories.previewInventory(viewer.getBukkit(), holder.getInventory());
    } else {
      throw exception("preview.notViewable");
    }
  }
}
