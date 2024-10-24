package tc.oc.pgm.teams;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.scoreboard.NameTagVisibility;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.join.JoinResultOption;
import tc.oc.pgm.match.PartyImpl;
import tc.oc.pgm.teams.events.TeamResizeEvent;

/** A team of players. */
public class Team extends PartyImpl implements Competitor, Feature<TeamFactory> {
  // The maximum allowed ratio between the "fullness" of any two teams in a match,
  // as measured by the Team.getFullness method. An imbalance of one player is
  // always allowed, even if it exceeds this ratio.
  public static final float MAX_IMBALANCE = 1.25f;
  // Same as above, but for "standard" 2-team same max-players matches
  public static final float MAX_STANDARD_IMBALANCE = 1.1f;

  private final TeamFactory info;
  private int min, max, overfill;
  private @Nullable NameTagVisibility nameTagVisibilityOverride;

  private TeamMatchModule tmm;
  private JoinMatchModule jmm;

  public Team(final TeamFactory info, final Match match) {
    super(match, assertNotNull(info).getDefaultName(), info.getDefaultColor(), info.getDyeColor());
    this.info = info;
    this.min = info.getMinPlayers();
    this.max = info.getMaxPlayers();
    this.overfill = info.getMaxOverfill();
  }

  private JoinMatchModule join() {
    if (jmm == null) {
      jmm = getMatch().needModule(JoinMatchModule.class);
    }
    return jmm;
  }

  private TeamMatchModule module() {
    if (tmm == null) {
      tmm = getMatch().needModule(TeamMatchModule.class);
    }
    return tmm;
  }

  @Override
  public String getId() {
    return this.info.getId();
  }

  public TeamFactory getInfo() {
    return this.info;
  }

  public boolean isInstance(final TeamFactory definition) {
    return info.equals(definition);
  }

  @Override
  public TeamFactory getDefinition() {
    return this.info;
  }

  @Override
  public boolean isParticipating() {
    return this.getMatch().isRunning();
  }

  @Override
  public boolean isObserving() {
    return !this.getMatch().isRunning();
  }

  @Override
  public String getDefaultName() {
    return info.getDefaultName();
  }

  public String getShortName() {
    String lower = getNameLegacy().toLowerCase();
    if (lower.endsWith(" team")) {
      return getNameLegacy().substring(0, lower.length() - " team".length());
    } else if (lower.startsWith("team ")) {
      return getNameLegacy().substring("team ".length());
    } else {
      return getNameLegacy();
    }
  }

  @Override
  public NameTagVisibility getNameTagVisibility() {
    return nameTagVisibilityOverride != null
        ? nameTagVisibilityOverride
        : info.getNameTagVisibility();
  }

  @Override
  public void setNameTagVisibility(NameTagVisibility visibility) {
    this.nameTagVisibilityOverride = visibility;
  }

  public int getMinPlayers() {
    return this.min;
  }

  public int getMaxPlayers() {
    return this.max;
  }

  public int getMaxOverfill() {
    return this.overfill;
  }

  public void setMinSize(@Nullable Integer minPlayers) {
    this.min = minPlayers == null ? info.getMinPlayers() : minPlayers;
    getMatch().callEvent(new TeamResizeEvent(this));
  }

  public void resetMinSize() {
    setMinSize(null);
  }

  public void setMaxSize(@Nullable Integer maxPlayers, @Nullable Integer maxOverfill) {
    this.max = maxPlayers == null ? info.getMaxPlayers() : maxPlayers;
    this.overfill = maxOverfill == null ? info.getMaxOverfill() : maxOverfill;
    getMatch().callEvent(new TeamResizeEvent(this));
    module().updateMaxPlayers();
  }

  public void resetMaxSize() {
    setMaxSize(null, null);
  }

  @Override
  public boolean isAutomatic() {
    return false;
  }

  public int getMaxSize(JoinRequest request) {
    return join().canJoinFull(request) ? this.getMaxOverfill() : this.getMaxPlayers();
  }

  public boolean isMinSize() {
    return getPlayers().size() >= getMinPlayers();
  }

  public int getSize() {
    return this.getPlayers().size();
  }

  @Deprecated // Kept to avoid other plugins breaking
  public int getSize(boolean priority) {
    return this.getPlayers().size();
  }

  @Deprecated // Kept to avoid other plugins breaking
  public float getFullness(boolean priority) {
    return getFullness();
  }

  @Deprecated // Kept to avoid other plugins breaking
  public int getSizeAfterJoin(MatchPlayer joining, Team newTeam, boolean priority) {
    return this.getSize() + (newTeam == this ? 1 : 0);
  }

  /** Return a normalized "fullness" ratio for this team. */
  public float getFullness() {
    return (float) this.getSize() / this.getMaxOverfill();
  }

  /** Return a normalized "fullness" ratio for this team. */
  public float getFullnessAfterJoin(int players) {
    return (float) (this.getSize() + players) / this.getMaxOverfill();
  }

  /**
   * Get the maximum number of players currently allowed on this team without exceeding any limits.
   */
  public int getMaxBalancedSize() {
    // Find the minimum fullness among other teams
    float minFullness = 1f;
    boolean isStandard = true;
    for (Team team : module().getParticipatingTeams()) {
      if (team != this) {
        minFullness = Math.min(minFullness, team.getFullness());
        isStandard &= team.getMaxOverfill() == this.getMaxOverfill();
      }
    }

    // Calculate the dynamic limit to maintain balance with other teams (this can be zero)
    float maxImbalance = isStandard ? MAX_STANDARD_IMBALANCE : MAX_IMBALANCE;
    int slots = (int) Math.ceil(Math.min(1f, minFullness * maxImbalance) * this.getMaxOverfill());

    // Clamp to the static limit defined for this team (cannot be zero unless the static limit is
    // zero)
    return Math.min(this.getMaxOverfill(), Math.max(1, slots));
  }

  public boolean isStacked() {
    return this.getPlayers().size() > this.getMaxBalancedSize();
  }

  /**
   * Return the number of available slots for the given player. If priority is true, and the joining
   * player has priority kick privileges, assume that non-privileged players can be kicked off the
   * team to make room.
   */
  public int getOpenSlots(JoinRequest request, boolean priorityKick) {
    int slots = this.getMaxSize(request);
    if (!(priorityKick && join().canPriorityKick(request))) {
      // Subtract all player who have already joined
      slots -= this.getPlayers().size();
    } else {
      // Subtract all players who cannot be kicked
      JoinMatchModule jmm = join();
      slots -=
          this.getPlayers().stream().filter(pl -> !jmm.canBePriorityKicked(pl)).count();
    }
    return Math.max(0, slots);
  }

  /**
   * @return if there is a free slot available for the given player to join this team. If the player
   *     is already on this team, the test behaves as if they are not.
   */
  public boolean hasOpenSlots(JoinRequest request, boolean priorityKick) {
    return this.getOpenSlots(request, priorityKick) >= request.getPlayerCount();
  }

  public TeamMatchModule.TeamJoinResult queryJoin(JoinRequest request, boolean rejoin) {
    JoinResultOption joinStatus = rejoin ? JoinResultOption.REJOINED : JoinResultOption.JOINED;
    if (hasOpenSlots(request, false)) {
      return new TeamMatchModule.TeamJoinResult(joinStatus, this, false);
    }

    if (join().canPriorityKick(request) && hasOpenSlots(request, true)) {
      return new TeamMatchModule.TeamJoinResult(joinStatus, this, true);
    }

    return new TeamMatchModule.TeamJoinResult(JoinResultOption.FULL, this, false);
  }
}
