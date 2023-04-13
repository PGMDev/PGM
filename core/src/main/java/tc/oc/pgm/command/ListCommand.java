package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.api.integration.Integration.isVanished;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public final class ListCommand {

  @CommandMethod("list|who|online|ls")
  @CommandDescription("View a list of online players")
  public void list(CommandSender sender, Audience viewer, Match match) {
    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    if (tmm != null) {
      viewer.sendMessage(translatable("command.list.teams", NamedTextColor.GRAY));
      tmm.getTeams()
          .forEach(
              team ->
                  sendTeamInfo(
                      viewer, sender, team.getName(), team.getPlayers(), team.getMaxPlayers()));
    } else {
      sendTeamInfo(
          viewer,
          sender,
          translatable("command.list.participants"),
          match.getParticipants(),
          match.getMaxPlayers());
    }
    // Observers
    sendTeamInfo(
        viewer,
        sender,
        match.getDefaultParty().getName(),
        match.getDefaultParty().getPlayers(),
        -1);

    // Total count
    viewer.sendMessage(
        translatable(
            "command.list.online",
            NamedTextColor.GRAY,
            text(getSize(match.getPlayers(), false), NamedTextColor.GREEN)));
  }

  private void sendTeamInfo(
      Audience viewer,
      CommandSender sender,
      Component teamName,
      Collection<MatchPlayer> players,
      int max) {
    Component teamLine =
        text()
            .append(teamName)
            .append(text(": ", NamedTextColor.GRAY))
            .append(text(getSize(players, false)))
            .append(max != -1 ? text("/" + max, NamedTextColor.GRAY) : empty())
            .append(
                getSize(players, true) > 0 && sender.hasPermission(Permissions.STAFF)
                    ? formatVanishCount(players)
                    : empty())
            .build();
    viewer.sendMessage(teamLine);
    if (!players.isEmpty()) {
      viewer.sendMessage(formatNames(players, sender));
    }
  }

  private Component formatNames(Collection<MatchPlayer> players, CommandSender sender) {
    List<Component> names =
        players.stream()
            .filter(mp -> Players.isVisible(sender, mp.getBukkit()))
            .map(mp -> mp.getName(NameStyle.VERBOSE))
            .collect(Collectors.toList());

    return TextFormatter.list(names, NamedTextColor.GRAY);
  }

  private Component formatVanishCount(Collection<MatchPlayer> players) {
    return text()
        .append(text(" ("))
        .append(text(getSize(players, true), NamedTextColor.WHITE))
        .append(text(")"))
        .color(NamedTextColor.GRAY)
        .build();
  }

  private int getSize(Collection<MatchPlayer> players, boolean vanished) {
    return Math.toIntExact(
        players.stream().filter(mp -> vanished == isVanished(mp.getBukkit())).count());
  }
}
