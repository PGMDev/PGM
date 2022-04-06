package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Text;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.named.MapNameStyle;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.VotingPool;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public class VotingCommand {

  @Command(
      aliases = {"add"},
      desc = "Add a custom map to the next vote",
      usage = "[map name]",
      perms = Permissions.SETNEXT)
  public void addMap(
      Audience viewer,
      CommandSender sender,
      @Fallback(Type.NULL) @Text MapInfo map,
      MapOrder mapOrder,
      Match match)
      throws CommandException {
    VotingPool vote = getVotingPool(sender, mapOrder);

    Component addMessage =
        translatable(
            "vote.add",
            NamedTextColor.GRAY,
            UsernameFormatUtils.formatStaffName(sender, match),
            map.getStyledName(MapNameStyle.COLOR));

    if (vote.getOptions().isAdded(map)) {
      viewer.sendWarning(addMessage);
      return;
    }

    if (vote.getOptions().addVote(map)) {
      ChatDispatcher.broadcastAdminChatMessage(addMessage, match);
    } else {
      viewer.sendWarning(translatable("vote.limit", NamedTextColor.RED));
    }
  }

  @Command(
      aliases = {"remove", "rm"},
      desc = "Remove a custom map from the next vote",
      usage = "[map name]",
      perms = Permissions.SETNEXT)
  public void removeMap(
      Audience viewer,
      CommandSender sender,
      @Fallback(Type.NULL) @Text MapInfo map,
      MapOrder mapOrder,
      Match match)
      throws CommandException {
    VotingPool vote = getVotingPool(sender, mapOrder);
    if (vote.getOptions().removeMap(map)) {
      ChatDispatcher.broadcastAdminChatMessage(
          translatable(
              "vote.remove",
              NamedTextColor.GRAY,
              UsernameFormatUtils.formatStaffName(sender, match),
              map.getStyledName(MapNameStyle.COLOR)),
          match);
    } else {
      viewer.sendWarning(translatable("map.notFound"));
    }
  }

  @Command(
      aliases = {"mode"},
      desc = "Toggle the voting mode between replace and override",
      perms = Permissions.SETNEXT)
  public void mode(Audience viewer, CommandSender sender, MapOrder mapOrder, Match match)
      throws CommandException {
    VotingPool vote = getVotingPool(sender, mapOrder);
    Component voteModeName =
        translatable(
            vote.getOptions().toggleMode() ? "vote.mode.replace" : "vote.mode.create",
            NamedTextColor.LIGHT_PURPLE);
    ChatDispatcher.broadcastAdminChatMessage(
        translatable(
            "vote.toggle",
            NamedTextColor.GRAY,
            UsernameFormatUtils.formatStaffName(sender, match),
            voteModeName),
        match);
  }

  @Command(
      aliases = {"clear"},
      desc = "Clear all custom map selections from the next vote",
      perms = Permissions.SETNEXT)
  public void clearMaps(Audience viewer, CommandSender sender, Match match, MapOrder mapOrder)
      throws CommandException {
    VotingPool vote = getVotingPool(sender, mapOrder);

    List<Component> maps =
        vote.getOptions().getCustomVoteMaps().stream()
            .map(mi -> mi.getStyledName(MapNameStyle.COLOR))
            .collect(Collectors.toList());
    Component clearedMsg =
        translatable(
            "vote.remove",
            NamedTextColor.GRAY,
            UsernameFormatUtils.formatStaffName(sender, match),
            TextFormatter.list(maps, NamedTextColor.GRAY));

    vote.getOptions().clear();

    if (maps.isEmpty()) {
      viewer.sendWarning(translatable("vote.noMapsFound"));
    } else {
      ChatDispatcher.broadcastAdminChatMessage(clearedMsg, match);
    }
  }

  @Command(
      aliases = {"list", "ls"},
      desc = "View a list of maps that have been selected for the next vote")
  public void listMaps(CommandSender sender, Audience viewer, MapOrder mapOrder)
      throws CommandException {
    VotingPool vote = getVotingPool(sender, mapOrder);

    int currentMaps = vote.getOptions().getCustomVoteMaps().size();
    TextColor listNumColor =
        currentMaps >= VotingPool.MIN_CUSTOM_VOTE_OPTIONS
            ? currentMaps < VotingPool.MAX_VOTE_OPTIONS
                ? NamedTextColor.GREEN
                : NamedTextColor.YELLOW
            : NamedTextColor.RED;

    String modeKey = vote.getOptions().isReplace() ? "replace" : "create";
    Component mode =
        translatable(String.format("vote.mode.%s", modeKey), NamedTextColor.LIGHT_PURPLE)
            .hoverEvent(showText(translatable("vote.mode.hover", NamedTextColor.AQUA)))
            .clickEvent(runCommand("/vote mode"));

    Component listMsg =
        text()
            .append(translatable("vote.title.selection"))
            .append(text(": ("))
            .append(text(currentMaps, listNumColor))
            .append(text("/"))
            .append(text(VotingPool.MAX_VOTE_OPTIONS, NamedTextColor.RED))
            .append(text(") "))
            .append(text("\u00BB", NamedTextColor.GOLD))
            .append(text(" ["))
            .append(mode)
            .append(text("]"))
            .color(NamedTextColor.GRAY)
            .build();
    viewer.sendMessage(listMsg);

    int index = 1;
    for (MapInfo mi : vote.getOptions().getCustomVoteMaps()) {
      Component indexedName =
          text()
              .append(text(index, NamedTextColor.YELLOW))
              .append(text(". ", NamedTextColor.WHITE))
              .append(mi.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS))
              .build();
      viewer.sendMessage(indexedName);
      index++;
    }
  }

  public static VotingPool getVotingPool(CommandSender sender, MapOrder mapOrder)
      throws CommandException {
    if (mapOrder instanceof MapPoolManager) {
      MapPoolManager manager = (MapPoolManager) mapOrder;
      if (manager.getActiveMapPool() instanceof VotingPool) {
        VotingPool votePool = (VotingPool) manager.getActiveMapPool();
        if (votePool.getCurrentPoll() != null) {
          throw new CommandException(
              ChatColor.RED + TextTranslations.translate("vote.modify.disallow", sender));
        }
        return votePool;
      }
      throw new CommandException(
          ChatColor.RED + TextTranslations.translate("vote.disabled", sender));
    }

    throw new CommandException(
        ChatColor.RED + TextTranslations.translate("pool.mapPoolsDisabled", sender));
  }
}
