package tc.oc.pgm.teams;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.join.JoinHandler;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.JoinResult;
import tc.oc.pgm.join.JoinResultOption;
import tc.oc.pgm.match.ObserverParty;
import tc.oc.pgm.match.PartyImpl;
import tc.oc.pgm.match.QueuedParty;
import tc.oc.pgm.start.StartMatchModule;
import tc.oc.pgm.start.UnreadyReason;
import tc.oc.pgm.teams.events.TeamResizeEvent;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.Sounds;

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
  }
  ;

  public static class TeamJoinResult implements JoinResult {
    private final JoinResultOption status;
    private final Team team;
    private final boolean priorityKick;

    public TeamJoinResult(JoinResultOption status, @Nullable Team team, boolean priorityKick) {
      this.status = status;
      this.team = team;
      this.priorityKick = priorityKick;
    }

    public TeamJoinResult(JoinResultOption status) {
      this(status, null, false);
    }

    @Override
    public boolean isSuccess() {
      return status.isSuccess();
    }

    @Override
    public JoinResultOption getOption() {
      return status;
    }

    public Team getTeam() {
      return team;
    }

    public boolean priorityKickRequired() {
      return priorityKick;
    }
  }

  // All teams in the match
  private final Set<Team> teams;

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

    match.needModule(JoinMatchModule.class).setJoinHandler(this);

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

    final int playersQueued = match
        .needModule(JoinMatchModule.class)
        .getQueuedParticipants()
        .getPlayers()
        .size();
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
    return StringUtils.bestFuzzyMatch(name, getTeams(), PartyImpl::getNameLegacy);
  }

  public @Nullable Team getTeam(String name) {
    for (Team team : getTeams()) {
      if (team.getNameLegacy().equalsIgnoreCase(name)) return team;
    }
    return null;
  }

  public boolean canSwitchTeams(JoinRequest request) {
    return canChooseTeam(request);
  }

  public boolean canChooseTeam(JoinRequest request) {
    return request.isForcedOr(JoinRequest.Flag.JOIN_CHOOSE);
  }

  public boolean internalJoin(MatchPlayer player, Team newTeam, JoinRequest request) {
    assertNotNull(newTeam);
    return player.getParty() == newTeam
        || (!Integration.isVanished(player.getBukkit())
            && match.setParty(player, newTeam, request));
  }

  /** Return the most full participating team */
  public Team getFullestTeam() {
    Team fullestTeam = null;
    float maxFullness = Float.MIN_VALUE;

    for (Team team : this.getParticipatingTeams()) {
      float fullness = team.getFullness();
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
      float fullness = team.getFullness();
      if (emptiestTeam == null || fullness < minFullness) {
        emptiestTeam = team;
        minFullness = fullness;
      }
    }

    return emptiestTeam;
  }

  /**
   * Get the least full participating team currently in the match (based on the
   * {@link Team#getFullnessAfterJoin} result) that the given player can join, excluding any team
   * that player is already on.
   *
   * <p>If priority is true, then it will be assumed that players without "join full team"
   * privileges can be kicked from a team to make room for the joining player, though this will be
   * avoided if possible.
   *
   * <p>Ties are resolved randomly to ensure that one team doesn't consistently get more players
   * than any other.
   */
  public TeamJoinResult getEmptiestJoinableTeam(Team ignoreTeam, JoinRequest request) {
    TeamJoinResult bestResult = null;
    float minFullness = Float.MAX_VALUE;

    List<Team> shuffledTeams = new ArrayList<>(getParticipatingTeams());
    Collections.shuffle(shuffledTeams, match.getRandom());
    for (Team team : shuffledTeams) {
      if (team != ignoreTeam) {
        TeamJoinResult result = team.queryJoin(request, false);
        if (result.isSuccess()) {
          float fullness = team.getFullnessAfterJoin(request.getPlayerCount());
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

    return bestResult != null ? bestResult : new TeamJoinResult(JoinResultOption.FULL, null, false);
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
  public JoinResult queryJoin(MatchPlayer joining, JoinRequest request) {
    if (request.has(JoinRequest.Flag.FORCE)) {
      return new TeamJoinResult(
          JoinResultOption.JOINED,
          request.getTeam() == null ? getEmptiestTeam() : request.getTeam(),
          false);
    }

    final Team lastTeam = getLastTeam(joining.getId());

    if (request.getTeam() == null) {
      // If autojoining, and the player is already on a team, the request is satisfied
      if (joining.getParty() instanceof Competitor) {
        return new TeamJoinResult(
            JoinResultOption.REDUNDANT,
            joining.getParty() instanceof Team ? (Team) joining.getParty() : null,
            false);
      }

      // If team choosing is disabled, and the match has not started yet, defer the join.
      // Note that this can only happen with autojoin. Choosing a team always fails if
      // the condition below is true.
      if (PGM.get().getConfiguration().shouldQueueJoin()
          && !request.has(JoinRequest.Flag.IGNORE_QUEUE)
          && !canChooseTeam(request)
          && !match.isRunning()) {
        return JoinResultOption.QUEUED;
      }

      if (lastTeam != null) {
        // If the player was previously on a team, try to join that team first
        TeamJoinResult result = lastTeam.queryJoin(request, true);
        if (result.isSuccess()) return result;

        // If their previous team is full, and they are not allowed to switch, join fails
        if (!canSwitchTeams(request)) {
          return new TeamJoinResult(JoinResultOption.FULL, lastTeam, false);
        }
      }

      // Queue the player if there is an attempt to achieve balance
      if (PGM.get().getConfiguration().shouldQueueJoin()
          && !request.has(JoinRequest.Flag.IGNORE_QUEUE)
          && !match.isRunning()) {
        return JoinResultOption.QUEUED;
      }

      // Try to find a team for the player to join
      return getEmptiestJoinableTeam(null, request);
    } else {
      // If the player is already on the chosen team, there is nothing to do
      if (request.getTeam() == joining.getParty()) {
        return new TeamJoinResult(JoinResultOption.REDUNDANT, request.getTeam(), false);
      }

      // If team switching is disabled and the player is choosing to re-join their
      // last team, don't consider it a "choice" since that's the only team they can
      // join anyway. In any other case, check that they are allowed to choose their team.
      if (canSwitchTeams(request) || request.getTeam() != lastTeam) {
        // Player is not allowed to choose their team
        if (!canChooseTeam(request)) {
          return new TeamJoinResult(JoinResultOption.CHOICE_DENIED);
        }
      }

      // If team switching is disabled, check if the player is rejoining their former team
      if (!canSwitchTeams(request) && lastTeam != null) {
        if (request.getTeam() == lastTeam) {
          return request.getTeam().queryJoin(request, true);
        } else {
          return new TeamJoinResult(JoinResultOption.SWITCH_DISABLED);
        }
      }

      return request.getTeam().queryJoin(request, false);
    }
  }

  @Override
  public boolean join(MatchPlayer joining, JoinRequest request, JoinResult result) {
    if (result instanceof TeamJoinResult) {
      TeamJoinResult teamResult = (TeamJoinResult) result;
      Team lastTeam = getLastTeam(joining.getId());

      switch (teamResult.getOption()) {
        case SWITCH_DISABLED:
          joining.sendWarning(translatable("join.err.noSwitch", lastTeam.getName()));
          return true;

        case CHOICE_DISABLED:
        case CHOICE_DENIED:
          joining.sendWarning(translatable("join.err.noChoice"));
          return true;

        case FULL:
          if (teamResult.getTeam() != null) {
            joining.sendWarning(
                translatable("join.err.full.team", teamResult.getTeam().getName()));
          } else {
            joining.sendWarning(translatable("join.err.full"));
          }

          return true;

        case REDUNDANT:
          joining.sendWarning(
              translatable("join.err.alreadyJoined.team", joining.getParty().getName()));
          return true;
      }

      if (!internalJoin(joining, teamResult.getTeam(), request)) {
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
      JoinRequest request = JoinRequest.fromPlayer(player, null, JoinRequest.Flag.IGNORE_QUEUE);

      JoinResult result = queryJoin(player, request);
      if (result.isSuccess()) {
        shortList.add(player);
      } else {
        // This will send a failure message
        join(player, request, result);
      }
    }

    for (MatchPlayer player : shortList) {
      join(player, JoinRequest.fromPlayer(player, null, JoinRequest.Flag.IGNORE_QUEUE));
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
    assertTrue(kickFrom.getMatch() == match);

    // Find all players who can be bumped
    List<MatchPlayer> kickable = new ArrayList<>();
    for (MatchPlayer player : kickFrom.getPlayers()) {
      if (jmm.canBePriorityKicked(player) || (forBalance && jmm.isAutoJoin(player))) {
        // Premium players can be auto-balanced if they auto-joined
        kickable.add(player);
      }
    }

    if (kickable.isEmpty()) return false;

    // Choose an unfortunate cheapskate
    MatchPlayer kickMe = kickable.get(match.getRandom().nextInt(kickable.size()));

    // Try to put them on another team
    Party kickTo;
    TeamJoinResult kickResult =
        this.getEmptiestJoinableTeam(kickFrom, JoinRequest.fromPlayer(kickMe, null));
    if (kickResult.isSuccess()) {
      kickTo = kickResult.getTeam();
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
      kickMe.playSound(Sounds.MATCH_KICK);
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
      boolean title = event.getRequest().has(JoinRequest.Flag.SHOW_TITLE);

      if (title && !player.isLegacy()) {
        player.showTitle(title(
            space(),
            joinMsg,
            Times.times(Ticks.duration(5), Ticks.duration(20), Ticks.duration(5))));
      } else {
        player.sendMessage(joinMsg);
      }
    }

    updateReadiness();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void addPlayerToMatch(PlayerJoinPartyEvent event) {
    if (event.getNewParty() instanceof Team) {
      playerTeamMap.put(event.getPlayer().getId(), (Team) event.getNewParty());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onTeamResize(TeamResizeEvent event) {
    updateReadiness();
  }
}
