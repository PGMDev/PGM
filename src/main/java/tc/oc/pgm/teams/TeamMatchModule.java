package tc.oc.pgm.teams;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.math.Fraction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.GenericJoinResult;
import tc.oc.pgm.join.JoinHandler;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.join.QueuedParticipants;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;
import tc.oc.pgm.teams.events.TeamResizeEvent;
import tc.oc.util.StringUtils;

@ListenerScope(MatchScope.LOADED)
public class TeamMatchModule implements MatchModule, Listener, JoinHandler {

  class NeedMorePlayers implements UnreadyReason {
    final @Nullable Team team;
    final int players;

    NeedMorePlayers(@Nullable Team team, int players) {
      this.team = team;
      this.players = players;
    }

    @Override
    public Component getReason() {
      if (team != null) {
        if (players == 1) {
          return new PersonalizedTranslatable(
              "start.needMorePlayers.team.singular",
              new PersonalizedText(String.valueOf(players), ChatColor.AQUA),
              team.getComponentName());
        } else {
          return new PersonalizedTranslatable(
              "start.needMorePlayers.team.plural",
              new PersonalizedText(String.valueOf(players), ChatColor.AQUA),
              team.getComponentName());
        }
      } else {
        if (players == 1) {
          return new PersonalizedTranslatable(
              "start.needMorePlayers.ffa.singular",
              new PersonalizedText(String.valueOf(players), ChatColor.AQUA));
        } else {
          return new PersonalizedTranslatable(
              "start.needMorePlayers.ffa.plural",
              new PersonalizedText(String.valueOf(players), ChatColor.AQUA));
        }
      }
    }

    @Override
    public boolean canForceStart() {
      return true;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{players=" + players + " team=" + team + "}";
    }
  };

  public static class TeamJoinResult extends GenericJoinResult {
    public TeamJoinResult(Status status, @Nullable Team competitor, boolean priorityKick) {
      super(status, competitor, priorityKick);
    }

    public TeamJoinResult(Status status) {
      this(status, null, false);
    }

    public @Nullable Team getTeam() {
      return (Team) getCompetitor();
    }
  }

  // All teams in the match
  private final Set<Team> teams;
  private final boolean requireEven;

  // Players who autojoined their current team
  private final Set<MatchPlayer> autoJoins = new HashSet<>();

  // Minimum at any time of the number of additional players needed to start the match
  private int minPlayersNeeded = Integer.MAX_VALUE;

  private final JoinMatchModule jmm;
  private final Match match;

  public TeamMatchModule(Match match, Set<TeamFactory> teamFactories, boolean requireEven) {
    this.match = match;
    this.teams = new HashSet<>(teamFactories.size());
    this.requireEven = requireEven;

    for (TeamFactory teamFactory : teamFactories) {
      this.teams.add(teamFactory.createTeam(match));
    }

    this.jmm = match.needModule(JoinMatchModule.class);
  }

  @Override
  public void load() {
    for (Team team : teams) {
      match.addParty(team);
    }

    match.needModule(JoinMatchModule.class).registerHandler(this);

    updateMaxPlayers();
    updateReadiness();
  }

  protected void updateMaxPlayers() {
    int maxPlayers = 0;
    for (Team team : teams) {
      maxPlayers += team.getMaxPlayers();
    }
    match.setMaxPlayers(maxPlayers);
  }

  protected void updateReadiness() {
    if (match.isRunning()) return;

    final int playersQueued =
        match.needModule(JoinMatchModule.class).getQueuedParticipants().getPlayers().size();
    final int playersJoined = match.getParticipants().size();

    Team singleTeam = null;
    int teamNeeded = 0;
    for (Team t : teams) {
      int p = t.getMinPlayers() - t.getPlayers().size();
      if (p > 0) {
        singleTeam = teamNeeded == 0 ? t : null;
        teamNeeded += p;
      }
    }
    teamNeeded -= playersQueued;

    int globalNeeded = Config.minimumPlayers() - playersJoined - playersQueued;

    int playersNeeded;
    if (globalNeeded > teamNeeded) {
      playersNeeded = globalNeeded;
      singleTeam = null;
    } else {
      playersNeeded = teamNeeded;
    }

    final StartMatchModule smm = match.needModule(StartMatchModule.class);
    if (playersNeeded > 0) {
      smm.addUnreadyReason(new NeedMorePlayers(singleTeam, playersNeeded));

      // Whenever playersNeeded reaches a new minimum, reset the unready timeout
      if (playersNeeded < minPlayersNeeded) {
        minPlayersNeeded = playersNeeded;
        smm.restartUnreadyTimeout();
      }
    } else {
      smm.removeUnreadyReason(NeedMorePlayers.class);
    }
  }

  public Set<Team> getTeams() {
    return this.teams;
  }

  public Set<Team> getParticipatingTeams() {
    return this.teams;
  }

  public Team getTeam(TeamFactory info) {
    if (info == null) return null;
    for (Team team : this.teams) {
      if (team.getInfo() == info) {
        return team;
      }
    }
    return null;
  }

  public @Nullable Team bestFuzzyMatch(String name) {
    return bestFuzzyMatch(name, 0.9);
  }

  public @Nullable Team bestFuzzyMatch(String name, double threshold) {
    Map<String, Team> byName = new HashMap<>();
    for (Team team : getTeams()) byName.put(team.getName(), team);
    return StringUtils.bestFuzzyMatch(name, byName, threshold);
  }

  protected void setAutoJoin(MatchPlayer player, boolean autoJoined) {
    if (autoJoined) {
      autoJoins.add(player);
    } else {
      autoJoins.remove(player);
    }
  }

  protected boolean isAutoJoin(MatchPlayer player) {
    return autoJoins.contains(player);
  }

  public boolean canSwitchTeams(MatchPlayer joining) {
    return Config.Teams.allowSwitch() || !match.isRunning();
  }

  public boolean canChooseTeam(MatchPlayer joining) {
    return Config.Teams.allowChoose() && joining.getBukkit().hasPermission(Permissions.JOIN_CHOOSE);
  }

  @Override
  public boolean forceJoin(MatchPlayer joining, @Nullable Competitor forcedParty) {
    if (forcedParty instanceof Team) {
      return forceJoin(joining, (Team) forcedParty, false);
    } else if (forcedParty == null) {
      return forceJoin(joining, getEmptiestTeam(), true);
    } else {
      return false;
    }
  }

  public boolean forceJoin(MatchPlayer player, Team newTeam, boolean autoJoin) {
    checkNotNull(newTeam);
    Party oldTeam = player.getParty();
    if (oldTeam == newTeam) return true;

    if (match.setParty(player, newTeam)) {
      setAutoJoin(player, autoJoin);
      return true;
    } else {
      return false;
    }
  }

  private boolean requireEvenTeams() {
    if (!requireEven) return false;

    // If any teams are unequal in size, don't try to even the teams
    // TODO: This could be done, it's just more complicated
    int size = -1;
    for (Team team : getTeams()) {
      if (size == -1) {
        size = team.getMaxOverfill();
      } else if (size != team.getMaxOverfill()) {
        return false;
      }
    }

    return true;
  }

  public boolean areTeamsEven() {
    return areTeamsEvenAfterJoin(null, null);
  }

  /** Do all teams have equal fullness ratios? */
  public boolean areTeamsEvenAfterJoin(@Nullable MatchPlayer joining, @Nullable Team newTeam) {
    Fraction commonFullness = null;
    for (Team team : getParticipatingTeams()) {
      Fraction teamFullness = team.getFullnessAfterJoin(joining, newTeam);
      if (commonFullness == null) {
        commonFullness = teamFullness;
      } else if (!commonFullness.equals(teamFullness)) {
        return false;
      }
    }
    return true;
  }

  /** Return the most full participating team */
  public Team getFullestTeam() {
    Team fullestTeam = null;
    float maxFullness = Float.MIN_VALUE;

    for (Team team : this.getParticipatingTeams()) {
      float fullness = team.getFullness(false);
      if (fullness > maxFullness) {
        fullestTeam = team;
        maxFullness = fullness;
      }
    }

    return fullestTeam;
  }

  /** Return the least full participating team */
  public Team getEmptiestTeam() {
    Team emptiestTeam = null;
    float minFullness = Float.MAX_VALUE;

    for (Team team : this.getParticipatingTeams()) {
      float fullness = team.getFullness(false);
      if (fullness < minFullness) {
        emptiestTeam = team;
        minFullness = fullness;
      }
    }

    return emptiestTeam;
  }

  /**
   * Get the least full participating team currently in the match (based on the {@link
   * Team#getFullness} result) that the given player can join, excluding any team that player is
   * already on.
   *
   * <p>If priority is true, then it will be assumed that players without "join full team"
   * privileges can be kicked from a team to make room for the joining player, though this will be
   * avoided if possible.
   *
   * <p>Ties are resolved randomly to ensure that one team doesn't consistently get more players
   * than any other.
   */
  public TeamJoinResult getEmptiestJoinableTeam(MatchPlayer joining, boolean priorityKick) {
    TeamJoinResult bestResult = null;
    float minFullness = Float.MAX_VALUE;

    List<Team> shuffledTeams = new ArrayList<>(getParticipatingTeams());
    Collections.shuffle(shuffledTeams, match.getRandom());
    for (Team team : shuffledTeams) {
      if (team != joining.getParty()) {
        TeamJoinResult result = team.queryJoin(joining, priorityKick, false);
        if (result.isSuccess()) {
          float fullness = team.getFullness(false);
          if (bestResult == null
              || (!result.priorityKickRequired() && bestResult.priorityKickRequired())
              || (result.priorityKickRequired() == bestResult.priorityKickRequired()
                  && fullness < minFullness)) {

            bestResult = result;
            minFullness = fullness;
          }
        }
      }
    }

    return bestResult != null
        ? bestResult
        : new TeamJoinResult(TeamJoinResult.Status.FULL, null, priorityKick);
  }

  /**
   * Get the given player's last joined {@link Team} in this match, or null if the player has never
   * joined a team.
   */
  public @Nullable Team getLastTeam(UUID playerId) {
    return null; // No longer track last competitors
  }

  /** What would happen if the given player tried to join the given team right now? */
  @Override
  public @Nullable JoinResult queryJoin(MatchPlayer joining, @Nullable Competitor chosenParty) {
    if (chosenParty == null || chosenParty instanceof Team) {
      return queryJoin(joining, (Team) chosenParty, false);
    } else {
      return null;
    }
  }

  private JoinResult queryJoin(MatchPlayer joining, @Nullable Team chosenTeam, boolean queued) {
    final Team lastTeam = getLastTeam(joining.getId());

    if (chosenTeam == null) {
      // If autojoining, and the player is already on a team, the request is satisfied
      if (joining.getParty() instanceof Competitor) {
        return new TeamJoinResult(
            GenericJoinResult.Status.REDUNDANT,
            joining.getParty() instanceof Team ? (Team) joining.getParty() : null,
            false);
      }

      // If team choosing is disabled, and the match has not started yet, defer the join.
      // Note that this can only happen with autojoin. Choosing a team always fails if
      // the condition below is true.
      if (!queued && !Config.Teams.allowChoose() && !match.isRunning()) {
        return GenericJoinResult.Status.QUEUED.toResult();
      }

      if (lastTeam != null) {
        // If the player was previously on a team, try to join that team first
        GenericJoinResult result = lastTeam.queryJoin(joining, true, true);
        if (result.isSuccess()) return result;

        // If their previous team is full, and they are not allowed to switch, join fails
        if (!canSwitchTeams(joining)) {
          return new TeamJoinResult(GenericJoinResult.Status.FULL, lastTeam, false);
        }
      }

      // Try to find a team for the player to join
      return getEmptiestJoinableTeam(joining, true);

    } else {
      // If the player is already on the chosen team, there is nothing to do
      if (chosenTeam == joining.getParty()) {
        return new TeamJoinResult(GenericJoinResult.Status.REDUNDANT, chosenTeam, false);
      }

      // If team switching is disabled and the player is choosing to re-join their
      // last team, don't consider it a "choice" since that's the only team they can
      // join anyway. In any other case, check that they are allowed to choose their team.
      if (Config.Teams.allowSwitch() || chosenTeam != lastTeam) {
        // Team choosing is disabled
        if (!Config.Teams.allowChoose()) {
          return new TeamJoinResult(GenericJoinResult.Status.CHOICE_DISABLED);
        }

        // Player is not allowed to choose their team
        if (!canChooseTeam(joining)) {
          return new TeamJoinResult(GenericJoinResult.Status.CHOICE_DENIED);
        }
      }

      // If team switching is disabled, check if the player is rejoining their former team
      if (!canSwitchTeams(joining) && lastTeam != null) {
        if (chosenTeam == lastTeam) {
          return chosenTeam.queryJoin(joining, true, true);
        } else {
          return new TeamJoinResult(GenericJoinResult.Status.SWITCH_DISABLED);
        }
      }

      return chosenTeam.queryJoin(joining, true, false);
    }
  }

  @Override
  public boolean join(MatchPlayer joining, @Nullable Competitor chosenParty, JoinResult result) {
    if (result instanceof TeamJoinResult) {
      TeamJoinResult teamResult = (TeamJoinResult) result;
      Team lastTeam = getLastTeam(joining.getId());

      switch (teamResult.getStatus()) {
        case SWITCH_DISABLED:
          joining.sendWarning(
              new PersonalizedTranslatable(
                  "command.gameplay.join.switchDisabled", lastTeam.getComponentName()),
              false);
          return true;

        case CHOICE_DISABLED:
          joining.sendWarning(
              new PersonalizedTranslatable("command.gameplay.join.choiceDisabled"), false);
          return true;

        case CHOICE_DENIED:
          joining.sendWarning(
              new PersonalizedTranslatable("command.gameplay.join.choiceDenied"), false);
          return true;

        case FULL:
          if (teamResult.getTeam() != null) {
            joining.sendWarning(
                new PersonalizedTranslatable(
                    "command.gameplay.join.completelyFull",
                    teamResult.getTeam().getComponentName()),
                false);
          } else {
            joining.sendWarning(new PersonalizedTranslatable("autoJoin.teamsFull"), false);
          }

          return true;

        case REDUNDANT:
          joining.sendWarning(
              new PersonalizedTranslatable(
                  "command.gameplay.join.alreadyOnTeam", joining.getParty().getComponentName()),
              false);
          return true;
      }

      // FIXME: When a player rejoins their last team, we lose their autojoin status
      if (!forceJoin(joining, teamResult.getTeam(), lastTeam == null && chosenParty == null)) {
        return false;
      }

      if (teamResult.priorityKickRequired()) {
        match
            .getLogger()
            .info(
                "Bumping a player from "
                    + teamResult.getTeam().getColoredName()
                    + " to make room for "
                    + joining.getBukkit().getName());
        kickPlayerOffTeam(teamResult.getTeam(), false);
      }

      return true;
    } else {
      return false;
    }
  }

  @Override
  public void queuedJoin(QueuedParticipants queue) {
    final boolean even = requireEvenTeams();

    // First, eliminate any players who cannot join at all, so they do not influence the even teams
    // logic
    List<MatchPlayer> shortList = new ArrayList<>();
    for (MatchPlayer player : queue.getOrderedPlayers()) {
      JoinResult result = queryJoin(player, null, true);
      if (result.isSuccess()) {
        shortList.add(player);
      } else {
        // This will send a failure message
        join(player, null, result);
      }
    }

    for (int i = 0; i < shortList.size(); i++) {
      MatchPlayer player = shortList.get(i);
      if (even && areTeamsEven() && shortList.size() - i < getTeams().size()) {
        // Prevent join if even teams are required, and there aren't enough remaining players to go
        // around
        player.sendWarning(new PersonalizedTranslatable("command.gameplay.join.uneven"));
      } else {
        join(player, null, queryJoin(player, null, true));
      }
    }
  }

  /** Try to balance teams by bumping players to other teams */
  public void balanceTeams() {
    if (!Config.Teams.autoBalance()) return;

    match.getLogger().info("Auto-balancing teams");

    for (; ; ) {
      Team team = this.getFullestTeam();
      if (team == null) break;
      if (!team.isStacked()) break;
      match
          .getLogger()
          .info(
              "Bumping a player from stacked team "
                  + team.getColoredName()
                  + " size="
                  + team.getSize(false)
                  + " fullness="
                  + team.getFullness(false));
      if (!this.kickPlayerOffTeam(team, true)) break;
    }
  }

  public boolean kickPlayerOffTeam(Team kickFrom, boolean forBalance) {
    checkArgument(kickFrom.getMatch() == match);

    // Find all players who can be bumped
    List<MatchPlayer> kickable = new ArrayList<>();
    for (MatchPlayer player : kickFrom.getPlayers()) {
      if (!jmm.canPriorityKick(player) || (forBalance && isAutoJoin(player))) {
        // Premium players can be auto-balanced if they auto-joined
        kickable.add(player);
      }
    }

    if (kickable.isEmpty()) return false;

    // Choose an unfortunate cheapskate
    MatchPlayer kickMe = kickable.get(match.getRandom().nextInt(kickable.size()));

    // Try to put them on another team
    Party kickTo;
    GenericJoinResult kickResult = this.getEmptiestJoinableTeam(kickMe, false);
    if (kickResult.isSuccess()) {
      kickTo = kickResult.getCompetitor();
    } else {
      // If no teams are available, kick them to observers, if necessary
      if (forBalance) return false;
      kickTo = match.getDefaultParty();
    }

    // Give them the bad news
    if (jmm.canPriorityKick(kickMe)) {
      kickMe.sendMessage(
          new PersonalizedTranslatable("gameplay.kickedForBalance", kickTo.getComponentName()));
      kickMe.sendMessage(new PersonalizedTranslatable("gameplay.autoJoinSwitch"));
    } else {
      kickMe.playSound(new Sound("mob.villager.hit"));
      if (forBalance) {
        kickMe.sendWarning(
            new PersonalizedTranslatable("gameplay.kickedForBalance", kickTo.getComponentName()),
            false);
      } else {
        kickMe.sendWarning(
            new PersonalizedTranslatable("gameplay.kickedForPremium", kickFrom.getComponentName()),
            false);
      }
    }

    match
        .getLogger()
        .info("Bumping " + kickMe.getBukkit().getDisplayName() + " to " + kickTo.getColoredName());

    if (kickTo instanceof Team) {
      return forceJoin(kickMe, (Team) kickTo);
    } else {
      return match.setParty(kickMe, kickTo);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    if (event.getNewParty() instanceof Team) {
      event
          .getPlayer()
          .sendMessage(
              new PersonalizedTranslatable("team.join", event.getNewParty().getComponentName()));
    }
    updateReadiness();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onTeamResize(TeamResizeEvent event) {
    updateReadiness();
  }
}
