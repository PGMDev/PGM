package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityGoal;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.text.TextTranslations;

// TODO: improve format and translate
public final class MatchCommand {

  @Command(
      aliases = {"match", "matchinfo"},
      desc = "Show the match info")
  public void match(Audience viewer, CommandSender sender, Match match) {
    // indicates whether we have game information from the match yet
    boolean haveGameInfo =
        match.getPhase() == MatchPhase.RUNNING || match.getPhase() == MatchPhase.FINISHED;

    sender.sendMessage(
        LegacyFormatUtils.horizontalLineHeading(
            ChatColor.YELLOW
                + TextTranslations.translate("match.title", sender)
                + " #"
                + match.getId(),
            ChatColor.WHITE,
            LegacyFormatUtils.MAX_CHAT_WIDTH));

    if (haveGameInfo) {
      // show match time
      sender.sendMessage(
          ChatColor.DARK_PURPLE
              + TextTranslations.translate("match.info.time", sender)
              + ": "
              + ChatColor.GOLD
              + TimeUtils.formatDuration(match.getDuration()));
    }

    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    FreeForAllMatchModule ffamm = match.getModule(FreeForAllMatchModule.class);
    List<String> teamCountParts = Lists.newArrayList();

    if (tmm != null) {
      for (Team team : tmm.getTeams()) {
        StringBuilder msg = new StringBuilder();

        String teamName = team.getNameLegacy();
        if (teamName.endsWith(" Team")) teamName = teamName.substring(0, teamName.length() - 5);

        msg.append(team.getColor())
            .append(teamName)
            .append(ChatColor.GRAY)
            .append(": ")
            .append(ChatColor.WHITE)
            .append(
                team.getPlayers().stream()
                    .filter(mp -> !mp.isVanished())
                    .collect(Collectors.toList())
                    .size());

        if (team.getMaxPlayers() != Integer.MAX_VALUE) {
          msg.append(ChatColor.GRAY).append("/").append(team.getMaxPlayers());
        }

        teamCountParts.add(msg.toString());
      }
    } else if (ffamm != null) {
      teamCountParts.add(
          ChatColor.YELLOW
              + TextTranslations.translate("match.info.players", sender)
              + ChatColor.GRAY
              + ": "
              + ChatColor.WHITE
              + match.getParticipants().size()
              + ChatColor.GRAY
              + '/'
              + ffamm.getMaxPlayers());
    }

    teamCountParts.add(
        ChatColor.AQUA
            + TextTranslations.translate("match.info.observers", sender)
            + ChatColor.GRAY
            + ": "
            + ChatColor.WHITE
            + match.getObservers().stream()
                .filter(mp -> !mp.isVanished())
                .collect(Collectors.toList())
                .size());

    sender.sendMessage(Joiner.on(ChatColor.DARK_GRAY + " | ").join(teamCountParts));

    GoalMatchModule gmm = match.getModule(GoalMatchModule.class);
    if (haveGameInfo && gmm != null) {
      if (tmm != null && gmm.getGoalsByCompetitor().size() > 0) {
        Multimap<Team, String> teamGoalTexts = HashMultimap.create();

        MatchPlayer player = getMatchPlayer(sender, match);

        for (Team team : tmm.getParticipatingTeams()) {
          for (Goal<?> goal : gmm.getGoals(team)) {
            if (goal.isVisible()) {
              if (player != null) {
                teamGoalTexts.put(
                    team, renderGoal(goal, player.getCompetitor(), player.getParty()));
              } else {
                teamGoalTexts.put(team, renderGoal(goal, null, match.getDefaultParty()));
              }
            }
          }
        }

        if (!teamGoalTexts.isEmpty()) {
          sender.sendMessage(
              ChatColor.DARK_PURPLE + TextTranslations.translate("match.info.goals", sender) + ":");

          for (Map.Entry<Team, Collection<String>> entry : teamGoalTexts.asMap().entrySet()) {
            Team team = entry.getKey();
            Collection<String> goalTexts = entry.getValue();

            viewer.sendMessage(
                TextComponent.builder()
                    .append("  ")
                    .append(team.getName())
                    .append(": ", TextColor.GRAY)
                    .append(Joiner.on("  ").join(goalTexts))
                    .build());
          }
        }
      } else {
        // FIXME: this is not the best way to handle scores
        ScoreMatchModule smm = match.getModule(ScoreMatchModule.class);
        if (smm != null) {
          sender.sendMessage(smm.getStatusMessage());
        }
      }
    }
  }

  private MatchPlayer getMatchPlayer(CommandSender sender, Match match) {
    return sender instanceof Player ? match.getPlayer((Player) sender) : null;
  }

  // Modified from SidebarMatchModule to make formatting easier
  private static String renderGoal(
      Goal<?> goal, @Nullable Competitor competitor, Party viewingParty) {
    StringBuilder sb = new StringBuilder(" ");

    sb.append(goal.renderSidebarStatusColor(competitor, viewingParty));
    sb.append(goal.renderSidebarStatusText(competitor, viewingParty));

    if (goal instanceof ProximityGoal) {
      sb.append(" ");
      // Show teams their own proximity on shared goals
      sb.append(((ProximityGoal) goal).renderProximity(competitor, viewingParty));
    }

    sb.append(" ");
    sb.append(goal.renderSidebarLabelColor(competitor, viewingParty));
    sb.append(goal.renderSidebarLabelText(competitor, viewingParty));

    return sb.toString();
  }
}
