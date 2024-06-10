package tc.oc.pgm.command;

import static tc.oc.pgm.util.text.TextException.exception;

import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.inventory.ViewInventoryMatchModule;

public final class InventoryCommand {
  @Command("inventory|inv|vi <player>")
  @CommandDescription("View a player's inventory")
  @Permission(Permissions.VIEW_INVENTORY)
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
