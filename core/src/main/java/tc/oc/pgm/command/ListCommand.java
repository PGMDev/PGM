package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

// TODO: clean up format and use new components
public final class ListCommand {

  @Command(
      aliases = {"list", "who", "online", "ls"},
      desc = "View a list of online players")
  public void list(CommandSender sender, Audience viewer, Match match) {
    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    if (tmm != null) {
      viewer.sendMessage(TranslatableComponent.of("command.list.teams", TextColor.GRAY));
      tmm.getTeams()
          .forEach(
              team ->
                  sendTeamInfo(
                      viewer, sender, team.getName(), team.getPlayers(), team.getMaxPlayers()));
    } else {
      sendTeamInfo(
          viewer,
          sender,
          TranslatableComponent.of("command.list.participants"),
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
        TranslatableComponent.of(
            "command.list.online",
            TextColor.GRAY,
            TextComponent.of(
                Integer.toString(getSize(match.getPlayers(), false)), TextColor.GREEN)));
  }

  private void sendTeamInfo(
      Audience viewer,
      CommandSender sender,
      Component teamName,
      Collection<MatchPlayer> players,
      int max) {
    Component teamLine =
        TextComponent.builder()
            .append(teamName)
            .append(": ", TextColor.GRAY)
            .append(Integer.toString(getSize(players, false)))
            .append(
                max != -1
                    ? TextComponent.of("/" + Integer.toString(max), TextColor.GRAY)
                    : TextComponent.empty())
            .append(
                getSize(players, true) > 0 && sender.hasPermission(Permissions.STAFF)
                    ? formatVanishCount(players)
                    : TextComponent.empty())
            .build();
    viewer.sendMessage(teamLine);
    if (!players.isEmpty()) {
      viewer.sendMessage(formatNames(players, sender));
    }
  }

  private Component formatNames(Collection<MatchPlayer> players, CommandSender sender) {
    List<Component> names =
        players.stream()
            .filter(mp -> sender.hasPermission(Permissions.STAFF) || !isVanished(mp.getId()))
            .map(mp -> mp.getName(NameStyle.VERBOSE))
            .collect(Collectors.toList());

    return TextFormatter.list(names, TextColor.GRAY);
  }

  private Component formatVanishCount(Collection<MatchPlayer> players) {
    return TextComponent.builder()
        .append(" (")
        .append(Integer.toString(getSize(players, true)), TextColor.WHITE)
        .append(")")
        .color(TextColor.GRAY)
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
