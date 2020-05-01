package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Switch;
import app.ashcon.intake.parametric.annotation.Text;
import java.util.*;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
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
import tc.oc.pgm.util.text.TextTranslations;

public class TeamCommands {

  @Command(
      aliases = {"myteam", "mt"},
      desc = "Shows you what team you are on")
  public static void myTeam(CommandSender sender, MatchPlayer player) throws CommandException {
    if (player.getParty() instanceof Team) {
      sender.sendMessage(
          ChatColor.GRAY
              + TextTranslations.translate(
                  "match.myTeam",
                  player.getBukkit(),
                  player.getParty().getColoredName() + ChatColor.GRAY));
    } else {
      throw new CommandException(TextTranslations.translate("match.notOnTeam", sender));
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
                "join.ok.force",
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
  public static void shuffle(
      CommandSender sender,
      TeamMatchModule tmm,
      Match match,
      @Switch('a') boolean all,
      @Switch('f') boolean force)
      throws CommandException {
    if (match.isRunning() && !force) {
      throw new CommandException(TextTranslations.translate("match.shuffle.err", sender));
    } else {
      List<MatchPlayer> players =
          new ArrayList<>(all ? match.getPlayers() : match.getParticipants());
      Collections.shuffle(players);
      for (MatchPlayer player : players) {
        tmm.forceJoin(player, null);
      }
      match.sendMessage(new PersonalizedTranslatable("match.shuffle.ok"));
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
      throw new CommandException(TextTranslations.translate("command.teamNotFound", sender));
    }

    if (newName.length() > 32) newName = newName.substring(0, 32);

    for (Team t : tmm.getTeams()) {
      if (t.getName().equalsIgnoreCase(newName)) {
        throw new CommandException(TextTranslations.translate("match.alias.err", sender, newName));
      }
    }

    String oldName = team.getColoredName();
    team.setName(newName);

    match.sendMessage(
        TranslatableComponent.of(
            "match.alias.ok",
            TextComponent.of(oldName, TextColor.GRAY),
            LegacyComponentSerializer.INSTANCE.deserialize(team.getColoredName())));
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
            TextTranslations.translate(
                "command.incorrectUsage",
                sender,
                "<team> (default | [-p max-players] [-o max-overfill])"));
      }

      for (Team team : teams) {
        maxPlayers = maxPlayers == null ? team.getMaxPlayers() : maxPlayers;
        maxOverfill = maxOverfill == null ? maxPlayers : maxOverfill;
        // TODO: Localize this one
        if (maxPlayers < 0) throw new CommandException("max-players cannot be less than 0");

        if (maxOverfill < maxPlayers) // TODO: Localize this one
        throw new CommandException("max-overfill cannot be less than max-players");

        team.setMaxSize(maxPlayers, maxOverfill);
      }
    }

    teams.forEach(
        team ->
            sender.sendMessage(
                team.getColoredName()
                    + ChatColor.WHITE
                    + " now has max size " // TODO: Localize this one
                    + ChatColor.AQUA
                    + team.getMaxPlayers()
                    + ChatColor.WHITE
                    + " and max overfill " // TODO: Localize this one
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
                    + " now has min size " // TODO: Localize this one
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
      throw new CommandException(TextTranslations.translate("command.teamNotFound", sender));

    return teams;
  }
}
