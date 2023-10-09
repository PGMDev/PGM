package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.player.PlayerComponent.player;
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
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextParser;

@CommandMethod("team")
public final class TeamCommand {

  @CommandMethod("force <player> [team]")
  @CommandDescription("Force a player onto a team")
  @CommandPermission(Permissions.JOIN_FORCE)
  public void force(
      CommandSender sender,
      JoinMatchModule join,
      @Argument("player") MatchPlayer joiner,
      @Argument("team") Party team) {

    final Party oldParty = joiner.getParty();

    if (team != null && !(team instanceof Competitor)) {
      join.leave(joiner, JoinRequest.force());
    } else {
      join.forceJoin(joiner, (Competitor) team);
    }
    ChatDispatcher.broadcastAdminChatMessage(
        translatable(
            "join.ok.force.announce",
            player(sender, NameStyle.FANCY),
            joiner.getName(NameStyle.FANCY),
            joiner.getParty().getName(),
            oldParty.getName()),
        joiner.getMatch());
  }

  @CommandMethod("shuffle")
  @CommandDescription("Shuffle players among the teams")
  @CommandPermission(Permissions.JOIN_FORCE)
  public void shuffle(
      Match match,
      CommandSender sender,
      TeamMatchModule teams,
      @Flag("a") boolean all,
      @Flag("f") boolean force) {
    if (match.isRunning() && !force) {
      throw exception("match.shuffle.err");
    }

    List<MatchPlayer> players = new ArrayList<>(all ? match.getPlayers() : match.getParticipants());
    Collections.shuffle(players);
    for (MatchPlayer player : players) {
      teams.forceJoin(player, null);
    }

    ChatDispatcher.broadcastAdminChatMessage(
        translatable("match.shuffle.announce.ok", player(sender, NameStyle.FANCY)), match);
  }

  @CommandMethod("alias <team> <name>")
  @CommandDescription("Rename a team")
  @CommandPermission(Permissions.GAMEPLAY)
  public void alias(
      Match match,
      CommandSender sender,
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

    ChatDispatcher.broadcastAdminChatMessage(
        translatable(
            "match.alias.announce.ok", player(sender, NameStyle.FANCY), oldName, team.getName()),
        match);
  }

  @CommandMethod("scale <teams> <factor>")
  @CommandDescription("Resizes all teams by a given factor")
  @CommandPermission(Permissions.RESIZE)
  public void scale(
      CommandSender sender,
      Match match,
      @Argument("teams") Collection<Team> teams,
      @Argument("factor") double scale) {
    for (Team team : teams) {
      int maxOverfill = (int) (team.getMaxOverfill() * scale);
      int maxSize = (int) (team.getMaxPlayers() * scale);
      team.setMaxSize(maxSize, maxOverfill);

      ChatDispatcher.broadcastAdminChatMessage(
          translatable(
              "match.resize.announce.max",
              player(sender, NameStyle.FANCY),
              team.getName(),
              text(team.getMaxPlayers(), NamedTextColor.AQUA)),
          match);
    }
  }

  @CommandMethod("size <teams> <max-players> [max-overfill]")
  @CommandDescription("Set the max players on a team")
  @CommandPermission(Permissions.RESIZE)
  public void max(
      CommandSender sender,
      Match match,
      @Argument("teams") Collection<Team> teams,
      @Argument("max-players") int maxPlayers,
      @Argument("max-overfill") Integer maxOverfill) {
    for (Team team : teams) {
      TextParser.assertInRange(maxPlayers, Range.atLeast(team.getMinPlayers()));

      if (maxOverfill == null) maxOverfill = (int) Math.ceil(1.25 * maxPlayers);
      else TextParser.assertInRange(maxOverfill, Range.atLeast(maxPlayers));

      team.setMaxSize(maxPlayers, maxOverfill);
      ChatDispatcher.broadcastAdminChatMessage(
          translatable(
              "match.resize.announce.max",
              player(sender, NameStyle.FANCY),
              team.getName(),
              text(team.getMaxPlayers(), NamedTextColor.AQUA)),
          match);
    }
  }

  @CommandMethod("size <teams> reset")
  @CommandDescription("Reset the max players on a team")
  @CommandPermission(Permissions.RESIZE)
  public void max(CommandSender sender, Match match, @Argument("teams") Collection<Team> teams) {
    for (Team team : teams) {
      team.resetMaxSize();
      ChatDispatcher.broadcastAdminChatMessage(
          translatable(
              "match.resize.announce.max",
              player(sender, NameStyle.FANCY),
              team.getName(),
              text(team.getMaxPlayers(), NamedTextColor.AQUA)),
          match);
    }
  }

  @CommandMethod("min <teams> <min-players>")
  @CommandDescription("Set the min players on a team")
  @CommandPermission(Permissions.RESIZE)
  public void min(
      CommandSender sender,
      Match match,
      @Argument("teams") Collection<Team> teams,
      @Argument("min-players") int minPlayers) {
    TextParser.assertInRange(minPlayers, Range.atLeast(0));
    for (Team team : teams) {
      team.setMinSize(minPlayers);
      ChatDispatcher.broadcastAdminChatMessage(
          translatable(
              "match.resize.announce.min",
              player(sender, NameStyle.FANCY),
              team.getName(),
              text(team.getMaxPlayers(), NamedTextColor.AQUA)),
          match);
    }
  }

  @CommandMethod("min <teams> reset")
  @CommandDescription("Reset the min players on a team")
  @CommandPermission(Permissions.RESIZE)
  public void min(CommandSender sender, Match match, @Argument("teams") Collection<Team> teams) {
    for (Team team : teams) {
      team.resetMinSize();
      ChatDispatcher.broadcastAdminChatMessage(
          translatable(
              "match.resize.announce.min",
              player(sender, NameStyle.FANCY),
              team.getName(),
              text(team.getMaxPlayers(), NamedTextColor.AQUA)),
          match);
    }
  }
}
