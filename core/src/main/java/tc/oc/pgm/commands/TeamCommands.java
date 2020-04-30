package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import java.util.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.translations.AllTranslations;

public class TeamCommands {

  @Command(
      aliases = {"myteam", "mt"},
      desc = "Shows you what team you are on")
  public static void myTeam(CommandSender sender, MatchPlayer player) throws CommandException {
    if (player.getParty() instanceof Team) {
      sender.sendMessage(
          ChatColor.GRAY
              + AllTranslations.get()
                  .translate(
                      "command.gameplay.myteam.message",
                      player.getBukkit(),
                      player.getParty().getColoredName() + ChatColor.GRAY));
    } else {
      throw new CommandException(
          AllTranslations.get().translate("command.gameplay.myteam.notOnTeam", sender));
    }
  }

  @Command(
      aliases = {"force"},
      desc = "Force a player onto a team",
      usage = "<player> [team]",
      perms = Permissions.JOIN_FORCE)
  public static void force(
      CommandSender sender,
      Match match,
      TeamMatchModule tmm,
      Player player,
      @Text String teamName) {
    MatchPlayer matchPlayer = match.getPlayer(player);
    Party oldParty = matchPlayer.getParty();

    // Exempt vanished players from being forced
    if (matchPlayer.isVanished()) {
      sender.sendMessage(
          new PersonalizedTranslatable(
                  "command.team.force.exempt", matchPlayer.getStyledName(NameStyle.FANCY))
              .getPersonalizedText()
              .color(ChatColor.RED));
      return;
    }

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
    sender.sendMessage(
        new PersonalizedTranslatable(
                "command.team.force.success",
                matchPlayer.getStyledName(NameStyle.FANCY),
                matchPlayer.getParty().getComponentName(),
                oldParty.getComponentName())
            .getPersonalizedText()
            .color(ChatColor.GRAY));
  }

  @Command(
      aliases = {"shuffle"},
      desc = "Shuffle the teams",
      perms = Permissions.JOIN_FORCE)
  public static void shuffle(CommandSender sender, TeamMatchModule tmm, Match match)
      throws CommandException {
    if (match.isRunning()) {
      throw new CommandException(
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
      aliases = {"alias"},
      desc = "Rename a team",
      usage = "<old name> <new name>",
      perms = Permissions.GAMEPLAY)
  public static void alias(
      CommandSender sender, TeamMatchModule tmm, Match match, String target, @Text String newName)
      throws CommandException {
    Team team = tmm.bestFuzzyMatch(target);

    if (team == null) {
      throw new CommandException(AllTranslations.get().translate("command.teamNotFound", sender));
    }

    if (newName.length() > 32) {
      throw new CommandException("Team name cannot be longer than 32 characters");
    }

    for (Team t : tmm.getTeams()) {
      if (t.getName().equalsIgnoreCase(newName)) {
        throw new CommandException(
            AllTranslations.get().translate("command.team.alias.nameAlreadyUsed", sender, newName));
      }
    }

    String oldName = team.getColoredName();
    team.setName(newName);

    match.sendMessage(oldName + ChatColor.GRAY + " renamed to " + team.getColoredName());
  }

  @Command(
      aliases = {"max", "size"},
      desc = "Change the maximum size and overfill size of a team.",
      usage = "<team> (default | [-p max-players] [-o max-overfill])",
      flags = "po",
      perms = Permissions.RESIZE)
  public static void maxPlayers(
      CommandSender sender,
      TeamMatchModule tmm,
      String teamName,
      Optional<String> max,
      @Fallback(Type.NULL) @Switch('p') Integer maxPlayers,
      @Fallback(Type.NULL) @Switch('o') Integer maxOverfill)
      throws CommandException {
    Collection<Team> teams = getTeams(sender, tmm, teamName);

    if (max.isPresent() && "default".equals(max.get())) {
      teams.forEach(Team::resetMinSize);
    } else {
      if (maxPlayers == null && maxOverfill == null) {
        throw new CommandException(
            AllTranslations.get()
                .translate(
                    "commands.incorrectUsage",
                    sender,
                    "<team> (default | [-p max-players] [-o max-overfill])"));
      }

      for (Team team : teams) {
        maxPlayers = maxPlayers == null ? team.getMaxPlayers() : maxPlayers;
        maxOverfill = maxOverfill == null ? maxPlayers : maxOverfill;

        if (maxPlayers < 0) throw new CommandException("max-players cannot be less than 0");

        if (maxOverfill < maxPlayers)
          throw new CommandException("max-overfill cannot be less than max-players");

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
      aliases = {"min"},
      desc = "Change the minimum size of a team.",
      usage = "<team> (default | <min-players>)",
      perms = Permissions.RESIZE)
  public static void min(CommandSender sender, TeamMatchModule tmm, String teamName, String value)
      throws CommandException {
    Collection<Team> teams = getTeams(sender, tmm, teamName);

    if ("default".equals(value)) {
      teams.forEach(Team::resetMinSize);
    } else {
      int minPlayers = Integer.parseInt(value);
      if (minPlayers < 0) throw new CommandException("min-players cannot be less than 0");
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
      CommandSender sender, TeamMatchModule tmm, String teamName) throws CommandException {
    Collection<Team> teams;

    // Check for all (*) selector
    if (teamName.equals("*")) teams = tmm.getTeams();
    else teams = Collections.singletonList(tmm.bestFuzzyMatch(teamName));

    if (teams.size() == 0)
      throw new CommandException(AllTranslations.get().translate("command.teamNotFound", sender));

    return teams;
  }
}
