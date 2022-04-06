package tc.oc.pgm.teams;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.title.Title.title;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import org.apache.commons.lang.math.Fraction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.StringUtils;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.GenericJoinResult;
import tc.oc.pgm.join.JoinHandler;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.match.ObserverParty;
import tc.oc.pgm.match.QueuedParty;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;
import tc.oc.pgm.teams.events.TeamResizeEvent;

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
          return translatable(
              "join.wait.singular.team", text(players, NamedTextColor.AQUA), team.getName());
        } else {
          return translatable(
              "join.wait.plural.team", text(players, NamedTextColor.AQUA), team.getName());
        }
      } else {
        if (players == 1) {
          return translatable("join.wait.singular", text(players, NamedTextColor.AQUA));
        } else {
          return translatable("join.wait.plural", text(players, NamedTextColor.AQUA));
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

  // Players who autojoined their current team
  private final Set<MatchPlayer> autoJoins = new HashSet<>();

  // Players who were forced into the match
  private final Set<MatchPlayer> forced = new HashSet<>();

  // Players who are being switched between teams see TeamSwitchKit
  private final Map<MatchPlayer, Boolean> teamSwitchKit = new HashMap<>();

  // Minimum at any time of the number of additional players needed to start the match
  private int minPlayersNeeded = Integer.MAX_VALUE;

  private final JoinMatchModule jmm;
  private final Match match;

  private final Map<UUID, Team> playerTeamMap = new HashMap<>();

  public TeamMatchModule(Match match, Set<TeamFactory> teamFactories) {
    this.match = match;
    this.teams = new HashSet<>(teamFactories.size());

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

    int globalNeeded =
        (int) PGM.get().getConfiguration().getMinimumPlayers() - playersJoined - playersQueued;

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
    for (Team team : getTeams()) byName.put(team.getNameLegacy(), team);
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

  public void setForced(MatchPlayer player, boolean force) {
    if (force) {
      forced.add(player);
    } else {
      forced.remove(player);
    }
  }

  public boolean isForced(MatchPlayer player) {
    return forced.contains(player);
  }

  public void setTeamSwitchKit(MatchPlayer player, boolean showTitle, boolean add) {
    setForced(player, add);
    if (add) {
      teamSwitchKit.put(player, showTitle);
    } else {
      teamSwitchKit.remove(player);
    }
  }

  public boolean hasTeamSwitchKit(MatchPlayer player) {
    return teamSwitchKit.containsKey(player);
  }

  public boolean showTeamSwitchTitle(MatchPlayer player) {
    return teamSwitchKit.getOrDefault(player, false);
  }

  public boolean canSwitchTeams(MatchPlayer joining) {
    return canChooseTeam(joining);
  }

  public boolean canChooseTeam(MatchPlayer joining) {
    return joining.getBukkit().hasPermission(Permissions.JOIN_CHOOSE);
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

    if (!player.isVanished() && match.setParty(player, newTeam)) {
      setAutoJoin(player, autoJoin);
      return true;
    } else {
      return false;
    }
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
      if (fullestTeam == null || fullness > maxFullness) {
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
      if (emptiestTeam == null || fullness < minFullness) {
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
    return playerTeamMap.get(playerId);
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
      if (!queued
          && !canChooseTeam(joining)
          && !match.isRunning()
          && PGM.get().getConfiguration().shouldQueueJoin()) {
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

      // Queue the player if there is an attempt to achieve balance
      if (!queued && !match.isRunning() && PGM.get().getConfiguration().shouldQueueJoin()) {
        return GenericJoinResult.Status.QUEUED.toResult();
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
      if (canSwitchTeams(joining) || chosenTeam != lastTeam) {
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
          joining.sendWarning(translatable("join.err.noSwitch", lastTeam.getName()));
          return true;

        case CHOICE_DISABLED:
        case CHOICE_DENIED:
          joining.sendWarning(translatable("join.err.noChoice"));
          return true;

        case FULL:
          if (teamResult.getTeam() != null) {
            joining.sendWarning(translatable("join.err.full.team", teamResult.getTeam().getName()));
          } else {
            joining.sendWarning(translatable("join.err.full"));
          }

          return true;

        case REDUNDANT:
          joining.sendWarning(
              translatable("join.err.alreadyJoined.team", joining.getParty().getName()));
          return true;
      }

      // FIXME: When a player rejoins their last team, we lose their autojoin status
      if (!forceJoin(joining, teamResult.getTeam(), lastTeam == null && chosenParty == null)) {
        return false;
      }

      if (teamResult.priorityKickRequired()) {
        kickPlayerOffTeam(teamResult.getTeam(), false);
      }

      return true;
    } else {
      return false;
    }
  }

  @Override
  public void queuedJoin(QueuedParty queue) {
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
      join(player, null, queryJoin(player, null, true));
    }
  }

  /** Try to balance teams by bumping players to other teams */
  public void balanceTeams() {
    if (!PGM.get().getConfiguration().shouldBalanceJoin()) return;

    match.getLogger().info("Auto-balancing teams");

    for (; ; ) {
      Team team = this.getFullestTeam();
      if (team == null) break;
      if (!team.isStacked()) break;
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
      kickMe.sendMessage(translatable("join.ok.moved", kickTo.getName()));
      kickMe.sendMessage(translatable("join.ok.moved.explanation"));
    } else {
      kickMe.playSound(sound(key("mob.villager.hit"), Sound.Source.MASTER, 1, 1));
      if (forBalance) {
        kickMe.sendWarning(translatable("join.ok.moved", kickTo.getName()));
      } else {
        kickMe.sendWarning(translatable("leave.ok.priorityKick.team", kickFrom.getName()));
      }
    }

    match
        .getLogger()
        .info("Bumping " + kickMe.getBukkit().getDisplayName() + " to " + kickTo.getDefaultName());

    if (kickTo instanceof Team) {
      return forceJoin(kickMe, (Team) kickTo);
    } else {
      return match.setParty(kickMe, kickTo);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    MatchPlayer player = event.getPlayer();

    if (event.getNewParty() instanceof Team
        || (event.getNewParty() instanceof ObserverParty && event.getOldParty() != null)) {
      Component joinMsg = translatable("join.ok.team", event.getNewParty().getName());
      boolean title = false;

      // Players with team switch kit, check for title value and remove from map
      if (hasTeamSwitchKit(player)) {
        title = showTeamSwitchTitle(player);
        setTeamSwitchKit(player, false, false);
      }

      if (title && !player.isLegacy()) {
        player.showTitle(
            title(
                space(),
                joinMsg,
                Times.of(Ticks.duration(5), Ticks.duration(20), Ticks.duration(5))));
      } else {
        player.sendMessage(joinMsg);
      }
    }

    if (event.getNewParty() instanceof ObserverParty) {
      setForced(event.getPlayer(), false);
    }
    updateReadiness();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void addPlayerToMatch(PlayerJoinPartyEvent event) {
    if (!(event.getNewParty() instanceof Team)) {
      return;
    }
    UUID playerID = event.getPlayer().getId();
    Team newTeam = (Team) event.getNewParty();
    if (playerTeamMap.containsKey(playerID)
        && !event
            .getNewParty()
            .isObserving()) { // If player was previously on team but joins obs, keep previous team
      playerTeamMap.replace(playerID, newTeam);

    } else if (!playerTeamMap.containsKey(playerID)) {
      playerTeamMap.put(playerID, newTeam);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onTeamResize(TeamResizeEvent event) {
    updateReadiness();
  }
}
