package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.FlagYielding;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.named.MapNameStyle;

public final class MapOrderCommand {

  @CommandMethod("nextmap|mn|mapnext|nm|next")
  @CommandDescription("Show which map is playing next")
  public void nextmap(Audience audience, MapOrder mapOrder) {
    final MapInfo next = mapOrder.getNextMap();

    if (next == null) throw exception("map.noNextMap");

    audience.sendMessage(
        translatable(
            "map.nextMap",
            NamedTextColor.DARK_PURPLE,
            next.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)));
  }

  @CommandMethod("setnext|sn [map]")
  @CommandDescription("Change the next map")
  @CommandPermission(Permissions.SETNEXT)
  public void setNext(
      Audience viewer,
      CommandSender sender,
      MapOrder mapOrder,
      Match match,
      @Flag(value = "force", aliases = "f") boolean force,
      @Flag(value = "reset", aliases = "r") boolean reset,
      @Argument("map") @FlagYielding MapInfo map) {
    if (RestartManager.isQueued() && !force) {
      throw exception("map.setNext.confirm");
    }

    if (reset) {
      if (mapOrder.getNextMap() != null) {
        Component mapName = mapOrder.getNextMap().getStyledName(MapNameStyle.COLOR);
        mapOrder.setNextMap(null);
        ChatDispatcher.broadcastAdminChatMessage(
            translatable(
                "map.setNext.revert",
                NamedTextColor.GRAY,
                UsernameFormatUtils.formatStaffName(sender, match),
                mapName),
            match);
      } else {
        viewer.sendWarning(translatable("map.noNextMap"));
      }
      return;
    }

    mapOrder.setNextMap(map);

    if (RestartManager.isQueued()) {
      RestartManager.cancelRestart();
      viewer.sendWarning(translatable("admin.cancelRestart.restartUnqueued", NamedTextColor.GREEN));
    }

    Component mapName = text(map.getName(), NamedTextColor.GOLD);
    Component successful =
        translatable(
            "map.setNext",
            NamedTextColor.GRAY,
            UsernameFormatUtils.formatStaffName(sender, match),
            mapName);
    ChatDispatcher.broadcastAdminChatMessage(successful, match);
  }
}
