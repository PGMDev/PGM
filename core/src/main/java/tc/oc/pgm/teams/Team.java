package tc.oc.pgm.teams;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import javax.annotation.Nullable;
import org.apache.commons.lang.math.Fraction;
import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.GenericJoinResult;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.match.PartyImpl;
import tc.oc.pgm.teams.events.TeamResizeEvent;

/** A team of players. */
public class Team extends PartyImpl implements Competitor, Feature<TeamFactory> {
  // The maximum allowed ratio between the "fullness" of any two teams in a match,
  // as measured by the Team.getFullness method. An imbalance of one player is
  // always allowed, even if it exceeds this ratio.
  public static final float MAX_IMBALANCE = 1.2f;

  private final TeamFactory info;
  private int min, max, overfill;

  private TeamMatchModule tmm;
  private JoinMatchModule jmm;

  public Team(final TeamFactory info, final Match match) {
    super(match, requireNonNull(info).getDefaultName(), info.getDefaultColor());
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
    return info.getNameTagVisibility();
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
    this.min = minPlayers;
    getMatch().callEvent(new TeamResizeEvent(this));
  }

  public void resetMinSize() {
    setMinSize(null);
  }

  public void setMaxSize(@Nullable Integer maxPlayers, @Nullable Integer maxOverfill) {
    this.max = maxPlayers;
    this.overfill = maxOverfill;
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

  public int getMaxSize(MatchPlayer joining) {
    return join().canJoinFull(joining) ? this.getMaxOverfill() : this.getMaxPlayers();
  }

  /**
   * Return the number of players on this team. If priority is true, exclude players who can be
   * bumped off the team.
   */
  public int getSize(boolean priority) {
    return getSizeAfterJoin(null, null, priority);
  }

  /**
   * Return the number of players that will be on this team after the given player joins the given
   * team. If the player is null, just return the current team size.
   *
   * @param joining Player who is joining a team
   * @param newTeam Team the player is joining, which may or may not be this team
   * @param priority Exclude from the result players who can be priority kicked off this team
   * @return Number of players on the team after the join
   */
  public int getSizeAfterJoin(
      @Nullable MatchPlayer joining, @Nullable Team newTeam, boolean priority) {
    Collection<MatchPlayer> members = this.getPlayers();
    int size = members.size();

    if (joining != null) {
      boolean member = members.contains(joining);
      if (!member && this == newTeam) size++;
      if (member && this != newTeam) size--;
    }

    if (priority)
      for (MatchPlayer member : members) {
        if (join().canPriorityKick(member)) size--;
      }

    return size;
  }

  public boolean isMinSize() {
    return getPlayers().size() >= getMinPlayers();
  }

  /** Return a normalized "fullness" ratio for this team. */
  public float getFullness(boolean priority) {
    return (float) this.getSize(priority) / this.getMaxOverfill();
  }

  public Fraction getFullnessAfterJoin(@Nullable MatchPlayer joining, @Nullable Team newTeam) {
    return Fraction.getReducedFraction(getSizeAfterJoin(joining, newTeam, false), getMaxOverfill());
  }

  /**
   * Get the maximum number of players currently allowed on this team without exceeding any limits.
   */
  public int getMaxBalancedSize() {
    // Find the minimum fullness among other teams
    float minFullness = 1f;
    for (Team team : module().getParticipatingTeams()) {
      if (team != this) {
        minFullness = Math.min(minFullness, team.getFullness(false));
      }
    }

    // Calculate the dynamic limit to maintain balance with other teams (this can be zero)
    int slots = (int) Math.ceil(Math.min(1f, minFullness * MAX_IMBALANCE) * this.getMaxOverfill());

    // Clamp to the static limit defined for this team (cannot be zero unless the static limit is
    // zero)
    return Math.min(this.getMaxOverfill(), Math.max(1, slots));
  }

  public boolean isStacked() {
    return this.getSize(false) > this.getMaxBalancedSize();
  }

  /**
   * Return the number of available slots for the given player. If priority is true, and the joining
   * player has priority kick privileges, assume that non-privileged players can be kicked off the
   * team to make room.
   */
  public int getOpenSlots(MatchPlayer joining, boolean priorityKick) {
    // Count existing team members with and without join privileges
    int normal = 0, privileged = 0;

    for (MatchPlayer player : this.getPlayers()) {
      if (player != joining) {
        if (join().canPriorityKick(player)) privileged++;
        else normal++;
      }
    }

    // Get the maximum slots and deduct priority players
    int slots = this.getMaxSize(joining) - privileged;

    // If normal players cannot be bumped, deduct them as well
    if (!(priorityKick && join().canPriorityKick(joining))) slots -= normal;

    return Math.max(0, slots);
  }

  /**
   * @return if there is a free slot available for the given player to join this team. If the player
   *     is already on this team, the test behaves as if they are not.
   */
  public boolean hasOpenSlots(MatchPlayer joining, boolean priorityKick) {
    return this.getOpenSlots(joining, priorityKick) > 0;
  }

  public TeamMatchModule.TeamJoinResult queryJoin(
      MatchPlayer joining, boolean priorityKick, boolean rejoin) {
    GenericJoinResult.Status joinStatus =
        rejoin ? GenericJoinResult.Status.REJOINED : GenericJoinResult.Status.JOINED;
    if (hasOpenSlots(joining, false)) {
      return new TeamMatchModule.TeamJoinResult(joinStatus, this, false);
    }

    if (priorityKick && hasOpenSlots(joining, true)) {
      return new TeamMatchModule.TeamJoinResult(joinStatus, this, true);
    }

    return new TeamMatchModule.TeamJoinResult(GenericJoinResult.Status.FULL, this, false);
  }
}
