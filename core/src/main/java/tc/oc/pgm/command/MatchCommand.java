package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.clock;

import app.ashcon.intake.Command;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.api.goal.Goal;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityGoal;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.text.TextFormatter;
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

    viewer.sendMessage(
        TextFormatter.horizontalLineHeading(
            sender,
            translatable("match.title")
                .append(text(" #" + match.getId()))
                .color(NamedTextColor.YELLOW),
            NamedTextColor.WHITE,
            TextFormatter.MAX_CHAT_WIDTH));

    if (haveGameInfo) {
      // show match time
      viewer.sendMessage(
          translatable("match.info.time", NamedTextColor.DARK_PURPLE)
              .append(text(": ", NamedTextColor.DARK_PURPLE))
              .append(clock(match.getDuration()).color(NamedTextColor.GOLD)));
    }

    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    FreeForAllMatchModule ffamm = match.getModule(FreeForAllMatchModule.class);
    List<Component> teamCountParts = Lists.newArrayList();

    if (tmm != null) {
      for (Team team : tmm.getTeams()) {
        TextComponent.Builder msg = text();

        String teamName = team.getNameLegacy();
        if (teamName.endsWith(" Team")) teamName = teamName.substring(0, teamName.length() - 5);

        msg.append(text(teamName, TextFormatter.convert(team.getColor())))
            .append(
                text(": ", NamedTextColor.GRAY)
                    .append(
                        text(
                            team.getPlayers().stream().filter(mp -> !mp.isVanished()).count(),
                            NamedTextColor.WHITE)));

        if (team.getMaxPlayers() != Integer.MAX_VALUE) {
          msg.append(text("/" + team.getMaxPlayers(), NamedTextColor.GRAY));
        }

        teamCountParts.add(msg.build());
      }
    } else if (ffamm != null) {
      teamCountParts.add(
          text()
              .append(
                  translatable("match.info.players", NamedTextColor.YELLOW)
                      .append(text(": ", NamedTextColor.GRAY))
                      .append(text(match.getParticipants().size(), NamedTextColor.WHITE))
                      .append(text("/" + ffamm.getMaxPlayers(), NamedTextColor.GRAY)))
              .build());
    }

    teamCountParts.add(
        text()
            .append(
                text(
                    TextTranslations.translate("match.info.observers", sender),
                    NamedTextColor.AQUA))
            .append(text(": ", NamedTextColor.GRAY))
            .append(
                text(
                    match.getObservers().stream().filter(mp -> !mp.isVanished()).count(),
                    NamedTextColor.WHITE))
            .build());

    viewer.sendMessage(join(text(" | ", NamedTextColor.DARK_GRAY), teamCountParts));

    GoalMatchModule gmm = match.getModule(GoalMatchModule.class);
    if (haveGameInfo && gmm != null) {
      if (tmm != null && gmm.getGoalsByCompetitor().size() > 0) {
        Multimap<Team, Component> teamGoalTexts = HashMultimap.create();

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
          viewer.sendMessage(
              translatable("match.info.goals").append(text(":")).color(NamedTextColor.DARK_PURPLE));

          for (Map.Entry<Team, Collection<Component>> entry : teamGoalTexts.asMap().entrySet()) {
            Team team = entry.getKey();
            Collection<Component> goalTexts = entry.getValue();

            viewer.sendMessage(
                text()
                    .append(space())
                    .append(space())
                    .append(team.getName())
                    .append(text(": ", NamedTextColor.GRAY))
                    .append(join(text("  "), goalTexts))
                    .build());
          }
        }
      } else {
        // FIXME: this is not the best way to handle scores
        ScoreMatchModule smm = match.getModule(ScoreMatchModule.class);
        if (smm != null) {
          viewer.sendMessage(smm.getStatusMessage(getMatchPlayer(sender, match)));
        }
      }
    }
  }

  private MatchPlayer getMatchPlayer(CommandSender sender, Match match) {
    return sender instanceof Player ? match.getPlayer((Player) sender) : null;
  }

  // Modified from SidebarMatchModule to make formatting easier
  private static Component renderGoal(
      Goal<?> goal, @Nullable Competitor competitor, Party viewingParty) {
    TextComponent.Builder sb = text().append(space());

    sb.append(
        text(
            goal.renderSidebarStatusText(competitor, viewingParty),
            TextFormatter.convert(goal.renderSidebarStatusColor(competitor, viewingParty))));

    if (goal instanceof ProximityGoal) {
      sb.append(space());
      // Show teams their own proximity on shared goals
      sb.append(text(((ProximityGoal) goal).renderProximity(competitor, viewingParty)));
    }

    sb.append(space());
    sb.append(
        goal.renderSidebarLabelText(competitor, viewingParty)
            .color(TextFormatter.convert(goal.renderSidebarLabelColor(competitor, viewingParty))));

    return sb.build();
  }
}
