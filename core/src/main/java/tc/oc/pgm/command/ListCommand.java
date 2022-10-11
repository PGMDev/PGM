package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import app.ashcon.intake.Command;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.command.graph.Sender;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

// TODO: clean up format and use new components
public final class ListCommand {

  @Command(
      aliases = {"list", "who", "online", "ls"},
      desc = "View a list of online players")
  public void list(Sender sender) {
    Match match = sender.getMatch();
    TeamMatchModule tmm = sender.getMatch().getModule(TeamMatchModule.class);
    if (tmm != null) {
      sender.sendMessage(translatable("command.list.teams", NamedTextColor.GRAY));
      tmm.getTeams()
          .forEach(
              team ->
                  sendTeamInfo(sender, team.getName(), team.getPlayers(), team.getMaxPlayers()));
    } else {
      sendTeamInfo(
          sender,
          translatable("command.list.participants"),
          match.getParticipants(),
          match.getMaxPlayers());
    }
    // Observers
    sendTeamInfo(
        sender, match.getDefaultParty().getName(), match.getDefaultParty().getPlayers(), -1);

    // Total count
    sender.sendMessage(
        translatable(
            "command.list.online",
            NamedTextColor.GRAY,
            text(getSize(match.getPlayers(), false), NamedTextColor.GREEN)));
  }

  private void sendTeamInfo(
      Sender sender, Component teamName, Collection<MatchPlayer> players, int max) {
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
    sender.sendMessage(teamLine);
    if (!players.isEmpty()) {
      sender.sendMessage(formatNames(players, sender));
    }
  }

  private Component formatNames(Collection<MatchPlayer> players, Sender sender) {
    List<Component> names =
        players.stream()
            .filter(mp -> sender.hasPermission(Permissions.STAFF) || !isVanished(mp.getId()))
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
        players.stream().filter(mp -> vanished == isVanished(mp.getId())).count());
  }

  private boolean isVanished(UUID playerId) {
    return PGM.get().getVanishManager().isVanished(playerId);
  }
}
