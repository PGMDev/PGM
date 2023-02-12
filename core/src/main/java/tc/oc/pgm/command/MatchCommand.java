package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.clock;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityGoal;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public final class MatchCommand {

  @CommandMethod("match|matchinfo")
  @CommandDescription("Show the match info")
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
                    .append(text(getNonVanishedCount(team.getPlayers()), NamedTextColor.WHITE)));

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
            .append(text(getNonVanishedCount(match.getObservers()), NamedTextColor.WHITE))
            .build());

    viewer.sendMessage(
        join(JoinConfiguration.separator(text(" | ", NamedTextColor.DARK_GRAY)), teamCountParts));

    if (!haveGameInfo) return;

    GoalMatchModule gmm = match.getModule(GoalMatchModule.class);
    if (gmm != null && tmm != null && gmm.getGoalsByCompetitor().size() > 0) {
      Multimap<Team, Component> teamGoalTexts = LinkedHashMultimap.create();
      Map<Goal<?>, Component> sharedGoalTexts = new LinkedHashMap<>();

      MatchPlayer player = getMatchPlayer(sender, match);
      Party viewingParty = player == null ? match.getDefaultParty() : player.getParty();

      for (Team team : tmm.getParticipatingTeams()) {
        for (Goal<?> goal : gmm.getGoals(team)) {
          if (goal.hasShowOption(ShowOption.SHOW_INFO)) {
            if (goal.isShared()) {
              sharedGoalTexts.computeIfAbsent(goal, g -> renderGoal(g, null, viewingParty));
            } else if (player != null) {
              teamGoalTexts.put(team, renderGoal(goal, player.getCompetitor(), viewingParty));
            } else {
              teamGoalTexts.put(team, renderGoal(goal, null, viewingParty));
            }
          }
        }
      }

      if (!teamGoalTexts.isEmpty() || !sharedGoalTexts.isEmpty()) {
        viewer.sendMessage(
            translatable("match.info.goals").append(text(":")).color(NamedTextColor.DARK_PURPLE));

        // Team goals
        for (Map.Entry<Team, Collection<Component>> entry : teamGoalTexts.asMap().entrySet()) {
          Team team = entry.getKey();
          Collection<Component> goalTexts = entry.getValue();

          viewer.sendMessage(
              text()
                  .append(space())
                  .append(space())
                  .append(team.getName())
                  .append(text(": ", NamedTextColor.GRAY))
                  .append(join(JoinConfiguration.separator(text("  ")), goalTexts))
                  .build());
        }
        // Shared goals
        viewer.sendMessage(join(JoinConfiguration.separator(text("  ")), sharedGoalTexts.values()));
      }
    }

    ScoreMatchModule smm = match.getModule(ScoreMatchModule.class);
    if (smm != null) {
      viewer.sendMessage(smm.getStatusMessage(getMatchPlayer(sender, match)));
    }
  }

  private long getNonVanishedCount(Collection<MatchPlayer> players) {
    return players.stream()
        .map(MatchPlayer::getBukkit)
        .filter(p -> !Integration.isVanished(p))
        .count();
  }

  private MatchPlayer getMatchPlayer(CommandSender sender, Match match) {
    return sender instanceof Player ? match.getPlayer((Player) sender) : null;
  }

  // Modified from SidebarMatchModule to make formatting easier
  private static Component renderGoal(
      Goal<?> goal, @Nullable Competitor competitor, Party viewingParty) {
    TextComponent.Builder sb = text().append(space());

    sb.append(
        goal.renderSidebarStatusText(competitor, viewingParty)
            .color(goal.renderSidebarStatusColor(competitor, viewingParty)));

    sb.append(space());
    if (goal instanceof ProximityGoal) {
      ProximityGoal<?> proxGoal = (ProximityGoal<?>) goal;
      Component proximity = proxGoal.renderProximity(competitor, viewingParty);
      if (proximity != empty()) {
        TextColor proximityColor = proxGoal.renderProximityColor(competitor, viewingParty);
        sb.append(proximity.color(proximityColor));
        sb.append(space());
      }
    }

    sb.append(
        goal.renderSidebarLabelText(competitor, viewingParty)
            .color(goal.renderSidebarLabelColor(competitor, viewingParty)));

    return sb.build();
  }
}
