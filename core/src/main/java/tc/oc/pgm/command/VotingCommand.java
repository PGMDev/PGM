package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Text;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.event.HoverEvent.Action;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.VotingPool;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.MapNameStyle;
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
        TranslatableComponent.of(
            "vote.add",
            TextColor.GRAY,
            UsernameFormatUtils.formatStaffName(sender, match),
            map.getStyledName(MapNameStyle.COLOR));

    if (vote.isCustomMapSelected(map)) {
      viewer.sendWarning(addMessage);
      return;
    }

    if (vote.addCustomVoteMap(map)) {
      ChatDispatcher.broadcastAdminChatMessage(addMessage, match);
    } else {
      viewer.sendWarning(TranslatableComponent.of("vote.limit", TextColor.RED));
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
    if (vote.removeCustomVote(map)) {
      ChatDispatcher.broadcastAdminChatMessage(
          TranslatableComponent.of(
              "vote.remove",
              TextColor.GRAY,
              UsernameFormatUtils.formatStaffName(sender, match),
              map.getStyledName(MapNameStyle.COLOR)),
          match);
    } else {
      viewer.sendWarning(TranslatableComponent.of("map.notFound"));
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
        TranslatableComponent.of(
            vote.toggleVoteMode() ? "vote.mode.replace" : "vote.mode.create",
            TextColor.LIGHT_PURPLE);
    ChatDispatcher.broadcastAdminChatMessage(
        TranslatableComponent.of(
            "vote.toggle",
            TextColor.GRAY,
            UsernameFormatUtils.formatStaffName(sender, match),
            voteModeName),
        match);
  }

  @Command(
      aliases = {"clear"},
      desc = "Clear all custom map selections from the next vote",
      perms = Permissions.SETNEXT)
  public void clearMaps(CommandSender sender, Match match, MapOrder mapOrder)
      throws CommandException {
    VotingPool vote = getVotingPool(sender, mapOrder);

    List<Component> maps =
        vote.getCustomVoteMaps().stream()
            .map(mi -> mi.getStyledName(MapNameStyle.COLOR))
            .collect(Collectors.toList());
    Component clearedMsg =
        TranslatableComponent.of(
            "vote.remove",
            TextColor.GRAY,
            UsernameFormatUtils.formatStaffName(sender, match),
            TextFormatter.list(maps, TextColor.GRAY));

    vote.getCustomVoteMaps().clear();

    ChatDispatcher.broadcastAdminChatMessage(clearedMsg, match);
  }

  @Command(
      aliases = {"list", "ls"},
      desc = "View a list of maps that have been selected for the next vote")
  public void listMaps(CommandSender sender, Audience viewer, MapOrder mapOrder)
      throws CommandException {
    VotingPool vote = getVotingPool(sender, mapOrder);

    int currentMaps = vote.getCustomVoteMaps().size();
    TextColor listNumColor =
        currentMaps >= VotingPool.MIN_CUSTOM_VOTE_OPTIONS
            ? currentMaps < VotingPool.MAX_VOTE_OPTIONS ? TextColor.GREEN : TextColor.YELLOW
            : TextColor.RED;

    String modeKey = vote.getVoteMode() ? "replace" : "create";
    Component mode =
        TranslatableComponent.of(String.format("vote.mode.%s", modeKey), TextColor.LIGHT_PURPLE)
            .hoverEvent(
                HoverEvent.of(
                    Action.SHOW_TEXT, TranslatableComponent.of("vote.mode.hover", TextColor.AQUA)))
            .clickEvent(ClickEvent.runCommand("/vote mode"));

    Component listMsg =
        TextComponent.builder()
            .append(TranslatableComponent.of("vote.title.selection"))
            .append(": (")
            .append(Integer.toString(currentMaps), listNumColor)
            .append("/")
            .append(Integer.toString(VotingPool.MAX_VOTE_OPTIONS), TextColor.RED)
            .append(") ")
            .append("\u00BB", TextColor.GOLD)
            .append(" [")
            .append(mode)
            .append("]")
            .color(TextColor.GRAY)
            .build();
    viewer.sendMessage(listMsg);

    int index = 1;
    for (MapInfo mi : vote.getCustomVoteMaps()) {
      Component indexedName =
          TextComponent.builder()
              .append(Integer.toString(index), TextColor.YELLOW)
              .append(". ", TextColor.WHITE)
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
              ChatColor.RED + TextTranslations.translate("vote.disabled", sender));
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
