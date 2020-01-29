package tc.oc.pgm.teams;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.commons.lang.math.Fraction;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.Feature;
import tc.oc.pgm.join.GenericJoinResult;
import tc.oc.pgm.join.JoinMatchModule;
import tc.oc.pgm.match.SimpleParty;
import tc.oc.pgm.teams.events.TeamResizeEvent;
import tc.oc.server.BukkitUtils;
import tc.oc.util.components.ComponentUtils;

/**
 * Mutable class to represent a team created from a TeamInfo instance that is tied to a specific
 * match and will only live as long as the match lives. Teams support custom names and colors that
 * differ from the defaults specified by the map creator.
 */
public class Team extends SimpleParty implements Competitor, Feature<TeamFactory> {
  // The maximum allowed ratio between the "fullness" of any two teams in a match,
  // as measured by the Team.getFullness method. An imbalance of one player is
  // always allowed, even if it exceeds this ratio.
  public static final float MAX_IMBALANCE = 1.2f;

  protected final TeamFactory info;
  private TeamMatchModule tmm;
  private JoinMatchModule jmm;
  protected @Nullable String name = null;
  protected @Nullable Component componentName;
  protected Component chatPrefix;
  protected Integer minPlayers, maxPlayers, maxOverfill;

  // Recorded in the match document, Tourney plugin sets this
  protected @Nullable String leagueTeamId;

  /**
   * Construct a Team instance with the necessary information.
   *
   * @param info Defaults to use for name and color.
   * @param match Match this team is in.
   */
  public Team(TeamFactory info, Match match) {
    super(match);
    this.info = info;
  }

  protected JoinMatchModule join() {
    if (jmm == null) {
      jmm = getMatch().needModule(JoinMatchModule.class);
    }
    return jmm;
  }

  protected TeamMatchModule module() {
    if (tmm == null) {
      tmm = getMatch().needModule(TeamMatchModule.class);
    }
    return tmm;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{match=" + getMatch() + ", name=" + getName() + "}";
  }

  @Override
  public String getId() {
    return this.info.getId();
  }

  /**
   * Gets map specified information about this team.
   *
   * @return Map-specific information about the team.
   */
  public TeamFactory getInfo() {
    return this.info;
  }

  public @Nullable String getLeagueTeamId() {
    return leagueTeamId;
  }

  public void setLeagueTeamId(@Nullable String leagueTeamId) {
    this.leagueTeamId = leagueTeamId;
  }

  public boolean isInstance(TeamFactory definition) {
    return info.equals(definition);
  }

  @Override
  public TeamFactory getDefinition() {
    return this.info;
  }

  @Override
  public boolean isParticipating() {
    return match.isRunning();
  }

  @Override
  public boolean isObserving() {
    return !match.isRunning();
  }

  @Override
  public String getDefaultName() {
    return info.getDefaultName();
  }

  /**
   * Gets the name of this team that can be modified using setTeam. If no custom name is set then
   * this will return the default team name as specified in the team info.
   *
   * @return Name of the team without colors.
   */
  @Override
  public String getName() {
    return name != null ? name : getDefaultName();
  }

  public String getShortName() {
    String lower = getName().toLowerCase();
    if (lower.endsWith(" team")) {
      return getName().substring(0, lower.length() - " team".length());
    } else if (lower.startsWith("team ")) {
      return getName().substring("team ".length());
    } else {
      return getName();
    }
  }

  @Override
  public String getName(@Nullable CommandSender viewer) {
    return getName();
  }

  @Override
  public boolean isNamePlural() {
    // Assume custom names are singular
    return this.name == null && this.info.isDefaultNamePlural();
  }

  /**
   * Gets the combination of the team color with the team name.
   *
   * @return Colored version of the team name.
   */
  @Override
  public String getColoredName() {
    return getColor() + getName();
  }

  @Override
  public String getColoredName(@Nullable CommandSender viewer) {
    return getColor() + getName(viewer);
  }

  /**
   * Sets a custom name for this team that should be unique in the match. Note that setting the name
   * to null will reset it to the default name as specified in the team info.
   *
   * @param newName New name for this team. Should not include colors.
   */
  public void setName(@Nullable String newName) {
    if (Objects.equals(this.name, newName) || this.getName().equals(newName)) return;
    String oldName = this.getName();
    this.name = newName;
    this.componentName = null;
    this.match.callEvent(new PartyRenameEvent(this, oldName, this.getName()));
  }

  @Override
  public ChatColor getColor() {
    return this.info.getDefaultColor();
  }

  @Override
  public Color getFullColor() {
    return BukkitUtils.colorOf(this.getColor());
  }

  @Override
  public Component getComponentName() {
    if (componentName == null) {
      this.componentName = new PersonalizedText(getName(), ComponentUtils.convert(getColor()));
    }
    return componentName;
  }

  @Override
  public Component getStyledName(NameStyle style) {
    return getComponentName();
  }

  @Override
  public Component getChatPrefix() {
    if (chatPrefix == null) {
      this.chatPrefix =
          new PersonalizedText("(" + getShortName() + ") ", ComponentUtils.convert(getColor()));
    }
    return chatPrefix;
  }

  @Override
  public NameTagVisibility getNameTagVisibility() {
    return info.getNameTagVisibility();
  }

  public int getMinPlayers() {
    return this.minPlayers != null ? minPlayers : this.info.getMinPlayers();
  }

  public int getMaxPlayers() {
    return this.maxPlayers != null ? maxPlayers : this.info.getMaxPlayers();
  }

  public int getMaxOverfill() {
    return this.maxOverfill != null ? maxOverfill : this.info.getMaxOverfill();
  }

  public void setMinSize(@Nullable Integer minPlayers) {
    this.minPlayers = minPlayers;
    getMatch().callEvent(new TeamResizeEvent(this));
  }

  public void resetMinSize() {
    setMinSize(null);
  }

  public void setMaxSize(@Nullable Integer maxPlayers, @Nullable Integer maxOverfill) {
    this.maxPlayers = maxPlayers;
    this.maxOverfill = maxOverfill;
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
    // Can always join obs
    if (this.isObserving()) return 1;

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
