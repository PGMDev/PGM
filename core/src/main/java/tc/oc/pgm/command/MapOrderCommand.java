package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextTranslations;

public final class MapOrderCommand {

  @Command(
      aliases = {"nextmap", "mn", "mapnext", "nm", "next"},
      desc = "Show which map is playing next")
  public void nextmap(Audience audience, CommandSender sender, MapOrder mapOrder) {
    final MapInfo next = mapOrder.getNextMap();

    if (next == null) {
      sender.sendMessage(ChatColor.RED + TextTranslations.translate("map.noNextMap", sender));
      return;
    }

    audience.sendMessage(
        TranslatableComponent.of(
            "map.nextMap",
            TextColor.DARK_PURPLE,
            next.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)));
  }

  @Command(
      aliases = {"setnext", "sn"},
      desc = "Change the next map",
      usage = "[map name] -f (force) -r (revert)",
      flags = "fr",
      perms = Permissions.SETNEXT)
  public void setNext(
      Audience viewer,
      CommandSender sender,
      @Switch('f') boolean force,
      @Switch('r') boolean reset,
      @Fallback(Type.NULL) @Text MapInfo map,
      MapOrder mapOrder,
      Match match)
      throws CommandException {
    if (RestartManager.isQueued() && !force) {
      throw new CommandException(TextTranslations.translate("map.setNext.confirm", sender));
    }

    if (reset) {
      if (mapOrder.getNextMap() != null) {
        Component mapName = mapOrder.getNextMap().getStyledName(MapNameStyle.COLOR);
        mapOrder.resetNextMap();
        ChatDispatcher.broadcastAdminChatMessage(
            TranslatableComponent.of(
                "map.setNext.revert",
                TextColor.GRAY,
                UsernameFormatUtils.formatStaffName(sender, match),
                mapName),
            match);
      } else {
        viewer.sendWarning(TranslatableComponent.of("map.noNextMap"));
      }
      return;
    }

    mapOrder.setNextMap(map);

    if (RestartManager.isQueued()) {
      RestartManager.cancelRestart();
      viewer.sendWarning(
          TranslatableComponent.of("admin.cancelRestart.restartUnqueued", TextColor.GREEN));
    }

    Component mapName = TextComponent.of(map.getName(), TextColor.GOLD);
    Component successful =
        TranslatableComponent.of(
            "map.setNext",
            TextColor.GRAY,
            UsernameFormatUtils.formatStaffName(sender, match),
            mapName);
    ChatDispatcher.broadcastAdminChatMessage(successful, match);
  }
}
