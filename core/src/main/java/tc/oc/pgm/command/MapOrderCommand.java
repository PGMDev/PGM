package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.api.Permissions.DEV;
import static tc.oc.pgm.api.map.Phase.DEVELOPMENT;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TextException.exception;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotation.specifier.FlagYielding;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.channels.ChatManager;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.MapNameStyle;

public final class MapOrderCommand {

  @Command("nextmap|mn|mapnext|nm|next")
  @CommandDescription("Show which map is playing next")
  public void nextmap(Audience audience, MapOrder mapOrder) {
    final MapInfo next = mapOrder.getNextMap();

    if (next == null) throw exception("map.noNextMap");

    audience.sendMessage(translatable(
        "map.nextMap",
        NamedTextColor.DARK_PURPLE,
        next.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)));
  }

  @Command("setnext|sn [map]")
  @CommandDescription("Change the next map")
  @Permission(Permissions.SETNEXT)
  public void setNext(
      Audience viewer,
      CommandSender sender,
      MapOrder mapOrder,
      @Flag(value = "force", aliases = "f") boolean force,
      @Flag(value = "reset", aliases = "r") boolean reset,
      @Argument("map") @Default(CURRENT) @FlagYielding MapInfo map) {
    if (RestartManager.isQueued() && !force) {
      throw exception("map.setNext.confirm");
    }

    if (PGM.get().getConfiguration().enforceDevPhase()
        && DEVELOPMENT.equals(map.getPhase())
        && !sender.hasPermission(DEV)) {
      throw exception("map.setNext.notDev");
    }

    if (reset) {
      if (mapOrder.getNextMap() != null) {
        Component mapName = mapOrder.getNextMap().getStyledName(MapNameStyle.COLOR);
        mapOrder.setNextMap(null);
        ChatManager.broadcastAdminMessage(
            translatable("map.setNext.revert", NamedTextColor.GRAY, player(sender), mapName));
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

    sendSetNextMessage(map, sender);
  }

  public static void sendSetNextMessage(@NotNull MapInfo map, CommandSender sender) {
    Component mapName = text(map.getName(), NamedTextColor.GOLD);
    ChatManager.broadcastAdminMessage(
        translatable("map.setNext", NamedTextColor.GRAY, player(sender), mapName));
  }
}
