package tc.oc.pgm.teams;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.event.Event;
import org.bukkit.scoreboard.NameTagVisibility;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.filters.matcher.TypedFilter;

/** Immutable class to represent a team in a map that is not tied to any specific match. */
@FeatureInfo(name = "team")
public class TeamFactory extends SelfIdentifyingFeatureDefinition
    implements TypedFilter<PartyQuery> {
  protected final String defaultName;
  protected final boolean defaultNamePlural;
  protected final ChatColor defaultColor;
  protected final @Nullable DyeColor dyeColor;
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
   * @param dyeColor Dye color to be used for blocks on kits or control points
   * @param maxPlayers Maximum amount of players that may be on this team.
   * @param nameTagVisibility Who can see the name tags of players on this team
   */
  public TeamFactory(
      @Nullable String id,
      String defaultName,
      boolean defaultNamePlural,
      ChatColor defaultColor,
      @Nullable DyeColor dyeColor,
      int minPlayers,
      int maxPlayers,
      int maxOverfill,
      NameTagVisibility nameTagVisibility) {
    super(id);
    this.defaultName = defaultName;
    this.defaultNamePlural = defaultNamePlural;
    this.defaultColor = defaultColor;
    this.dyeColor = dyeColor;
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
   * Gets the dye color to use for blocks on kits or control points
   *
   * @return Dye color for the team
   */
  public @Nullable DyeColor getDyeColor() {
    return this.dyeColor;
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

  // Filter implementation:
  @Override
  public Class<? extends PartyQuery> queryType() {
    return PartyQuery.class;
  }

  @Override
  public boolean matches(PartyQuery query) {
    final Party party = query.getParty();
    return party instanceof Team team && team.isInstance(this);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(PlayerPartyChangeEvent.class);
  }
}
