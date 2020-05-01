package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.VanishManager;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.Components;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.NameStyle;

public class ListCommands {

  private final VanishManager vanish;

  public ListCommands(VanishManager vanish) {
    this.vanish = vanish;
  }

  @Command(
      aliases = {"list", "who", "online", "ls"},
      desc = "View a list of online players")
  public void list(CommandSender sender, MatchPlayer player, Match match) {
    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    if (tmm != null) {
      sender.sendMessage(
          new PersonalizedTranslatable("command.list.teams")
              .getPersonalizedText()
              .color(ChatColor.GRAY));
      tmm.getTeams()
          .forEach(
              team ->
                  sendTeamInfo(
                      sender, team.getComponentName(), team.getPlayers(), team.getMaxPlayers()));
    } else {
      sendTeamInfo(
          sender,
          new PersonalizedTranslatable("command.list.participants"),
          match.getParticipants(),
          match.getMaxPlayers());
    }
    // Observers
    sendTeamInfo(
        sender,
        match.getDefaultParty().getComponentName(),
        match.getDefaultParty().getPlayers(),
        -1);

    // Total count
    sender.sendMessage(
        new PersonalizedTranslatable(
                "command.list.online",
                new PersonalizedText(
                    Integer.toString(getSize(match.getPlayers(), false)), ChatColor.GREEN))
            .getPersonalizedText()
            .color(ChatColor.GRAY));
  }

  private void sendTeamInfo(
      CommandSender sender, Component teamName, Collection<MatchPlayer> players, int max) {
    Component teamLine =
        new PersonalizedText(
            teamName,
            new PersonalizedText(": ", ChatColor.GRAY),
            new PersonalizedText(Integer.toString(getSize(players, false))),
            max != -1
                ? new PersonalizedText("/" + Integer.toString(max)).color(ChatColor.GRAY)
                : Components.blank(),
            getSize(players, true) > 0 && sender.hasPermission(Permissions.STAFF)
                ? formatVanishCount(players)
                : Components.blank());
    sender.sendMessage(teamLine);
    if (!players.isEmpty()) {
      sender.sendMessage(formatNames(players, sender));
    }
  }

  private Component formatNames(Collection<MatchPlayer> players, CommandSender sender) {
    List<Component> names =
        players.stream()
            .filter(
                mp ->
                    sender.hasPermission(Permissions.STAFF) ? true : !vanish.isVanished(mp.getId()))
            .map(mp -> new Component(mp.getStyledName(NameStyle.VERBOSE).render(sender)))
            .collect(Collectors.toList());

    return new Component(Components.join(new PersonalizedText(", ", ChatColor.GRAY), names));
  }

  private Component formatVanishCount(Collection<MatchPlayer> players) {
    return new PersonalizedText(
            new PersonalizedText(" ("),
            new PersonalizedText(Integer.toString(getSize(players, true))).color(ChatColor.WHITE),
            new PersonalizedText(")"))
        .color(ChatColor.GRAY);
  }

  private int getSize(Collection<MatchPlayer> players, boolean vanished) {
    return Math.toIntExact(
        players.stream()
            .filter(mp -> vanished ? vanish.isVanished(mp.getId()) : !vanish.isVanished(mp.getId()))
            .count());
  }
}
