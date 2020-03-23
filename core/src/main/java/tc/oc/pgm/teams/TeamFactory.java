package tc.oc.pgm.teams;

import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

/** Immutable class to represent a team in a map that is not tied to any specific match. */
@FeatureInfo(name = "team")
public class TeamFactory extends SelfIdentifyingFeatureDefinition {
  protected final String defaultName;
  protected final boolean defaultNamePlural;
  protected final ChatColor defaultColor;
  @Nullable protected final ChatColor overheadColor;
  protected final int minPlayers;
  protected final int maxPlayers;
  protected final int maxOverfill;
  protected final NameTagVisibility nameTagVisibility;

  /**
   * Create a TeamInfo instance with the specified information.
   *
   * @param id unique id
   * @param defaultName Default name for the team.
   * @param defaultNamePlural Is the default name a plural word?
   * @param defaultColor Default color for the team.
   * @param overheadColor Color to be displayed above the team members' avatars.
   * @param maxPlayers Maximum amount of players that may be on this team.
   * @param nameTagVisibility Who can see the name tags of players on this team
   */
  public TeamFactory(
      @Nullable String id,
      String defaultName,
      boolean defaultNamePlural,
      ChatColor defaultColor,
      @Nullable ChatColor overheadColor,
      int minPlayers,
      int maxPlayers,
      int maxOverfill,
      NameTagVisibility nameTagVisibility) {
    super(id);
    this.defaultName = defaultName;
    this.defaultNamePlural = defaultNamePlural;
    this.defaultColor = defaultColor;
    this.overheadColor = overheadColor;
    this.minPlayers = minPlayers;
    this.maxPlayers = maxPlayers;
    this.maxOverfill = maxOverfill;
    this.nameTagVisibility = nameTagVisibility;
  }

  @Override
  protected String getDefaultId() {
    return super.makeDefaultId() + "--" + makeId(this.defaultName);
  }

  public Team createTeam(Match match) {
    Team newTeam = new Team(this, match);
    match.getFeatureContext().add(newTeam);
    return newTeam;
  }

  @Override
  public String toString() {
    return this.getDefaultName();
  }

  /**
   * Gets this team's default name as set by the map creator.
   *
   * @return Default team name.
   */
  public String getDefaultName() {
    return this.defaultName;
  }

  public boolean isDefaultNamePlural() {
    return this.defaultNamePlural;
  }

  /**
   * Gets this team's default color as set by the map creator.
   *
   * @return Default team color.
   */
  public ChatColor getDefaultColor() {
    return this.defaultColor;
  }

  public String getDefaultColoredName() {
    return this.getDefaultColor() + this.getDefaultName();
  }

  /**
   * Gets the color to be displayed alongside the player's name above the player.
   *
   * @return Overhead color.
   */
  public ChatColor getOverheadColor() {
    return this.overheadColor != null ? this.overheadColor : this.defaultColor;
  }

  public int getMinPlayers() {
    return minPlayers;
  }

  /**
   * Gets the maximum players that may be on this team.
   *
   * @return Maximum players for this team.
   */
  public int getMaxPlayers() {
    return this.maxPlayers;
  }

  /**
   * Gets the maximum overfill players that may be on this team.
   *
   * @return Maximum team overfill size for this team always >= maxPlayers
   */
  public int getMaxOverfill() {
    return this.maxOverfill;
  }

  /**
   * @return The number of slots reserved for players with "join full teams" privileges i.e.
   *     maxOverfill - maxPlayers
   */
  public int getOverfillSlots() {
    return this.maxOverfill - this.maxPlayers;
  }

  public NameTagVisibility getNameTagVisibility() {
    return nameTagVisibility;
  }
}
