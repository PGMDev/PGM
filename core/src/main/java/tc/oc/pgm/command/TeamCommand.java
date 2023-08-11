package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.Greedy;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextParser;

@CommandMethod("team")
public final class TeamCommand {

  @CommandMethod("force <player> [team]")
  @CommandDescription("Force a player onto a team")
  @CommandPermission(Permissions.JOIN_FORCE)
  public void force(
      MatchPlayer sender,
      JoinMatchModule join,
      @Argument("player") MatchPlayer joiner,
      @Argument("team") Party team) {

    final Party oldParty = joiner.getParty();

    if (team != null && !(team instanceof Competitor)) {
      join.leave(joiner, JoinRequest.force());
    } else {
      join.forceJoin(joiner, (Competitor) team);
    }

    sender.sendMessage(
        translatable(
            "join.ok.force",
            NamedTextColor.GRAY,
            joiner.getName(NameStyle.FANCY),
            joiner.getParty().getName(),
            oldParty.getName()));
  }

  @CommandMethod("shuffle")
  @CommandDescription("Shuffle players among the teams")
  @CommandPermission(Permissions.JOIN_FORCE)
  public void shuffle(
      Match match, TeamMatchModule teams, @Flag("a") boolean all, @Flag("f") boolean force) {
    if (match.isRunning() && !force) {
      throw exception("match.shuffle.err");
    }

    List<MatchPlayer> players = new ArrayList<>(all ? match.getPlayers() : match.getParticipants());
    Collections.shuffle(players);
    for (MatchPlayer player : players) {
      teams.forceJoin(player, null);
    }

    match.sendMessage(translatable("match.shuffle.ok", NamedTextColor.GREEN));
  }

  @CommandMethod("alias <team> <name>")
  @CommandDescription("Rename a team")
  @CommandPermission(Permissions.GAMEPLAY)
  public void alias(
      Match match,
      TeamMatchModule teams,
      @Argument("team") Team team,
      @Argument("name") @Greedy String name) {
    if (name.length() > 32) {
      name = name.substring(0, 32);
    }

    for (Team other : teams.getTeams()) {
      if (other.getNameLegacy().equalsIgnoreCase(name)) {
        throw exception("match.alias.err", text(name));
      }
    }

    final Component oldName = team.getName().color(NamedTextColor.GRAY);
    team.setName(name);

    match.sendMessage(translatable("match.alias.ok", oldName, team.getName()));
  }

  @CommandMethod("size <teams> <max-players> [max-overfill]")
  @CommandDescription("Set the max players on a team")
  @CommandPermission(Permissions.RESIZE)
  public void max(
      Audience audience,
      @Argument("teams") Collection<Team> teams,
      @Argument("max-players") int maxPlayers,
      @Argument("max-overfill") Integer maxOverfill) {
    for (Team team : teams) {
      TextParser.assertInRange(maxPlayers, Range.atLeast(team.getMinPlayers()));

      if (maxOverfill == null) maxOverfill = (int) Math.ceil(1.25 * maxPlayers);
      else TextParser.assertInRange(maxOverfill, Range.atLeast(maxPlayers));

      team.setMaxSize(maxPlayers, maxOverfill);
      audience.sendMessage(
          translatable(
              "match.resize.max", team.getName(), text(team.getMaxPlayers(), NamedTextColor.AQUA)));
    }
  }

  @CommandMethod("size <teams> reset")
  @CommandDescription("Reset the max players on a team")
  @CommandPermission(Permissions.RESIZE)
  public void max(Audience audience, @Argument("teams") Collection<Team> teams) {
    for (Team team : teams) {
      team.resetMaxSize();
      audience.sendMessage(
          translatable(
              "match.resize.max", team.getName(), text(team.getMaxPlayers(), NamedTextColor.AQUA)));
    }
  }

  @CommandMethod("min <teams> <min-players>")
  @CommandDescription("Set the min players on a team")
  @CommandPermission(Permissions.RESIZE)
  public void min(
      Audience audience,
      @Argument("teams") Collection<Team> teams,
      @Argument("min-players") int minPlayers) {
    TextParser.assertInRange(minPlayers, Range.atLeast(0));
    for (Team team : teams) {
      team.setMinSize(minPlayers);
      audience.sendMessage(
          translatable(
              "match.resize.min", team.getName(), text(team.getMinPlayers(), NamedTextColor.AQUA)));
    }
  }

  @CommandMethod("min <teams> reset")
  @CommandDescription("Reset the min players on a team")
  @CommandPermission(Permissions.RESIZE)
  public void min(Audience audience, @Argument("teams") Collection<Team> teams) {
    for (Team team : teams) {
      team.resetMinSize();
      audience.sendMessage(
          translatable(
              "match.resize.min", team.getName(), text(team.getMinPlayers(), NamedTextColor.AQUA)));
    }
  }
}
