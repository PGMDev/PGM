package tc.oc.pgm.commands;

import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.param.ArgFlag;
import java.util.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;

@CommandContainer
public class TeamCommands {

  @Command(
          name = "myteam",
          aliases = {"mt"},
          desc = "Shows you what team you are on")
  public static void myTeam(CommandSender sender, MatchPlayer player){
    if (player.getParty() instanceof Team) {
      sender.sendMessage(
          ChatColor.GRAY
              + AllTranslations.get()
                  .translate(
                      "command.gameplay.myteam.message",
                      player.getBukkit(),
                      player.getParty().getColoredName() + ChatColor.GRAY));
    } else {
      throw new IllegalStateException(
          AllTranslations.get().translate("command.gameplay.myteam.notOnTeam", sender));
    }
  }

  @Command(
      name = "force",
      desc = "Force a player onto a team",
      descFooter = "<player> [team]")
  public static void force(Match match, TeamMatchModule tmm, Player player, @Text String teamName) {
    MatchPlayer matchPlayer = match.getPlayer(player);
    if (teamName != null) {
      if (teamName.trim().toLowerCase().startsWith("obs")) {
        match.setParty(matchPlayer, match.getDefaultParty());
      } else {
        Team team = tmm.bestFuzzyMatch(teamName);
        tmm.forceJoin(matchPlayer, team);
      }
    } else {
      tmm.forceJoin(matchPlayer, null);
    }
  }

  @Command(
      name = "shuffle",
      desc = "Shuffle the teams")
  public static void shuffle(CommandSender sender, TeamMatchModule tmm, Match match)
  {
    if (match.isRunning()) {
      throw new IllegalStateException(
          AllTranslations.get().translate("command.team.shuffle.matchRunning", sender));
    } else {
      List<Team> teams = new ArrayList<>(tmm.getParticipatingTeams());
      List<MatchPlayer> participating = new ArrayList<>(match.getParticipants());
      Collections.shuffle(participating);
      for (int i = 0; i < participating.size(); i++) {
        tmm.forceJoin(participating.get(i), teams.get((i * teams.size()) / participating.size()));
      }
      match.sendMessage(new PersonalizedTranslatable("command.team.shuffle.success"));
    }
  }

  @Command(
      name = "alias",
      desc = "Rename a team",
      descFooter = "<old name> <new name>")
  public static void alias(
      CommandSender sender, TeamMatchModule tmm, Match match, String target, @Text String newName)
  {
    Team team = tmm.bestFuzzyMatch(target);

    if (team == null) {
      throw new NoSuchElementException(AllTranslations.get().translate("command.teamNotFound", sender));
    }

    if (newName.length() > 32) {
      throw new IllegalArgumentException("Team name cannot be longer than 32 characters");
    }

    for (Team t : tmm.getTeams()) {
      if (t.getName().equalsIgnoreCase(newName)) {
        throw new IllegalArgumentException(
            AllTranslations.get().translate("command.team.alias.nameAlreadyUsed", sender, newName));
      }
    }

    String oldName = team.getColoredName();
    team.setName(newName);

    match.sendMessage(oldName + ChatColor.GRAY + " renamed to " + team.getColoredName());
  }

  @Command(
          name = "max",
          aliases= {"size"},
          desc = "Change the maximum size and overfill size of a team.",
          descFooter = "<team> (default | [-p max-players] [-o max-overfill])")
  public static void maxPlayers(
      CommandSender sender,
      TeamMatchModule tmm,
      String teamName,
      Optional<String> max,
      @ArgFlag(name = 'p', desc = "Define the max amount of players on a team") Integer maxPlayers,
      @ArgFlag(name = 'o', desc = "Define the max amount of overfill permitted on a team") Integer maxOverfill)
 {
    Collection<Team> teams = getTeams(sender, tmm, teamName);

    if (max.isPresent() && "default".equals(max.get())) {
      teams.forEach(Team::resetMinSize);
    } else {
      if (maxPlayers == null && maxOverfill == null) {
        throw new IllegalArgumentException(
            AllTranslations.get()
                .translate(
                    "commands.incorrectUsage",
                    sender,
                    "<team> (default | [-p max-players] [-o max-overfill])"));
      }

      for (Team team : teams) {
        maxPlayers = maxPlayers == null ? team.getMaxPlayers() : maxPlayers;
        maxOverfill = maxOverfill == null ? maxPlayers : maxOverfill;

        if (maxPlayers < 0) throw new IllegalArgumentException("max-players cannot be less than 0");

        if (maxOverfill < maxPlayers)
          throw new IllegalArgumentException("max-overfill cannot be less than max-players");

        team.setMaxSize(maxPlayers, maxOverfill);
      }
    }

    teams.forEach(
        team ->
            sender.sendMessage(
                team.getColoredName()
                    + ChatColor.WHITE
                    + " now has max size "
                    + ChatColor.AQUA
                    + team.getMaxPlayers()
                    + ChatColor.WHITE
                    + " and max overfill "
                    + ChatColor.AQUA
                    + team.getMaxOverfill()));
  }

  @Command(
      name = "min",
      desc = "Change the minimum size of a team.",
      descFooter = "<team> (default | <min-players>)")
  public static void min(CommandSender sender, TeamMatchModule tmm, String teamName, String value)
  {
    Collection<Team> teams = getTeams(sender, tmm, teamName);

    if ("default".equals(value)) {
      teams.forEach(Team::resetMinSize);
    } else {
      int minPlayers = Integer.parseInt(value);
      if (minPlayers < 0) throw new IllegalArgumentException("min-players cannot be less than 0");
      teams.forEach(team -> team.setMinSize(minPlayers));
    }

    teams.forEach(
        team ->
            sender.sendMessage(
                team.getColoredName()
                    + ChatColor.WHITE
                    + " now has min size "
                    + ChatColor.AQUA
                    + team.getMinPlayers()));
  }

  private static Collection<Team> getTeams(
      CommandSender sender, TeamMatchModule tmm, String teamName){
    Collection<Team> teams;

    // Check for all (*) selector
    if (teamName.equals("*")) teams = tmm.getTeams();
    else teams = Collections.singletonList(tmm.bestFuzzyMatch(teamName));

    if (teams.size() == 0)
      throw new NoSuchElementException(AllTranslations.get().translate("command.teamNotFound", sender));

    return teams;
  }
}
