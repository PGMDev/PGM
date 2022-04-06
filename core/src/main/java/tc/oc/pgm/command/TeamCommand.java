package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import com.google.common.collect.Range;
import java.util.*;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.named.NameStyle;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.text.TextParser;

public final class TeamCommand {

  @Command(
      aliases = {"force"},
      desc = "Force a player onto a team",
      perms = Permissions.JOIN_FORCE)
  public void force(
      Match match, TeamMatchModule teams, MatchPlayer sender, Player player, @Nullable Party team) {
    final MatchPlayer joiner = match.getPlayer(player);
    if (joiner == null) throw exception("command.playerNotFound");

    teams.setForced(joiner, true);
    final Party oldParty = joiner.getParty();
    if (team == null) {
      teams.forceJoin(joiner, null);
    } else if (!(team instanceof Competitor)) {
      match.setParty(joiner, team);
    } else {
      teams.forceJoin(joiner, (Competitor) team);
    }

    sender.sendMessage(
        translatable(
            "join.ok.force",
            NamedTextColor.GRAY,
            joiner.getName(NameStyle.FANCY),
            joiner.getParty().getName(),
            oldParty.getName()));
  }

  @Command(
      aliases = {"shuffle"},
      desc = "Shuffle players among the teams",
      flags = "af",
      perms = Permissions.JOIN_FORCE)
  public void shuffle(
      Match match, TeamMatchModule teams, @Switch('a') boolean all, @Switch('f') boolean force) {
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

  @Command(
      aliases = {"alias"},
      desc = "Rename a team",
      usage = "<old name> <new name>",
      perms = Permissions.GAMEPLAY)
  public void alias(Match match, TeamMatchModule teams, Party team, @Text String newName) {
    if (newName.length() > 32) {
      newName = newName.substring(0, 32);
    } else if (!(team instanceof Team)) {
      throw exception("command.teamNotFound");
    }

    for (Team other : teams.getTeams()) {
      if (other.getNameLegacy().equalsIgnoreCase(newName)) {
        throw exception("match.alias.err", text(newName));
      }
    }

    final Component oldName = team.getName().color(NamedTextColor.GRAY);
    ((Team) team).setName(newName);

    match.sendMessage(translatable("match.alias.ok", oldName, team.getName()));
  }

  @Command(
      aliases = {"size"},
      desc = "Set the max players on a team",
      usage = "<team> (reset | <max-players) [max-overfill]",
      perms = Permissions.RESIZE)
  public void max(
      Audience audience,
      TeamMatchModule teams,
      String teamName,
      String maxPlayers,
      @Nullable String maxOverfill) {
    for (Team team : getTeams(teams, teamName)) {
      if (maxPlayers.equalsIgnoreCase("reset")) {
        team.resetMaxSize();
      } else {
        final int max = TextParser.parseInteger(maxPlayers, Range.atLeast(team.getMinPlayers()));
        final int overfill =
            maxOverfill == null
                ? (int) Math.ceil(1.25 * max)
                : TextParser.parseInteger(maxOverfill, Range.atLeast(max));

        team.setMaxSize(max, overfill);
      }

      audience.sendMessage(
          translatable(
              "match.resize.max", team.getName(), text(team.getMaxPlayers(), NamedTextColor.AQUA)));
    }
  }

  @Command(
      aliases = {"min"},
      desc = "Set the min players on a team",
      usage = "<team> (reset | <min-players>)",
      perms = Permissions.RESIZE)
  public void min(Audience audience, TeamMatchModule teams, String teamName, String minPlayers) {
    for (Team team : getTeams(teams, teamName)) {
      if (minPlayers.equalsIgnoreCase("reset")) {
        team.resetMinSize();
      } else {
        team.setMinSize(TextParser.parseInteger(minPlayers, Range.atLeast(0)));
      }

      audience.sendMessage(
          translatable(
              "match.resize.min", team.getName(), text(team.getMinPlayers(), NamedTextColor.AQUA)));
    }
  }

  private Collection<Team> getTeams(TeamMatchModule teams, String query) {
    final Collection<Team> list;
    if (query.equalsIgnoreCase("*")) {
      list = teams.getTeams();
    } else {
      final Team team = teams.bestFuzzyMatch(query);
      list = team == null ? Collections.emptyList() : Collections.singletonList(team);
    }

    if (list.isEmpty()) {
      throw exception("command.teamNotFound");
    }
    return list;
  }
}
